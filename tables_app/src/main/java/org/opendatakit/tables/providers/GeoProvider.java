package org.opendatakit.tables.providers;

import android.app.Activity;
import android.content.Context;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Surface;
import org.opendatakit.tables.logic.AverageAngle;

/**
 * @author belendia@gmail.com
 */

public class GeoProvider implements SensorEventListener, LocationListener {
  public static final String TAG = GeoProvider.class.getSimpleName();

  /**
   * Interface definition for a callback to be invoked when the bearing
   * changes.
   */
  public interface DirectionEventListener {
    void onHeadingToNorthChanged(float heading);
    void onBearingToDestinationLocationChanged(float bearing, float heading);
  }

  public interface LocationEventListener {
    void onLocationChanged(Location location);
    void onProviderDisabled(String provider);
    void onProviderEnabled(String provider);
    void onStatusChanged(String provider, int status, Bundle extras);
  }

  private final SensorManager mSensorManager;
  private final LocationManager mLocationManager;

  private final Sensor mAccelerometer;
  private final Sensor mMagneticField;

  /**
  * Intermediate values read from the sensors, used to
  * calculate our azimuth value
  */
  private float[] mValuesAccelerometer;
  private float[] mValuesMagneticField;
  private float[] mTempRotationMatrix;
  private float[] mRotationMatrix;
  private float[] mMatrixI;

  private boolean mIsGPSOn = false;
  private boolean mIsNetworkOn = false;

  private boolean mHasAccelerometer = false;
  private boolean mHasMagnetometer = false;

  /**
   * minimum change of bearing (degrees) to notify the direction listener
   */
  private final double mMinDiffForEvent;

  /**
   * minimum delay (millis) between notifications for the direction listener
   */
  private final double mThrottleTime;

  /**
   * the direction event listener
   */
  private DirectionEventListener mDirectionEventListener;

  /**
   * the location event listener
   */
  private LocationEventListener mLocationEventListener;

  /**
   * angle to magnetic north
   */
  private AverageAngle mAzimuthRadians;

  /**
   * smoothed angle to magnetic north
   */
  private double mAzimuth = Double.NaN;

  /**
   * angle to true north
   */
  private double mBearing = Double.NaN;

  /**
   * last notified angle to true north
   */
  private double mLastBearing = Double.NaN;

  /**
   * Current GPS/WiFi location
   */
  private Location mLocation;

  /**
   *
   */
  private Location mDestinationLocation;

  /**
   * when we last dispatched the change event
   */
  private long mLastChangeDispatchedAt = -1;

  private Activity mActivity;
  /**
   * Default constructor.
   *
   * @param activity
   *            Application Context
   */
  public GeoProvider(Activity activity) {
    this(activity, 15, 0.5, 100);
  }

  /**
   * @param activity
   *            Application Context
   * @param smoothing
   *            the number of measurements used to calculate a mean for the
   *            azimuth. Set this to 1 for the smallest delay. Setting it to
   *            5-10 to prevents the needle from going crazy
   * @param minDiffForEvent
   *            minimum change of bearing (degrees) to notify the change
   *            listener
   * @param throttleTime
   *            minimum delay (millis) between notifications for the change
   *            listener
   */
  @SuppressWarnings("MissingPermission")
  public GeoProvider(Activity activity, int smoothing,
      double minDiffForEvent, int throttleTime) {
    mActivity = activity;

    mLocationManager = (LocationManager) mActivity.getSystemService(Context.LOCATION_SERVICE);

    mSensorManager = (SensorManager) mActivity.getSystemService(Context.SENSOR_SERVICE);
    mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    mMagneticField = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

    mValuesAccelerometer = new float[3];
    mValuesMagneticField = new float[3];

    mTempRotationMatrix = new float[9];
    mRotationMatrix = new float[9];
    mMatrixI = new float[9];

    mMinDiffForEvent = minDiffForEvent;
    mThrottleTime = throttleTime;

    mAzimuthRadians = new AverageAngle(smoothing);

    for (final String provider : mLocationManager.getProviders(true)) {
      if (LocationManager.GPS_PROVIDER.equals(provider)
          || LocationManager.NETWORK_PROVIDER.equals(provider)) {
        if (mLocation == null) {
          // TODO: Add lint exception for this, and also check for permission in Navigate fragment
          mLocation = mLocationManager.getLastKnownLocation(provider);
        }

        if (LocationManager.GPS_PROVIDER.equals(provider)) {
          mIsGPSOn = true;
        } else if (LocationManager.NETWORK_PROVIDER.equals(provider)) {
          mIsNetworkOn = true;
        }
      }
    }

  }

