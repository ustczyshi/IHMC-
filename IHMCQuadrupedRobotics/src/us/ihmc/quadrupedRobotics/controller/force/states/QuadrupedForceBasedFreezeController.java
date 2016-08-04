package us.ihmc.quadrupedRobotics.controller.force.states;

import us.ihmc.SdfLoader.SDFFullQuadrupedRobotModel;
import us.ihmc.SdfLoader.partNames.JointRole;
import us.ihmc.SdfLoader.partNames.QuadrupedJointName;
import us.ihmc.quadrupedRobotics.controller.ControllerEvent;
import us.ihmc.quadrupedRobotics.controller.QuadrupedController;
import us.ihmc.quadrupedRobotics.controller.force.QuadrupedForceControllerToolbox;
import us.ihmc.quadrupedRobotics.controller.force.toolbox.QuadrupedSoleForceEstimator;
import us.ihmc.quadrupedRobotics.controller.force.toolbox.QuadrupedSolePositionController;
import us.ihmc.quadrupedRobotics.controller.force.toolbox.QuadrupedTaskSpaceController;
import us.ihmc.quadrupedRobotics.controller.force.toolbox.QuadrupedTaskSpaceEstimator;
import us.ihmc.quadrupedRobotics.model.QuadrupedRuntimeEnvironment;
import us.ihmc.quadrupedRobotics.params.BooleanParameter;
import us.ihmc.quadrupedRobotics.params.DoubleArrayParameter;
import us.ihmc.quadrupedRobotics.params.DoubleParameter;
import us.ihmc.quadrupedRobotics.params.ParameterFactory;
import us.ihmc.quadrupedRobotics.planning.ContactState;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.BooleanYoVariable;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.robotSide.QuadrantDependentList;
import us.ihmc.robotics.robotSide.RobotQuadrant;
import us.ihmc.robotics.screwTheory.OneDoFJoint;

public class QuadrupedForceBasedFreezeController implements QuadrupedController
{
   private final YoVariableRegistry registry = new YoVariableRegistry(QuadrupedForceBasedFreezeController.class.getSimpleName());

   // Parameters
   private final ParameterFactory parameterFactory = ParameterFactory.createWithRegistry(getClass(), registry);
   private final DoubleParameter jointDampingParameter = parameterFactory.createDouble("jointDamping", 5.0);
   private final DoubleArrayParameter solePositionProportionalGainsParameter = parameterFactory
         .createDoubleArray("solePositionProportionalGains", 5000, 5000, 5000);
   private final DoubleArrayParameter solePositionDerivativeGainsParameter = parameterFactory.createDoubleArray("solePositionDerivativeGains", 100, 100, 100);
   private final DoubleArrayParameter solePositionIntegralGainsParameter = parameterFactory.createDoubleArray("solePositionIntegralGains", 0, 0, 0);
   private final DoubleParameter solePositionMaxIntegralErrorParameter = parameterFactory.createDouble("solePositionMaxIntegralError", 0);
   private final DoubleParameter jointPositionLimitDampingParameter = parameterFactory.createDouble("jointPositionLimitDamping", 10);
   private final DoubleParameter jointPositionLimitStiffnessParameter = parameterFactory.createDouble("jointPositionLimitStiffness", 100);
   private final BooleanParameter useForceFeedbackControlParameter = parameterFactory.createBoolean("useForceFeedbackControl", false);
   private final BooleanParameter useSetpointCompensationParameter = parameterFactory.createBoolean("useSetpointCompensation", true);
   private final DoubleParameter maxSetpointCompensationParameter = parameterFactory.createDouble("maxSetpointCompensation", .05);
   private final DoubleParameter setpointCompensationMultiplierParameter = parameterFactory.createDouble("setpointCompensationMultiplier", 1.1);

   // Yo variables
   private final QuadrantDependentList<DoubleYoVariable[]> yoSetpointCompensationList;
   private final BooleanYoVariable yoUseForceFeedbackControl;
   // Reference frames
   private final ReferenceFrame bodyFrame;

