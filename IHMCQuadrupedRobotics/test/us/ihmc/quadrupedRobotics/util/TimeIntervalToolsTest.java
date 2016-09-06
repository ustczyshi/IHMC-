package us.ihmc.quadrupedRobotics.util;

import org.junit.Test;
import us.ihmc.tools.testing.TestPlanAnnotations;
import static org.junit.Assert.assertEquals;
import java.util.ArrayList;

public class TimeIntervalToolsTest
{

   @TestPlanAnnotations.DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testSortMethods()
   {
      double epsilon = 1e-6;

      int size = 10;
      ArrayList<TimedValue> arrayValues = new ArrayList<>(size);
      PreallocatedList<TimedValue> preallocatedValues = new PreallocatedList<>(TimedValue.class, size);

      for (int i = 0; i < size; i++)
      {
         TimedValue tv = new TimedValue(i, new TimeInterval(i, i + 1));
         arrayValues.add(tv);
         preallocatedValues.add();
         preallocatedValues.get(i).set(tv);
      }

      TimeIntervalTools.sortByReverseStartTime(arrayValues);
      TimeIntervalTools.sortByReverseStartTime(preallocatedValues);
      for (int i = 0; i < size; i++)
      {
         assertEquals(arrayValues.get(i).getValue(), size - 1 - i, epsilon);
         assertEquals(preallocatedValues.get(i).getValue(), size - 1 - i, epsilon);
      }

      TimeIntervalTools.sortByStartTime(arrayValues);
      TimeIntervalTools.sortByStartTime(preallocatedValues);
      for (int i = 0; i < size; i++)
      {
         assertEquals(arrayValues.get(i).getValue(), i, epsilon);
         assertEquals(preallocatedValues.get(i).getValue(), i, epsilon);
      }

      TimeIntervalTools.sortByReverseEndTime(arrayValues);
      TimeIntervalTools.sortByReverseEndTime(preallocatedValues);
      for (int i = 0; i < size; i++)
      {
         assertEquals(arrayValues.get(i).getValue(), size - 1 - i, epsilon);
         assertEquals(preallocatedValues.get(i).getValue(), size - 1 - i, epsilon);
      }


      TimeIntervalTools.sortByEndTime(arrayValues);
      TimeIntervalTools.sortByEndTime(preallocatedValues);
      for (int i = 0; i < arrayValues.size(); i++)
      {
         assertEquals(arrayValues.get(i).getValue(), i, epsilon);
         assertEquals(preallocatedValues.get(i).getValue(), i, epsilon);
      }
   }

   @TestPlanAnnotations.DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testRemoveMethods()
   {
      double epsilon = 1e-6;

      int size = 10;
      ArrayList<TimedValue> arrayValues = new ArrayList<>(size);
      PreallocatedList<TimedValue> preallocatedValues = new PreallocatedList<>(TimedValue.class, size);

      for (int i = 0; i < size; i++)
      {
         TimedValue tv = new TimedValue(i, new TimeInterval(i, i + 1));
         arrayValues.add(tv);
         preallocatedValues.add();
         preallocatedValues.get(i).set(tv);
      }

      TimeIntervalTools.removeAllEndingAfterTime(arrayValues, 8.5);
      TimeIntervalTools.removeAllEndingAfterTime(preallocatedValues, 8.5);
      size = 8;
      assertEquals(arrayValues.size(), size);
      assertEquals(preallocatedValues.size(), size);
      for (int i = 0; i < size; i++)
      {
         assertEquals(arrayValues.get(i).getValue(), i, epsilon);
         assertEquals(preallocatedValues.get(i).getValue(), i, epsilon);
      }

      TimeIntervalTools.removeAllStartingAfterTime(arrayValues, 6.5);
      TimeIntervalTools.removeAllStartingAfterTime(preallocatedValues, 6.5);
      size = 7;
      assertEquals(arrayValues.size(), size);
      assertEquals(preallocatedValues.size(), size);
      for (int i = 0; i < size; i++)
      {
         assertEquals(arrayValues.get(i).getValue(), i, epsilon);
         assertEquals(preallocatedValues.get(i).getValue(), i, epsilon);
      }

      TimeIntervalTools.removeAllStartingBeforeTime(arrayValues, 1.5);
      TimeIntervalTools.removeAllStartingBeforeTime(preallocatedValues, 1.5);
      size = 5;
      assertEquals(arrayValues.size(), size);
      assertEquals(preallocatedValues.size(), size);
      for (int i = 0; i < size; i++)
      {
         assertEquals(arrayValues.get(i).getValue(), i + 2, epsilon);
         assertEquals(preallocatedValues.get(i).getValue(), i + 2, epsilon);
      }

      TimeIntervalTools.removeAllEndingBeforeTime(arrayValues, 6.5);
      TimeIntervalTools.removeAllEndingBeforeTime(preallocatedValues, 6.5);
      assertEquals(arrayValues.size(), 1);
      assertEquals(preallocatedValues.size(), 1);
      assertEquals(arrayValues.get(0).getValue(), 6, epsilon);
      assertEquals(preallocatedValues.get(0).getValue(), 6, epsilon);
   }
}
