package us.ihmc.atlas.parameters;

import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotPhysicalProperties;
import us.ihmc.utilities.Pair;
import us.ihmc.utilities.humanoidRobot.partNames.ArmJointName;
import us.ihmc.utilities.math.geometry.TransformTools;

import javax.media.j3d.Transform3D;
import javax.vecmath.Vector3d;

public class AtlasPhysicalProperties extends DRCRobotPhysicalProperties
{
   public static final double ankleHeight = 0.084;
   public static final double pelvisToFoot = 0.887;

   // this transform is probably in the wrong direction.  The ankle is above the sole,  so the origin of the ankle should have a positive z value
   // from the point of view of the sole.  With this transform it will have a negative value -- PA
   public static final double foot_width = 0.12;    // 0.08;   //0.124887;
   public static final double toe_width = 0.095;    // 0.07;   //0.05;   //
   public static final double foot_length = 0.255;
   public static final double foot_back = 0.09;    // 0.06;   //0.082;    // 0.07;
   public static final double foot_start_toetaper_from_back = 0.195;
   public static final double foot_forward = foot_length - foot_back;    // 0.16;   //0.178;    // 0.18;
   public static final Transform3D ankle_to_sole_frame_tranform = TransformTools.createTranslationTransform(new Vector3d(foot_length / 2.0 - foot_back, 0.0, -ankleHeight));
   public static final double shinLength = 0.374;
   public static final double thighLength = 0.422;

   {
      armJointLimits.put(ArmJointName.SHOULDER_PITCH, new Pair<Double, Double>(-2 * Math.PI, 2 * Math.PI));
      armJointLimits.put(ArmJointName.SHOULDER_ROLL, new Pair<Double, Double>(-2 * Math.PI, 2 * Math.PI));
      armJointLimits.put(ArmJointName.ELBOW_PITCH, new Pair<Double, Double>(-2 * Math.PI, 2 * Math.PI));
      armJointLimits.put(ArmJointName.ELBOW_ROLL, new Pair<Double, Double>(-2 * Math.PI, 2 * Math.PI));
      armJointLimits.put(ArmJointName.WRIST_PITCH, new Pair<Double, Double>(-2 * Math.PI, 2 * Math.PI));
      armJointLimits.put(ArmJointName.WRIST_ROLL, new Pair<Double, Double>(-2 * Math.PI, 2 * Math.PI));
   }

   @Override
   public double getAnkleHeight()
   {
      return ankleHeight;
   }

}
