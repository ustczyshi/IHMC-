package us.ihmc.humanoidRobotics.communication.packets;

import java.util.Random;

import us.ihmc.communication.packets.QueueableMessage;
import us.ihmc.communication.packets.SelectionMatrix3DMessage;
import us.ihmc.communication.packets.WeightMatrix3DMessage;
import us.ihmc.communication.ros.generators.RosExportedField;
import us.ihmc.communication.ros.generators.RosIgnoredField;
import us.ihmc.euclid.interfaces.Transformable;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.transform.QuaternionBasedTransform;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.transform.interfaces.Transform;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.euclid.tuple4D.Quaternion;
import us.ihmc.euclid.utils.NameBasedHashCodeTools;
import us.ihmc.robotics.math.trajectories.waypoints.FrameSO3TrajectoryPointList;
import us.ihmc.robotics.random.RandomGeometry;
import us.ihmc.robotics.screwTheory.SelectionMatrix3D;
import us.ihmc.robotics.weightMatrices.WeightMatrix3D;

public abstract class AbstractSO3TrajectoryMessage<T extends AbstractSO3TrajectoryMessage<T>> extends QueueableMessage<T>
      implements Transformable, FrameBasedMessage
{
   @RosExportedField(documentation = "List of trajectory points (in taskpsace) to go through while executing the trajectory. Use dataFrame to define what frame the points are expressed in")
   public SO3TrajectoryPointMessage[] taskspaceTrajectoryPoints;

   @RosExportedField(documentation = "Frame information for this message.")
   public FrameInformation frameInformation = new FrameInformation();

   @RosIgnoredField
   public SelectionMatrix3DMessage selectionMatrix;

   @RosIgnoredField
   public WeightMatrix3DMessage weightMatrix;

   @RosExportedField(documentation = "Flag that tells the controller whether the use of a custom control frame is requested.")
   public boolean useCustomControlFrame = false;

   @RosExportedField(documentation = "Pose of custom control frame. This is the frame attached to the rigid body that the taskspace trajectory is defined for.")
   public QuaternionBasedTransform controlFramePose;

   /**
    * Empty constructor for serialization.
    */
   public AbstractSO3TrajectoryMessage()
   {
      super();
      setUniqueId(VALID_MESSAGE_DEFAULT_ID);
   }

   public AbstractSO3TrajectoryMessage(Random random)
   {
      super(random);
      setUniqueId(VALID_MESSAGE_DEFAULT_ID);

      int randomNumberOfPoints = random.nextInt(16) + 1;
      taskspaceTrajectoryPoints = new SO3TrajectoryPointMessage[randomNumberOfPoints];
      for (int i = 0; i < randomNumberOfPoints; i++)
      {
         taskspaceTrajectoryPoints[i] = new SO3TrajectoryPointMessage(random);
      }

      frameInformation.setTrajectoryReferenceFrameId(random.nextLong());
      frameInformation.setDataReferenceFrameId(random.nextLong());

      useCustomControlFrame = random.nextBoolean();

      controlFramePose = new QuaternionBasedTransform(RandomGeometry.nextQuaternion(random), RandomGeometry.nextVector3D(random));
   }

   public AbstractSO3TrajectoryMessage(AbstractSO3TrajectoryMessage<?> so3TrajectoryMessage)
   {
      taskspaceTrajectoryPoints = new SO3TrajectoryPointMessage[so3TrajectoryMessage.getNumberOfTrajectoryPoints()];
      for (int i = 0; i < getNumberOfTrajectoryPoints(); i++)
         taskspaceTrajectoryPoints[i] = new SO3TrajectoryPointMessage(so3TrajectoryMessage.taskspaceTrajectoryPoints[i]);
      setExecutionMode(so3TrajectoryMessage.getExecutionMode(), so3TrajectoryMessage.getPreviousMessageId());

      setUniqueId(so3TrajectoryMessage.getUniqueId());
      setDestination(so3TrajectoryMessage.getDestination());
      setExecutionDelayTime(so3TrajectoryMessage.getExecutionDelayTime());
      frameInformation.set(so3TrajectoryMessage);
   }

   public AbstractSO3TrajectoryMessage(double trajectoryTime, Quaternion desiredOrientation, ReferenceFrame trajectoryFrame)
   {
      this(trajectoryTime, desiredOrientation, trajectoryFrame.getNameBasedHashCode());
   }

   public AbstractSO3TrajectoryMessage(double trajectoryTime, Quaternion desiredOrientation, long trajectoryReferenceFrameId)
   {
      Vector3D zeroAngularVelocity = new Vector3D();
      taskspaceTrajectoryPoints = new SO3TrajectoryPointMessage[] {new SO3TrajectoryPointMessage(trajectoryTime, desiredOrientation, zeroAngularVelocity)};
      setUniqueId(VALID_MESSAGE_DEFAULT_ID);
      frameInformation.setTrajectoryReferenceFrameId(trajectoryReferenceFrameId);
   }

   public AbstractSO3TrajectoryMessage(int numberOfTrajectoryPoints)
   {
      taskspaceTrajectoryPoints = new SO3TrajectoryPointMessage[numberOfTrajectoryPoints];
      setUniqueId(VALID_MESSAGE_DEFAULT_ID);
   }

   public void getTrajectoryPoints(FrameSO3TrajectoryPointList trajectoryPointListToPack)
   {
      FrameInformation.checkIfDataFrameIdsMatch(frameInformation, trajectoryPointListToPack.getReferenceFrame());

      SO3TrajectoryPointMessage[] trajectoryPointMessages = getTrajectoryPoints();
      int numberOfPoints = trajectoryPointMessages.length;

      for (int i = 0; i < numberOfPoints; i++)
      {
         SO3TrajectoryPointMessage so3TrajectoryPointMessage = trajectoryPointMessages[i];
         trajectoryPointListToPack.addTrajectoryPoint(so3TrajectoryPointMessage.time, so3TrajectoryPointMessage.orientation,
               so3TrajectoryPointMessage.angularVelocity);
      }
   }

   public void set(T other)
   {
      if (getNumberOfTrajectoryPoints() != other.getNumberOfTrajectoryPoints())
         throw new RuntimeException("Must the same number of waypoints.");
      for (int i = 0; i < getNumberOfTrajectoryPoints(); i++)
         taskspaceTrajectoryPoints[i] = new SO3TrajectoryPointMessage(other.taskspaceTrajectoryPoints[i]);
      setExecutionMode(other.getExecutionMode(), other.getPreviousMessageId());
      setExecutionDelayTime(other.getExecutionDelayTime());
      frameInformation.set(other);
   }

   /**
    * Create a trajectory point.
    *
    * @param trajectoryPointIndex index of the trajectory point to create.
    * @param time time at which the trajectory point has to be reached. The time is relative to when
    *           the trajectory starts.
    * @param orientation define the desired 3D orientation to be reached at this trajectory point.
    *           It is should be expressed in the frame defined by referenceFrameId
    * @param angularVelocity define the desired 3D angular velocity to be reached at this trajectory
    *           point. It is should be expressed in the frame defined by referenceFrameId
    */
   public final void setTrajectoryPoint(int trajectoryPointIndex, double time, Quaternion orientation, Vector3D angularVelocity,
         ReferenceFrame expressedInReferenceFrame)
   {
      FrameInformation.checkIfDataFrameIdsMatch(frameInformation, expressedInReferenceFrame);
      rangeCheck(trajectoryPointIndex);
      taskspaceTrajectoryPoints[trajectoryPointIndex] = new SO3TrajectoryPointMessage(time, orientation, angularVelocity);
   }

   /**
    * Create a trajectory point.
    *
    * @param trajectoryPointIndex index of the trajectory point to create.
    * @param time time at which the trajectory point has to be reached. The time is relative to when
    *           the trajectory starts.
    * @param orientation define the desired 3D orientation to be reached at this trajectory point.
    *           It is should be expressed in the frame defined by referenceFrameId
    * @param angularVelocity define the desired 3D angular velocity to be reached at this trajectory
    *           point. It is should be expressed in the frame defined by referenceFrameId
    */
   public final void setTrajectoryPoint(int trajectoryPointIndex, double time, Quaternion orientation, Vector3D angularVelocity,
         long expressedInReferenceFrameId)
   {
      FrameInformation.checkIfDataFrameIdsMatch(frameInformation, expressedInReferenceFrameId);
      rangeCheck(trajectoryPointIndex);
      taskspaceTrajectoryPoints[trajectoryPointIndex] = new SO3TrajectoryPointMessage(time, orientation, angularVelocity);
   }

   @Override
   public void applyTransform(Transform transform)
   {
      for (int i = 0; i < getNumberOfTrajectoryPoints(); i++)
         taskspaceTrajectoryPoints[i].applyTransform(transform);
   }

   @Override
   public void applyInverseTransform(Transform transform)
   {
      for (int i = 0; i < getNumberOfTrajectoryPoints(); i++)
         taskspaceTrajectoryPoints[i].applyInverseTransform(transform);
   }

   /**
    * Sets the selection matrix to use for executing this message.
    * <p>
    * The selection matrix is used to determinate which degree of freedom of the end-effector should
    * be controlled. When it is NOT provided, the controller will assume that all the degrees of
    * freedom of the end-effector should be controlled.
    * </p>
    * <p>
    * The selection frame coming along with the given selection matrix is used to determine to what
    * reference frame the selected axes are referring to. For instance, if only the hand height in
    * world should be controlled on the linear z component of the selection matrix should be
    * selected and the reference frame should be world frame. When no reference frame is provided with
    * the selection matrix, it will be used as it is in the control frame, i.e. the body-fixed frame
    * if not defined otherwise.
    * </p>
    *
    * @param selectionMatrix the selection matrix to use when executing this trajectory message. Not
    *           modified.
    */
   public void setSelectionMatrix(SelectionMatrix3D selectionMatrix)
   {
      if (this.selectionMatrix == null)
         this.selectionMatrix = new SelectionMatrix3DMessage(selectionMatrix);
      else
         this.selectionMatrix.set(selectionMatrix);
   }

   /**
    * Sets the weight matrix to use for executing this message.
    * <p>
    * The weight matrix is used to set the qp weights for the controlled degrees of freedom of the end-effector.
    * When it is not provided, or when the weights are set to Double.NaN, the controller will use the default QP Weights
    * set for each axis.
    * </p>
    * <p>
    * The weight frame coming along with the given weight matrix is used to determine to what
    * reference frame the weights are referring to.
    * </p>
    *
    * @param weightMatrix the weight matrix to use when executing this trajectory message. parameter is not
    *           modified.
    */
   public void setWeightMatrix(WeightMatrix3D weightMatrix)
   {
      if (this.weightMatrix == null)
         this.weightMatrix = new WeightMatrix3DMessage(weightMatrix);
      else
         this.weightMatrix.set(weightMatrix);
   }

   public final int getNumberOfTrajectoryPoints()
   {
      return taskspaceTrajectoryPoints.length;
   }

   public final SO3TrajectoryPointMessage[] getTrajectoryPoints()
   {
      return taskspaceTrajectoryPoints;
   }

   public final SO3TrajectoryPointMessage getTrajectoryPoint(int trajectoryPointIndex)
   {
      rangeCheck(trajectoryPointIndex);
      return taskspaceTrajectoryPoints[trajectoryPointIndex];
   }

   public final SO3TrajectoryPointMessage getLastTrajectoryPoint()
   {
      return taskspaceTrajectoryPoints[taskspaceTrajectoryPoints.length - 1];
   }

   public final double getTrajectoryTime()
   {
      return getLastTrajectoryPoint().time;
   }

   public boolean hasSelectionMatrix()
   {
      return selectionMatrix != null;
   }

   public void getSelectionMatrix(SelectionMatrix3D selectionMatrixToPack)
   {
      selectionMatrixToPack.resetSelection();
      if (selectionMatrix != null)
         selectionMatrix.getSelectionMatrix(selectionMatrixToPack);
   }

   public boolean hasWeightMatrix()
   {
      return weightMatrix != null;
   }

   public void getWeightMatrix(WeightMatrix3D weightMatrixToPack)
   {
      weightMatrixToPack.clear();
      if (weightMatrix != null)
         weightMatrix.getWeightMatrix(weightMatrixToPack);
   }

   @Override
   public FrameInformation getFrameInformation()
   {
      if (frameInformation == null)
         frameInformation = new FrameInformation();
      return frameInformation;
   }

   /**
    * Returns the unique ID referring to the selection frame to use with the selection matrix of
    * this message.
    * <p>
    * If this message does not have a selection matrix, this method returns
    * {@link NameBasedHashCodeTools#NULL_HASHCODE}.
    * </p>
    *
    * @return the selection frame ID for the selection matrix.
    */
   public long getSelectionFrameId()
   {
      if (selectionMatrix != null)
         return selectionMatrix.getSelectionFrameId();
      else
         return NameBasedHashCodeTools.NULL_HASHCODE;
   }

   /**
    * Returns the unique ID referring to the weight frame to use with the weight matrix of
    * this message.
    * <p>
    * If this message does not have a weight matrix, this method returns
    * {@link NameBasedHashCodeTools#NULL_HASHCODE}.
    * </p>
    *
    * @return the weight frame ID for the weight matrix.
    */
   public long getWeightMatrixFrameId()
   {
      if (weightMatrix != null)
         return weightMatrix.getWeightFrameId();
      else
         return NameBasedHashCodeTools.NULL_HASHCODE;
   }

   private void rangeCheck(int trajectoryPointIndex)
   {
      if (trajectoryPointIndex >= getNumberOfTrajectoryPoints() || trajectoryPointIndex < 0)
         throw new IndexOutOfBoundsException(
               "Trajectory point index: " + trajectoryPointIndex + ", number of trajectory points: " + getNumberOfTrajectoryPoints());
   }

   /** {@inheritDoc} */
   @Override
   public String validateMessage()
   {
      return PacketValidityChecker.validateSO3TrajectoryMessage(this);
   }

   @Override
   public String toString()
   {
      if (taskspaceTrajectoryPoints != null)
         return getClass().getSimpleName() + ": number of SO3 trajectory points = " + getNumberOfTrajectoryPoints() + "\n" + frameInformation.toString();
      else
         return getClass().getSimpleName() + ": no SO3 trajectory points";
   }

   @Override
   public boolean epsilonEquals(T other, double epsilon)
   {
      if (!frameInformation.epsilonEquals(other.frameInformation, epsilon))
         return false;

      if (getNumberOfTrajectoryPoints() != other.getNumberOfTrajectoryPoints())
         return false;

      for (int i = 0; i < getNumberOfTrajectoryPoints(); i++)
      {
         if (!taskspaceTrajectoryPoints[i].epsilonEquals(other.taskspaceTrajectoryPoints[i], epsilon))
            return false;
      }

      return super.epsilonEquals(other, epsilon);
   }

   public void setUseCustomControlFrame(boolean useCustomControlFrame)
   {
      this.useCustomControlFrame = useCustomControlFrame;
   }

   public void setControlFramePosition(Point3D controlFramePosition)
   {
      if (controlFramePose == null)
         controlFramePose = new QuaternionBasedTransform();
      controlFramePose.setTranslation(controlFramePosition);
   }

   public void setControlFrameOrientation(Quaternion controlFrameOrientation)
   {
      if (controlFramePose == null)
         controlFramePose = new QuaternionBasedTransform();
      controlFramePose.setRotation(controlFrameOrientation);
   }

   public boolean useCustomControlFrame()
   {
      return useCustomControlFrame;
   }

   public void getControlFramePose(RigidBodyTransform controlFrameTransformToPack)
   {
      if (controlFramePose == null)
         controlFrameTransformToPack.setToNaN();
      else
         controlFrameTransformToPack.set(controlFramePose);
   }
}
