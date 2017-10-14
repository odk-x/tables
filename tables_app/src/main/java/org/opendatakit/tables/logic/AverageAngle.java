package org.opendatakit.tables.logic;

/**
 * Created by nkuebler on 14/07/14.
 */
public class AverageAngle
{
  private double[] mValues;
  private int mCurrentIndex;
  private int mNumberOfFrames;
  private boolean mIsFull;
  private double mAverageValue = Double.NaN;

  public AverageAngle(int frames)
  {
    this.mNumberOfFrames = frames;
    this.mCurrentIndex = 0;
    this.mValues = new double[frames];
  }

  public void add(double d)
  {
    mValues[mCurrentIndex] = d;
    if (mCurrentIndex == mNumberOfFrames - 1) {
      mCurrentIndex = 0;
      mIsFull = true;
    } else {
      mCurrentIndex++;
    }
    updateAverageValue();
  }

  public double getAverage()
  {
    return this.mAverageValue;
  }

  private void updateAverageValue()
  {
    int numberOfElementsToConsider = mNumberOfFrames;
    if (!mIsFull) {
      numberOfElementsToConsider = mCurrentIndex + 1;
    }

    if (numberOfElementsToConsider == 1) {
      this.mAverageValue = mValues[0];
      return;
    }

    // Formula: http://en.wikipedia.org/wiki/Circular_mean
    double sumSin = 0.0;
    double sumCos = 0.0;
    for (int i = 0; i < numberOfElementsToConsider; i++) {
      double v = mValues[i];
      sumSin += Math.sin(v);
      sumCos += Math.cos(v);
    }
    this.mAverageValue = Math.atan2(sumSin, sumCos);
  }
}