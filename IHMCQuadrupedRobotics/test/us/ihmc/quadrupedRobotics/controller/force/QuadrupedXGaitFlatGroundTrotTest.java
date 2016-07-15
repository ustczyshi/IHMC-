package us.ihmc.quadrupedRobotics.controller.force;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import us.ihmc.quadrupedRobotics.QuadrupedForceTestYoVariables;
import us.ihmc.quadrupedRobotics.QuadrupedMultiRobotTestInterface;
import us.ihmc.quadrupedRobotics.QuadrupedTestBehaviors;
import us.ihmc.quadrupedRobotics.QuadrupedTestFactory;
import us.ihmc.quadrupedRobotics.QuadrupedTestGoals;
import us.ihmc.quadrupedRobotics.controller.QuadrupedControlMode;
import us.ihmc.quadrupedRobotics.simulation.QuadrupedGroundContactModelType;
import us.ihmc.robotics.testing.YoVariableTestGoal;
import us.ihmc.simulationconstructionset.util.simulationRunner.GoalOrientedTestConductor;
import us.ihmc.tools.MemoryTools;
import us.ihmc.tools.testing.TestPlanAnnotations.DeployableTestMethod;

public abstract class QuadrupedXGaitFlatGroundTrotTest implements QuadrupedMultiRobotTestInterface
{
   private GoalOrientedTestConductor conductor;
   private QuadrupedForceTestYoVariables variables;

   @Before
   public void setup()
   {
      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " before test.");