  // ==============================================================================================
  // Public API
  // ==============================================================================================

  /**
   * Call this method to start bearing updates.
   */
  @SuppressWarnings("MissingPermission")
  public boolean start() {
    boolean deviceHasSensors = true;

    mHasAccelerometer = mSensorManager.registerListener(this, mAccelerometer,
        SensorManager.SENSOR_DELAY_NORMAL);
    mHasMagnetometer = mSensorManager.registerListener(this, mMagneticField,
        SensorManager.SENSOR_DELAY_NORMAL);

    if(mHasAccelerometer == false && mHasMagnetometer == false) {
      unregisterSensorsListener();
      deviceHasSensors = false;
    }

    if (isGpsProviderOn()) {
      mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
    }

    if (isNetworkOn()) {
      mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
    }

    return deviceHasSensors;
  }

  /**
   * call this method to stop bearing updates.
   */
  public void stop() {
    unregisterSensorsListener();
    mLocationManager.removeUpdates(this);
  }

  public void unregisterSensorsListener() {
    mSensorManager.unregisterListener(this, mAccelerometer);
    mSensorManager.unregisterListener(this, mMagneticField);
  }

  public boolean isGpsProviderOn() {
    return mIsGPSOn;
  }

  public boolean isNetworkOn() {
    return mIsNetworkOn;
  }

  /**
   * @return current bearing
   */
  public double getBearing() {
    return mBearing;
  }

  /**
   * Returns the bearing event listener to which bearing events must be sent.
   *
   * @return the bearing event listener
   */
  public DirectionEventListener getChangeEventListener() {
    return mDirectionEventListener;
  }

  /**
   *
   */
  public LocationEventListener getLocationListener() {
    return mLocationEventListener;
  }

  /**
   * Specifies the bearing event listener to which bearing events must be
   * sent.
   *
   * @param listener
   *            the bearing event listener
   */
  public void setDirectionEventListener(DirectionEventListener listener) {
    this.mDirectionEventListener = listener;
  }

  /**
   *
   */

  public void setLocationEventListener(
      LocationEventListener locationEventListener) {
    this.mLocationEventListener = locationEventListener;
  }

  // ==============================================================================================
  // SensorEventListener implementation
  // ==============================================================================================

  @Override
  public void onSensorChanged(SensorEvent event) {
    switch (event.sensor.getType()) {
    case Sensor.TYPE_ACCELEROMETER:
      System.arraycopy(event.values, 0, mValuesAccelerometer, 0, 3);
      break;
    case Sensor.TYPE_MAGNETIC_FIELD:
      System.arraycopy(event.values, 0, mValuesMagneticField, 0, 3);
      break;
    }

    if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER || event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {

      // calculate a new smoothed azimuth value and store to mAzimuth
      if (SensorManager.getRotationMatrix(mTempRotationMatrix, mMatrixI,
          mValuesAccelerometer, mValuesMagneticField)) {

        //remapCoordinate();

        float[] orientation = new float[3];
        //SensorManager.getOrientation(mRotationMatrix, orientation);
        SensorManager.getOrientation(mTempRotationMatrix, orientation);
        mAzimuthRadians.add(orientation[0]);
        mAzimuth = (Math.toDegrees(mAzimuthRadians.getAverage()) + 360 ) % 360;

        // update mBearing
        updateBearing();
      }
    }
  }

  private void remapCoordinate() {
    switch (mActivity.getWindowManager().getDefaultDisplay().getRotation()) {
    case Surface.ROTATION_0: // Portrait
      // device natural position
      SensorManager.remapCoordinateSystem(mTempRotationMatrix, SensorManager.AXIS_X,
          SensorManager.AXIS_Y, mRotationMatrix);
      break;
    case Surface.ROTATION_90: // Landscape
      // device rotated 90 deg counterclockwise
      SensorManager.remapCoordinateSystem(mTempRotationMatrix, SensorManager.AXIS_Y,
          SensorManager.AXIS_MINUS_X, mRotationMatrix);
      break;
    case Surface.ROTATION_180: // Portrait
      // device rotated 180 deg counterclockwise
      SensorManager.remapCoordinateSystem(mTempRotationMatrix, SensorManager.AXIS_MINUS_X,
          SensorManager.AXIS_MINUS_Y, mRotationMatrix);
      break;
    case Surface.ROTATION_270: // Landscape
      // device rotated 270 deg counterclockwise
      SensorManager.remapCoordinateSystem(mTempRotationMatrix, SensorManager.AXIS_MINUS_Y,
          SensorManager.AXIS_X, mRotationMatrix);
      break;
    }
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int accuracy) {

  }

