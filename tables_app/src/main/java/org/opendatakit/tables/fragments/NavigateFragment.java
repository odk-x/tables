package org.opendatakit.tables.fragments;

import android.app.Activity;
import androidx.fragment.app.Fragment;
import android.content.Intent;
import android.location.Location;
import android.location.LocationProvider;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.todddavies.components.progressbar.ProgressWheel;
import org.opendatakit.activities.IOdkDataActivity;
import org.opendatakit.consts.IntentConsts;
import org.opendatakit.data.utilities.TableUtil;
import org.opendatakit.database.data.*;
import org.opendatakit.database.service.DbHandle;
import org.opendatakit.database.service.UserDbInterface;
import org.opendatakit.exception.ServicesAvailabilityException;
import org.opendatakit.logging.WebLogger;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.TableDisplayActivity;
import org.opendatakit.tables.application.Tables;
import org.opendatakit.tables.providers.GeoProvider;
import org.opendatakit.tables.utils.DistanceUtil;
import org.opendatakit.tables.views.CompassView;

import java.text.DecimalFormat;

/**
 * Fragment displaying the navigate module
 *
 * @author belendia@gmail.com
 *
 */
public class NavigateFragment extends Fragment implements IMapListViewCallbacks,
    GeoProvider.DirectionEventListener, GeoProvider.LocationEventListener{

  private static final String TAG = NavigateFragment.class.getSimpleName();

  /**
   * Represents an index that can't possibly be in the list
   */
  public static final int INVALID_INDEX = -1;

  private static final String ROW_ID_KEY = "rowId";

  /**
   * Saves the index of the element that was selected.
   */
  private static final String INTENT_KEY_SELECTED_INDEX = "keySelectedIndex";

  /**
   * The index of an item that has been selected by the user.
   * We must default to invalid index because the initial load of the list view may take place
   * before onCreate is called
   */
  protected int mSelectedItemIndex = INVALID_INDEX;

  private enum SignalState {
    NO_SIGNAL, POOR_SIGNAL, MODERATE_SIGNAL, GOOD_SIGNAL
  }

  private GeoProvider mGeoProvider;

  // default location accuracy
  private static final double GOOD_LOCATION_ACCURACY = 10;
  private static final double MODERATE_LOCATION_ACCURACY = 50;

  private ProgressWheel mSignalQualitySpinner;

  private TextView mHeadingTextView;
  private TextView mBearingTextView;
  private TextView mDistanceTextView;

  private CompassView mCompass;
  private CompassView mDestinationLocation;

  private UserTable mTable;
  private ColumnDefinition mLatitudeColumn;
  private ColumnDefinition mLongitudeColumn;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (savedInstanceState != null) {
      this.mSelectedItemIndex = savedInstanceState.containsKey(INTENT_KEY_SELECTED_INDEX) ?
          savedInstanceState.getInt(INTENT_KEY_SELECTED_INDEX) :
          INVALID_INDEX;
    }
  }

  public static NavigateFragment newInstance(String rowId) {
    NavigateFragment f = new NavigateFragment();

    if (rowId != null && !rowId.isEmpty()) {
      Bundle args = new Bundle();
      args.putString(ROW_ID_KEY, rowId);
      f.setArguments(args);
    }
    return f;
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    TableDisplayActivity activity = (TableDisplayActivity) getActivity();

    mGeoProvider = new GeoProvider(activity );
    mGeoProvider.setDirectionEventListener(this);
    mGeoProvider.setLocationEventListener(this);

    mSignalQualitySpinner = activity .findViewById(R.id.signalQualitySpinner);
    mCompass = activity .findViewById(R.id.compass);
    mDestinationLocation = getActivity().findViewById(R.id.destination);

    mBearingTextView = activity .findViewById(R.id.bearingTextView);
    mHeadingTextView = activity .findViewById(R.id.headingTextView);
    mDistanceTextView = activity .findViewById(R.id.distanceTextView);
    mDistanceTextView.setText(getActivity().getString(R.string.distance,
        "-"));

    Button arriveButton = activity .findViewById(R.id.navigate_arrive_button);
    arriveButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        arrive(v);
      }
    });
    Button cancelButton = activity .findViewById(R.id.navigate_cancel_button);
    cancelButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        cancel(v);
      }
    });

    mTable = activity.getUserTable();
    OrderedColumns orderedDefns = activity.getColumnDefinitions();

    UserDbInterface dbInterface = Tables.getInstance().getDatabase();

    try {
      DbHandle db = dbInterface.openDatabase(activity.getAppName());

      mLatitudeColumn = orderedDefns.find(getLatitudeElementKey(db));
      mLongitudeColumn = orderedDefns.find(getLongitudeElementKey(db));
    } catch (ServicesAvailabilityException e) {
      WebLogger.getLogger(activity.getAppName()).printStackTrace(e);
      WebLogger.getLogger(activity.getAppName()).e(TAG, "Unable to access database");
    }

    // Check for a passed in rowId to default to. Only use it if we haven't already restored a
    // selected index
    Bundle args = getArguments();
    if (args != null && this.mSelectedItemIndex == INVALID_INDEX &&
        args.containsKey(ROW_ID_KEY)) {
      String rowId = args.getString(ROW_ID_KEY);
      setIndexOfSelectedItem(mTable.getRowNumFromId(rowId));
    }

    if (mGeoProvider.isGpsProviderOn() == false
        && mGeoProvider.isNetworkOn() == false) {
      setSpinnerColor(SignalState.NO_SIGNAL);
      Toast.makeText(getActivity(),
          getString(R.string.location_unavailable),
          Toast.LENGTH_SHORT).show();
    } else {
      setSpinnerColor(SignalState.POOR_SIGNAL);
    }
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putInt(INTENT_KEY_SELECTED_INDEX, mSelectedItemIndex);
  }

  @Override
  public void onResume() {
    super.onResume();

    mGeoProvider.start();
    if (mGeoProvider.isGpsProviderOn()
        || mGeoProvider.isNetworkOn()) {
      setSpinnerColor(SignalState.POOR_SIGNAL);
      mSignalQualitySpinner.startSpinning();
    }
  }

  @Override
  public void onPause() {
    super.onPause();

    mGeoProvider.stop();

    mSignalQualitySpinner.stopSpinning();
    mSignalQualitySpinner.setText(getString(R.string.acc_value));
  }

  @Override
  public void onLocationChanged(Location location) {
    updateNotification();
    if (isAdded()) {
      updateDistance(location);
    }
  }

  @Override
  public void onProviderDisabled(String provider) {

    if (mGeoProvider.isGpsProviderOn() == false
        && mGeoProvider.isNetworkOn() == false) {
      updateNotification();

      mSignalQualitySpinner.stopSpinning();
      mSignalQualitySpinner.setText(getString(R.string.acc_value));

      setSpinnerColor(SignalState.NO_SIGNAL);
    }
  }

  @Override
  public void onProviderEnabled(String provider) {
    if (mGeoProvider.isGpsProviderOn()
        || mGeoProvider.isNetworkOn()) {
      updateNotification();
      setSpinnerColor(SignalState.POOR_SIGNAL);
      mSignalQualitySpinner.startSpinning();
    }
  }

  @Override
  public void onStatusChanged(String provider, int status, Bundle extras) {
    switch (status) {
    case LocationProvider.AVAILABLE:
      if (mGeoProvider.getCurrentLocation() != null) {
        updateNotification();
      }
      break;
    case LocationProvider.OUT_OF_SERVICE:
      break;
    case LocationProvider.TEMPORARILY_UNAVAILABLE:
      break;
    }
  }

  @Override
  public void onHeadingToNorthChanged(float heading) {
    if (isAdded()) {
      mHeadingTextView.setText(getActivity().getString(R.string.heading,
          String.valueOf((int) (heading)),
          mGeoProvider.getDegToGeo(heading)));
      mCompass.setDegrees(heading);
    }
  }

  @Override
  public void onBearingToDestinationLocationChanged(float bearing, float heading) {
    if (isAdded()) {

      mBearingTextView.setText(getActivity().getString(R.string.bearing,
          String.valueOf((int) (bearing)),
          mGeoProvider.getDegToGeo(bearing)));

      float rotation = 360 - bearing + heading;

      mDestinationLocation.setDegrees(rotation);
    }
  }

  private void updateDistance(Location location) {
    if (mGeoProvider.getDestinationLocation() != null) {
      double distance = DistanceUtil.getDistance(mGeoProvider
          .getDestinationLocation().getLatitude(), mGeoProvider
          .getDestinationLocation().getLongitude(), location
          .getLatitude(), location.getLongitude());
      mDistanceTextView.setText(getActivity().getString(
          R.string.distance,
          DistanceUtil.getFormatedDistance(distance)));
    }
  }

  private void updateNotification() {
    Location location = mGeoProvider.getCurrentLocation();
    if (isAdded() && location != null) {
      mSignalQualitySpinner.setText(truncateDouble(
          location.getAccuracy(), 2)
          + " " + getString(R.string.meter_shorthand));

      if (location.getAccuracy() > 0
          && location.getAccuracy() <= GOOD_LOCATION_ACCURACY) {
        setSpinnerColor(SignalState.GOOD_SIGNAL);
      } else if (location.getAccuracy() > GOOD_LOCATION_ACCURACY
          && location.getAccuracy() <= MODERATE_LOCATION_ACCURACY) {
        setSpinnerColor(SignalState.MODERATE_SIGNAL);
      } else if (location.getAccuracy() > MODERATE_LOCATION_ACCURACY) {
        setSpinnerColor(SignalState.POOR_SIGNAL);
      } else {
        setSpinnerColor(SignalState.NO_SIGNAL);
      }
    }
  }

  private void setSpinnerColor(SignalState signal) {
    Activity activity = getActivity();

    int textColor;
    int barColor;
    int rimColor;// circle border color

    switch (signal) {
    case GOOD_SIGNAL:
      textColor = ContextCompat.getColor(activity, R.color.spinner_text_color_green);
      barColor = ContextCompat.getColor(activity, R.color.spinner_bar_color_green);
      rimColor = ContextCompat.getColor(activity, R.color.spinner_rim_color_green);
      break;
    case MODERATE_SIGNAL:
      textColor = ContextCompat.getColor(activity, R.color.spinner_text_color_yellow);
      barColor = ContextCompat.getColor(activity, R.color.spinner_bar_color_yellow);
      rimColor = ContextCompat.getColor(activity, R.color.spinner_rim_color_yellow);
      break;
    case POOR_SIGNAL:
      textColor = ContextCompat.getColor(activity, R.color.spinner_text_color_red);
      barColor = ContextCompat.getColor(activity, R.color.spinner_bar_color_red);
      rimColor = ContextCompat.getColor(activity, R.color.spinner_rim_color_red);
      break;
    case NO_SIGNAL:
      textColor = ContextCompat.getColor(activity, R.color.spinner_text_color_black);
      barColor = ContextCompat.getColor(activity, R.color.spinner_bar_color_black);
      rimColor = ContextCompat.getColor(activity, R.color.spinner_rim_color_black);
      break;
    default:
      textColor = ContextCompat.getColor(activity, R.color.spinner_text_color_black);
      barColor = ContextCompat.getColor(activity, R.color.spinner_bar_color_black);
      rimColor = ContextCompat.getColor(activity, R.color.spinner_rim_color_black);
      break;
    }

    mSignalQualitySpinner.setTextColor(textColor);
    mSignalQualitySpinner.setBarColor(barColor);
    mSignalQualitySpinner.setRimColor(rimColor);
  }

  private String truncateDouble(double number, int digitsAfterDouble) {
    StringBuilder numOfDigits = new StringBuilder();
    for (int i = 0; i < digitsAfterDouble; i++) {
      numOfDigits.append("#");
    }
    DecimalFormat df = new DecimalFormat("#"
        + (digitsAfterDouble > 0 ? "." + numOfDigits.toString() : ""));
    return df.format(number);
  }


  /**
   * Resets the view (the list), and sets the visibility to visible.
   */
  void resetView() {

    // do not initiate reload until we have the database set up...
    Activity activity = getActivity();
    if (activity instanceof IOdkDataActivity) {
      if (((IOdkDataActivity) activity).getDatabase() == null) {
        return;
      }
    } else {
      Log.e(TAG,
          "Problem: NavigateView not being rendered from activity that is an " +
              "IOdkDataActivity");
      return;
    }

    if (mSelectedItemIndex == INVALID_INDEX) {
      mGeoProvider.clearDestinationLocation();
      mDistanceTextView.setText(getActivity().getString(
          R.string.distance, "-"));
      mBearingTextView.setText("");
      mDestinationLocation.setVisibility(View.GONE);
      return;
    } else {
      mDestinationLocation.setVisibility(View.VISIBLE);
    }

    TypedRow selectedRow = mTable.getRowAtIndex(mSelectedItemIndex);
    String lat = selectedRow.getStringValueByKey(mLatitudeColumn.getElementKey());
    String lon = selectedRow.getStringValueByKey(mLongitudeColumn.getElementKey());

    Location destination = new Location(TAG);
    destination.setLatitude(Double.parseDouble(lat));
    destination.setLongitude(Double.parseDouble(lon));

    mGeoProvider.setDestinationLocation(destination);
    if (mGeoProvider.getCurrentLocation() != null) {
      updateDistance(mGeoProvider.getCurrentLocation());
    } else {
      mDistanceTextView.setText(getActivity().getString(
          R.string.distance, "-"));
    }
  }

  /**
   * Informs the list view that no item is selected. Resets the state after a
   * call to {@link #setIndexOfSelectedItem(int)}.
   */
  @Override
  public void setNoItemSelected() {
    this.mSelectedItemIndex = INVALID_INDEX;
    // TODO: Make this work with async API
    this.resetView();
  }

  public int getIndexOfSelectedItem() {
    return this.mSelectedItemIndex;
  }

  /**
   * Sets the index of the item to navigate to, which will be the row of the data wanting
   * to be displayed.
   */
  @Override
  public void setIndexOfSelectedItem(final int index) {
    this.mSelectedItemIndex = index;
    // TODO: Make this work with async API
    this.resetView();
  }

  private String getLatitudeElementKey(DbHandle dbHandle) throws ServicesAvailabilityException {
    TableDisplayActivity activity = (TableDisplayActivity) getActivity();

    OrderedColumns orderedDefns = activity.getColumnDefinitions();
    return TableUtil.get()
        .getMapListViewLatitudeElementKey(Tables.getInstance().getDatabase(),
            activity.getAppName(), dbHandle, activity.getTableId(), orderedDefns);
  }

  private String getLongitudeElementKey(DbHandle dbHandle) throws ServicesAvailabilityException {
    TableDisplayActivity activity = (TableDisplayActivity) getActivity();
    OrderedColumns orderedDefns = activity.getColumnDefinitions();
    return TableUtil.get()
        .getMapListViewLongitudeElementKey(Tables.getInstance().getDatabase(),
            activity.getAppName(), dbHandle, activity.getTableId(), orderedDefns);
  }

  private void arrive(View view) {
    if (mSelectedItemIndex == INVALID_INDEX) {
      getActivity().setResult(Activity.RESULT_CANCELED);
      getActivity().finish();
      return;
    }

    Intent data = new Intent();
    data.putExtra(IntentConsts.INTENT_KEY_ROW_ID, mTable.getRowId(mSelectedItemIndex));

    getActivity().setResult(Activity.RESULT_OK, data);
    getActivity().finish();
  }

  private void cancel(View view) {
    getActivity().setResult(Activity.RESULT_CANCELED);
    getActivity().finish();
  }

}