   // Feedback controller
   private final QuadrupedSolePositionController solePositionController;
   private final QuadrupedSolePositionController.Setpoints solePositionControllerSetpoints;

   // Task space controller
   private final QuadrupedTaskSpaceEstimator.Estimates taskSpaceEstimates;
   private final QuadrupedTaskSpaceEstimator taskSpaceEstimator;
   private final QuadrupedTaskSpaceController.Commands taskSpaceControllerCommands;
   private final QuadrupedTaskSpaceController.Settings taskSpaceControllerSettings;
   private final QuadrupedTaskSpaceController taskSpaceController;
   private final QuadrupedSoleForceEstimator soleForceEstimator;

   private SDFFullQuadrupedRobotModel fullRobotModel;

   public QuadrupedForceBasedFreezeController(QuadrupedRuntimeEnvironment environment, QuadrupedForceControllerToolbox controllerToolbox)
   {
      // Reference frames
      bodyFrame = controllerToolbox.getReferenceFrames().getBodyFrame();

      // Yo variables
      yoSetpointCompensationList = new QuadrantDependentList<>();
      yoUseForceFeedbackControl = new BooleanYoVariable("useForceFeedbackControl", registry);
      for (RobotQuadrant quadrant : RobotQuadrant.values())
      {
         DoubleYoVariable[] temp = new DoubleYoVariable[3];
         temp[0] = new DoubleYoVariable(quadrant.getPascalCaseName() + "SetpointCompensation" + "X", registry);
         temp[1] = new DoubleYoVariable(quadrant.getPascalCaseName() + "SetpointCompensation" + "Z", registry);
         temp[2] = new DoubleYoVariable(quadrant.getPascalCaseName() + "SetpointCompensation" + "Y", registry);
         yoSetpointCompensationList.set(quadrant, temp);
      }
      // Feedback controller
      solePositionController = controllerToolbox.getSolePositionController();
      solePositionControllerSetpoints = new QuadrupedSolePositionController.Setpoints();

      // Task space controller
      taskSpaceEstimates = new QuadrupedTaskSpaceEstimator.Estimates();
      taskSpaceEstimator = controllerToolbox.getTaskSpaceEstimator();
      soleForceEstimator = controllerToolbox.getSoleForceEstimator();
      taskSpaceControllerCommands = new QuadrupedTaskSpaceController.Commands();
      taskSpaceControllerSettings = new QuadrupedTaskSpaceController.Settings();
      taskSpaceController = controllerToolbox.getTaskSpaceController();
      fullRobotModel = environment.getFullRobotModel();

      environment.getParentRegistry().addChild(registry);
   }