  // ==============================================================================================
  // LocationListener implementation
  // ==============================================================================================

  @Override
  public void onLocationChanged(Location location) {
    // set the new location
    this.mLocation = location;

    // update mBearing
    updateBearing();

    if (mLocationEventListener != null) {
      mLocationEventListener.onLocationChanged(location);
    }
  }

  @Override
  public void onStatusChanged(String s, int i, Bundle bundle) {
    if (mLocationEventListener != null) {
      mLocationEventListener.onStatusChanged(s, i, bundle);
    }
  }

  @Override
  public void onProviderEnabled(String provider) {
    if (LocationManager.GPS_PROVIDER.equals(provider)) {
      mIsGPSOn = true;
    } else if (LocationManager.NETWORK_PROVIDER.equals(provider)) {
      mIsNetworkOn = true;
    }

    if (mLocationEventListener != null) {
      mLocationEventListener.onProviderEnabled(provider);
    }
  }

  @Override
  public void onProviderDisabled(String provider) {
    if (LocationManager.GPS_PROVIDER.equals(provider)) {
      mIsGPSOn = false;
    } else if (LocationManager.NETWORK_PROVIDER.equals(provider)) {
      mIsNetworkOn = false;
    }

    if (mLocationEventListener != null) {
      mLocationEventListener.onProviderDisabled(provider);
    }
  }

  // ==============================================================================================
  // Private Utilities
  // ==============================================================================================

  private void updateBearing() {
    if (!Double.isNaN(this.mAzimuth)) {
      if (this.mLocation == null) {
        // Log.w(TAG, "Location is NULL bearing is not true north!");
        mBearing = mAzimuth;
      } else {
        mBearing = getBearingForLocation(this.mLocation);
      }

      // Throttle dispatching based on mThrottleTime and minDiffForEvent
      if (System.currentTimeMillis() - mLastChangeDispatchedAt > mThrottleTime
          && (Double.isNaN(mLastBearing) || Math.abs(mLastBearing
          - mBearing) >= mMinDiffForEvent)) {
        mLastBearing = mBearing;
        if (mDirectionEventListener != null) {
          mDirectionEventListener
              .onHeadingToNorthChanged((float) mBearing);

          if (mDestinationLocation != null && this.mLocation != null) {
            float destinationBearing = getDestinationBearing(this.mLocation);
            mDirectionEventListener.onBearingToDestinationLocationChanged(
                destinationBearing, (float) mBearing);
          }
        }

        mLastChangeDispatchedAt = System.currentTimeMillis();
      }
    }
  }

  private float getDestinationBearing(Location location) {
    float destinationBearing = location.bearingTo(mDestinationLocation);
    destinationBearing = (destinationBearing + 360 ) % 360;

    return destinationBearing;
  }

  private double getBearingForLocation(Location location) {
    return mAzimuth + getGeomagneticField(location).getDeclination();
  }

  private GeomagneticField getGeomagneticField(Location location) {
    GeomagneticField geomagneticField = new GeomagneticField(
        (float) location.getLatitude(),
        (float) location.getLongitude(),
        (float) location.getAltitude(), System.currentTimeMillis());
    return geomagneticField;
  }

  /***
   * Returns orientation depending on degrees
   *
   * @param azimuth
   *            Value between 0 and 359
   * @return A 1- or 2-character String with "N", "NE", "E", "SE", "S", "SW",
   *         "W" or "NW" if input is in between 0 and 359, and "" otherwise.
   */
  public String getDegToGeo(float azimuth) {
    if (azimuth < 23 || azimuth > 337)
      return "N";
    else if (azimuth > 22 && azimuth < 68)
      return "NE";
    else if (azimuth > 67 && azimuth < 113)
      return "E";
    else if (azimuth > 112 && azimuth < 158)
      return "SE";
    else if (azimuth > 157 && azimuth < 203)
      return "S";
    else if (azimuth > 202 && azimuth < 248)
      return "SW";
    else if (azimuth > 247 && azimuth < 293)
      return "W";
    else if (azimuth > 292 && azimuth < 338)
      return "NW";
    else
      return "";
  }

  public Location getCurrentLocation() {
    return mLocation;
  }

  public Location getDestinationLocation() {
    return mDestinationLocation;
  }

  public void setDestinationLocation(Location destinationLocation) {
    mDestinationLocation = destinationLocation;
  }

  public void clearDestinationLocation() {
    mDestinationLocation = null;
    mBearing = Double.NaN;
    mLastBearing = Double.NaN;
  }
}