      try
      {
         QuadrupedTestFactory quadrupedTestFactory = createQuadrupedTestFactory();
         quadrupedTestFactory.setControlMode(QuadrupedControlMode.FORCE);
         quadrupedTestFactory.setGroundContactModelType(QuadrupedGroundContactModelType.FLAT);
         conductor = quadrupedTestFactory.createTestConductor();
         variables = new QuadrupedForceTestYoVariables(conductor.getScs());
      }
      catch (IOException e)
      {
         throw new RuntimeException("Error loading simulation: " + e.getMessage());
      }
   }
   
   @After
   public void tearDown()
   {
      conductor = null;
      variables = null;
      
      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " after test.");
   }
   
   @DeployableTestMethod(estimatedDuration = 15.0)
   @Test(timeout = 30000)
   public void testTrottingForwardFast()
   {
      QuadrupedTestBehaviors.standUp(conductor, variables);
      QuadrupedTestBehaviors.enterXGait(conductor, variables);
      
      variables.getXGaitEndPhaseShiftInput().set(180.0);
      
      variables.getYoPlanarVelocityInputX().set(1.0);
      conductor.addSustainGoal(QuadrupedTestGoals.notFallen(variables));
      conductor.addTimeLimit(variables.getYoTime(), 5.0);
      conductor.addTerminalGoal(YoVariableTestGoal.doubleGreaterThan(variables.getRobotBodyX(), 2.0));
      conductor.simulate();
      
      conductor.concludeTesting();
   }
   
   @DeployableTestMethod(estimatedDuration = 15.0)
   @Test(timeout = 30000)
   public void testTrottingForwardSlow()
   {
      QuadrupedTestBehaviors.standUp(conductor, variables);
      QuadrupedTestBehaviors.enterXGait(conductor, variables);
      
      variables.getXGaitEndPhaseShiftInput().set(180.0);
      variables.getXGaitEndDoubleSupportDurationInput().set(0.3);
      
      variables.getYoPlanarVelocityInputX().set(0.1);
      conductor.addSustainGoal(QuadrupedTestGoals.notFallen(variables));
      conductor.addTimeLimit(variables.getYoTime(), 5.0);
      conductor.addTerminalGoal(YoVariableTestGoal.doubleGreaterThan(variables.getRobotBodyX(), 0.3));
      conductor.simulate();
      
      conductor.concludeTesting();
   }
   
   @DeployableTestMethod(estimatedDuration = 15.0)
   @Test(timeout = 30000)
   public void testTrottingBackwardsFast()
   {
      QuadrupedTestBehaviors.standUp(conductor, variables);
      QuadrupedTestBehaviors.enterXGait(conductor, variables);
      
      variables.getXGaitEndPhaseShiftInput().set(180.0);
      
      variables.getYoPlanarVelocityInputX().set(-1.0);
      conductor.addSustainGoal(QuadrupedTestGoals.notFallen(variables));
      conductor.addTimeLimit(variables.getYoTime(), 5.0);
      conductor.addTerminalGoal(YoVariableTestGoal.doubleLessThan(variables.getRobotBodyX(), -2.0));
      conductor.simulate();
      
      conductor.concludeTesting();
   }
   
   @DeployableTestMethod(estimatedDuration = 15.0)
   @Test(timeout = 30000)
   public void testTrottingBackwardsSlow()
   {
      QuadrupedTestBehaviors.standUp(conductor, variables);
      QuadrupedTestBehaviors.enterXGait(conductor, variables);
      
      variables.getXGaitEndPhaseShiftInput().set(180.0);
      variables.getXGaitEndDoubleSupportDurationInput().set(0.3);
      
      variables.getYoPlanarVelocityInputX().set(-0.1);
      conductor.addSustainGoal(QuadrupedTestGoals.notFallen(variables));
      conductor.addTimeLimit(variables.getYoTime(), 6.0);
      conductor.addTerminalGoal(YoVariableTestGoal.doubleLessThan(variables.getRobotBodyX(), -0.3));
      conductor.simulate();
      
      conductor.concludeTesting();
   }
   
   @DeployableTestMethod(estimatedDuration = 25.0)
   @Test(timeout = 30000)
   public void testTrottingInAForwardLeftCircle()
   {
      QuadrupedTestBehaviors.standUp(conductor, variables);
      QuadrupedTestBehaviors.enterXGait(conductor, variables);
      
      variables.getXGaitEndPhaseShiftInput().set(180.0);
      
      variables.getYoPlanarVelocityInputX().set(1.0);
      variables.getYoPlanarVelocityInputZ().set(0.5);
      conductor.addSustainGoal(QuadrupedTestGoals.notFallen(variables));
      conductor.addTimeLimit(variables.getYoTime(), 15.0);
      conductor.addWaypointGoal(YoVariableTestGoal.doubleGreaterThan(variables.getRobotBodyX(), 1.5));
      conductor.addWaypointGoal(YoVariableTestGoal.doubleWithinEpsilon(variables.getRobotBodyYaw(), Math.PI / 2, 0.1));
      conductor.addTerminalGoal(YoVariableTestGoal.doubleWithinEpsilon(variables.getRobotBodyX(), 0.0, 0.3));
      conductor.addTerminalGoal(YoVariableTestGoal.doubleWithinEpsilon(variables.getRobotBodyYaw(), Math.PI, 0.1));
      conductor.simulate();
      
      conductor.concludeTesting();
   }
   
   @DeployableTestMethod(estimatedDuration = 25.0)
   @Test(timeout = 30000)
   public void testTrottingInAForwardRightCircle()
   {
      QuadrupedTestBehaviors.standUp(conductor, variables);
      QuadrupedTestBehaviors.enterXGait(conductor, variables);
      
      variables.getXGaitEndPhaseShiftInput().set(180.0);
      
      variables.getYoPlanarVelocityInputX().set(1.0);
      variables.getYoPlanarVelocityInputZ().set(-0.5);
      conductor.addSustainGoal(QuadrupedTestGoals.notFallen(variables));
      conductor.addTimeLimit(variables.getYoTime(), 15.0);
      conductor.addWaypointGoal(YoVariableTestGoal.doubleGreaterThan(variables.getRobotBodyX(), 1.5));
      conductor.addWaypointGoal(YoVariableTestGoal.doubleWithinEpsilon(variables.getRobotBodyYaw(), -Math.PI / 2, 0.1));
      conductor.addTerminalGoal(YoVariableTestGoal.doubleWithinEpsilon(variables.getRobotBodyX(), 0.0, 0.3));
      conductor.addTerminalGoal(YoVariableTestGoal.doubleWithinEpsilon(variables.getRobotBodyYaw(), -Math.PI, 0.1));
      conductor.simulate();
      
      conductor.concludeTesting();
   }
   
   @DeployableTestMethod(estimatedDuration = 25.0)
   @Test(timeout = 30000)
   public void testTrottingInABackwardLeftCircle()
   {
      QuadrupedTestBehaviors.standUp(conductor, variables);
      QuadrupedTestBehaviors.enterXGait(conductor, variables);
      
      variables.getXGaitEndPhaseShiftInput().set(180.0);
      
      variables.getYoPlanarVelocityInputX().set(-1.0);
      variables.getYoPlanarVelocityInputZ().set(-0.5);
      conductor.addSustainGoal(QuadrupedTestGoals.notFallen(variables));
      conductor.addTimeLimit(variables.getYoTime(), 15.0);
      conductor.addWaypointGoal(YoVariableTestGoal.doubleGreaterThan(variables.getRobotBodyX(), -1.5));
      conductor.addWaypointGoal(YoVariableTestGoal.doubleWithinEpsilon(variables.getRobotBodyYaw(), -Math.PI / 2, 0.1));
      conductor.addTerminalGoal(YoVariableTestGoal.doubleWithinEpsilon(variables.getRobotBodyX(), 0.0, 0.3));
      conductor.addTerminalGoal(YoVariableTestGoal.doubleWithinEpsilon(variables.getRobotBodyYaw(), -Math.PI, 0.1));
      conductor.simulate();
      
      conductor.concludeTesting();
   }
   
   @DeployableTestMethod(estimatedDuration = 25.0)
   @Test(timeout = 30000)
   public void testTrottingInABackwardRightCircle()
   {
      QuadrupedTestBehaviors.standUp(conductor, variables);
      QuadrupedTestBehaviors.enterXGait(conductor, variables);
      
      variables.getXGaitEndPhaseShiftInput().set(180.0);
      
      variables.getYoPlanarVelocityInputX().set(-1.0);
      variables.getYoPlanarVelocityInputZ().set(0.5);
      conductor.addSustainGoal(QuadrupedTestGoals.notFallen(variables));
      conductor.addTimeLimit(variables.getYoTime(), 15.0);
      conductor.addWaypointGoal(YoVariableTestGoal.doubleGreaterThan(variables.getRobotBodyX(), -1.5));
      conductor.addWaypointGoal(YoVariableTestGoal.doubleWithinEpsilon(variables.getRobotBodyYaw(), Math.PI / 2, 0.1));
      conductor.addTerminalGoal(YoVariableTestGoal.doubleWithinEpsilon(variables.getRobotBodyX(), 0.0, 0.3));
      conductor.addTerminalGoal(YoVariableTestGoal.doubleWithinEpsilon(variables.getRobotBodyYaw(), Math.PI, 0.1));
      conductor.simulate();
      
      conductor.concludeTesting();
   }
}