   @Override
   public void onEntry()
   {
      updateEstimates();

      // Initialize sole position controller
      solePositionControllerSetpoints.initialize(taskSpaceEstimates);
      solePositionController.reset();

      // Initialize task space controller
      taskSpaceControllerSettings.initialize();
      for (RobotQuadrant quadrant : RobotQuadrant.values)
      {
         taskSpaceControllerSettings.setContactState(quadrant, ContactState.NO_CONTACT);
      }
      taskSpaceController.reset();

      // Initialize sole force estimator
      soleForceEstimator.reset();

      // Initial sole position setpoints
      soleForceEstimator.compute();
      for (RobotQuadrant quadrant : RobotQuadrant.values)
      {
         solePositionControllerSetpoints.getSolePosition(quadrant).setIncludingFrame(taskSpaceEstimates.getSolePosition(quadrant));
         solePositionControllerSetpoints.getSolePosition(quadrant).changeFrame(bodyFrame);
         if (useSetpointCompensationParameter.get())
         {
            yoSetpointCompensationList.get(quadrant)[0].set(Math
                  .max(setpointCompensationMultiplierParameter.get()*soleForceEstimator.getSoleForce(quadrant).getX() / solePositionProportionalGainsParameter.get(0),
                        -maxSetpointCompensationParameter.get()));
            yoSetpointCompensationList.get(quadrant)[1].set(
                  Math.max(setpointCompensationMultiplierParameter.get()*soleForceEstimator.getSoleForce(quadrant).getY() / solePositionProportionalGainsParameter.get(1),
                        -maxSetpointCompensationParameter.get()));
            yoSetpointCompensationList.get(quadrant)[2].set(
                  Math.max(setpointCompensationMultiplierParameter.get()*soleForceEstimator.getSoleForce(quadrant).getZ() / solePositionProportionalGainsParameter.get(2),
                        -maxSetpointCompensationParameter.get()));
            solePositionControllerSetpoints.getSolePosition(quadrant)
                  .add(yoSetpointCompensationList.get(quadrant)[0].getDoubleValue(), yoSetpointCompensationList.get(quadrant)[1].getDoubleValue(), yoSetpointCompensationList.get(quadrant)[2].getDoubleValue());
         }
         else
         {
            yoSetpointCompensationList.get(quadrant)[0].set(0);
            yoSetpointCompensationList.get(quadrant)[1].set(0);
            yoSetpointCompensationList.get(quadrant)[2].set(0);
         }
      }
      yoUseForceFeedbackControl.set(useForceFeedbackControlParameter.get());
      // Initialize force feedback
      for (QuadrupedJointName jointName : QuadrupedJointName.values())
      {
         OneDoFJoint oneDoFJoint = fullRobotModel.getOneDoFJointByName(jointName);
         if (oneDoFJoint != null && jointName.getRole().equals(JointRole.LEG))
         {
            oneDoFJoint.setUseFeedBackForceControl(yoUseForceFeedbackControl.getBooleanValue());
         }
      }
   }

   @Override
   public ControllerEvent process()
   {
      updateGains();
      updateEstimates();
      updateSetpoints();
      taskSpaceControllerSettings.getVirtualModelControllerSettings().setJointDamping(jointDampingParameter.get());
      return null;
   }

   @Override
   public void onExit()
   {
      yoUseForceFeedbackControl.set(true);
      for (QuadrupedJointName jointName : QuadrupedJointName.values())
      {
         OneDoFJoint oneDoFJoint = fullRobotModel.getOneDoFJointByName(jointName);
         if (oneDoFJoint != null && jointName.getRole().equals(JointRole.LEG))
         {
            oneDoFJoint.setUseFeedBackForceControl(yoUseForceFeedbackControl.getBooleanValue());
         }
      }
   }

   private void updateGains()
   {
      for (RobotQuadrant quadrant : RobotQuadrant.values)
      {
         solePositionController.getGains(quadrant).setProportionalGains(solePositionProportionalGainsParameter.get());
         solePositionController.getGains(quadrant).setIntegralGains(solePositionIntegralGainsParameter.get(), solePositionMaxIntegralErrorParameter.get());
         solePositionController.getGains(quadrant).setDerivativeGains(solePositionDerivativeGainsParameter.get());
      }
      taskSpaceControllerSettings.getVirtualModelControllerSettings().setJointDamping(jointDampingParameter.get());
      taskSpaceControllerSettings.getVirtualModelControllerSettings().setJointPositionLimitDamping(jointPositionLimitDampingParameter.get());
      taskSpaceControllerSettings.getVirtualModelControllerSettings().setJointPositionLimitStiffness(jointPositionLimitStiffnessParameter.get());
   }

   private void updateEstimates()
   {
      taskSpaceEstimator.compute(taskSpaceEstimates);
   }

   private void updateSetpoints()
   {
      solePositionController.compute(taskSpaceControllerCommands.getSoleForce(), solePositionControllerSetpoints, taskSpaceEstimates);
      taskSpaceController.compute(taskSpaceControllerSettings, taskSpaceControllerCommands);
   }
}
