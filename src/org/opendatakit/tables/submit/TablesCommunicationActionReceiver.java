package org.opendatakit.tables.submit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.opendatakit.submit.flags.BroadcastExtraKeys;
import org.opendatakit.submit.flags.CommunicationState;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.sync.SyncProcessor;
import org.opendatakit.tables.sync.SynchronizationResult;
import org.opendatakit.tables.sync.Synchronizer;
import org.opendatakit.tables.sync.aggregate.AggregateSynchronizer;
import org.opendatakit.tables.sync.exceptions.InvalidAuthTokenException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SyncResult;
import android.os.AsyncTask;
import android.os.Parcelable;
import android.util.Log;

/**
 * The broadcast receiver responsible for dealing with Submit. Based on 
 * Waylon's sample app
 * org.opendatakit.submittest.CommunicationActionReceiver.java.
 * @author sudar.sam@gmail.com
 *
 */
public class TablesCommunicationActionReceiver extends BroadcastReceiver {
  
  private static final String TAG = 
      TablesCommunicationActionReceiver.class.getSimpleName();
  
  private static final int NUM_RETRIES_BEFORE_STOP = 10;
  
  private static TablesCommunicationActionReceiver mSingleton = null;
  
  public static TablesCommunicationActionReceiver getInstance(String appUuid, 
      String submitId, String aggregateServerUri, String authToken,
      Context context) {
    if (mSingleton == null) {
      mSingleton = new TablesCommunicationActionReceiver(appUuid, submitId,
          aggregateServerUri, authToken, context);
    } 
    return mSingleton;
  }
  
  private final Context mContext;
  private final Collection<String> mSendObjects;
  private final Map<String, Integer> mDataIdToFailureCount;
  private String mSubmitChannelUuid;
  
  /** Flag saying whether or not we're already in the middle of a sync. */
  private boolean mCurrentlySynching;
  
  /** 
   * The url of the server running Aggregate. Potentially changeable in the 
   * future.
   */
  private String mAggregateServerUri;
  /**
   * Auth token the user is using. This can expire and should eventually be 
   * smarter about getting the new one.
   */
  private String mAuthToken;
  
  private TablesCommunicationActionReceiver(String appUuid, String submitId,
      String aggregateServerUri, String authToken, Context context) {
    super();
    this.mContext = context;
    this.mCurrentlySynching = false;
    this.mAggregateServerUri = aggregateServerUri;
    this.mAuthToken = authToken;
    this.mSendObjects = new ArrayList<String>();
    this.mDataIdToFailureCount = new HashMap<String, Integer>();
    this.mSubmitChannelUuid = submitId;
    IntentFilter filter = new IntentFilter();
    filter.addAction(appUuid);
    this.mContext.registerReceiver(this, filter);
  }
  
  /**
   * Add the uid to the collection of Strings that this receiver is listening
   * for.
   * @param uid
   */
  public void listenForUid(String uid) {
    this.mSendObjects.add(uid);
  }
  
  public void destroy() {
    this.mContext.unregisterReceiver(this);
  }
  
  public void setSubmitId(String submitId) {
    this.mSubmitChannelUuid = submitId;
  }

  @Override
  public void onReceive(Context context, Intent intent) {
    if (!intent.hasExtra(BroadcastExtraKeys.SUBMIT_OBJECT_ID) ||
        !intent.hasExtra(BroadcastExtraKeys.COMMUNICATION_STATE)) {
      // we need both of these things, so log an error and do nothing.
      Log.e(TAG, "Broadcast is missing necessary info. Doing nothing.");
      return;
    }
    // Retrieve the ID of the data object Submit is telling us to send.
    String sendUid = 
        intent.getStringExtra(BroadcastExtraKeys.SUBMIT_OBJECT_ID);
    // Now make sure this data object has been registered for us, otherwise
    // there was a miscommunication somewhere.
    if (!this.mSendObjects.contains(sendUid)) {
      Log.e(TAG, "Data object id was received but not meant for this app.");
      return;
    }
    // Otherwise, try and actually perform the sync.
    String result = 
        intent.getStringExtra(BroadcastExtraKeys.COMMUNICATION_STATE);
    CommunicationState state = CommunicationState.valueOf(result);
    if (state == CommunicationState.SEND){
      // Then we fire up the sending.
      Log.d(TAG, "received communication SEND");
      if (this.mCurrentlySynching) {
        Log.d(TAG, "already synching, not trying again");
      } else {
        Log.d(TAG, "not synching, beginning a sync");
        SyncNowWithSubmitTask submitTask = new SyncNowWithSubmitTask(sendUid);
        submitTask.execute();
      }
    } else {
      Log.i(TAG, "communication state was not send, was: " + state);
    }
  }
  
  public class SyncNowWithSubmitTask extends 
      AsyncTask<Void, Void, SynchronizationResult> {
    
    private boolean mSuccess;
    private String mDataUidToSend;
    
    public SyncNowWithSubmitTask(String dataUidToSend) {
      this.mSuccess = false;
      mCurrentlySynching = true;
      this.mDataUidToSend = dataUidToSend;
    }
    
    @Override
    protected SynchronizationResult doInBackground(Void... params) {
      SynchronizationResult result = null;
      try {
        DbHelper dbh = DbHelper.getDbHelper(mContext);
        Synchronizer synchronizer = 
            new AggregateSynchronizer(mAggregateServerUri, mAuthToken);
        SyncProcessor syncProcessor = new SyncProcessor(dbh, synchronizer, 
            new SyncResult());
        // This is going to use Submit in demo mode: only synching the files.
        // We will do app-level and non media files, and then ask Submit to do
        // the media files separately.
        result = syncProcessor.synchronize(true, true, false);
        // TODO: hand off the media files.
        mSuccess = true;
      } catch (InvalidAuthTokenException e) {
        Log.e(TAG, "authtoken was invalid");
        mSuccess = false;
        Log.e(TAG, "invalid auth token, need to reauthorize");
      } catch (Exception e) {
        Log.e(TAG, "exception during synchronization. Stack trace:\n" +
            Arrays.toString(e.getStackTrace()));
        mSuccess = false;
      }      
      Intent intent = new Intent();
      intent.setAction(mSubmitChannelUuid);
      intent.putExtra(BroadcastExtraKeys.SUBMIT_OBJECT_ID, mDataUidToSend);
      if (mSuccess) {
        intent.putExtra(BroadcastExtraKeys.COMMUNICATION_STATE, 
            CommunicationState.SUCCESS.toString());
//            (Parcelable) CommunicationState.SUCCESS.toString());
        mDataIdToFailureCount.remove(mDataUidToSend);
        mSendObjects.remove(mDataUidToSend);
        mContext.sendBroadcast(intent);
      } else if (mDataIdToFailureCount.get(mDataUidToSend) == null ||
          mDataIdToFailureCount.get(mDataUidToSend) < NUM_RETRIES_BEFORE_STOP){
        intent.putExtra(BroadcastExtraKeys.COMMUNICATION_STATE, 
            CommunicationState.FAILURE_RETRY.toString());
//            (Parcelable) CommunicationState.FAILURE_RETRY);
        if (mDataIdToFailureCount.get(mDataUidToSend) == null) {
          mDataIdToFailureCount.put(mDataUidToSend, 1);
        } else {
          int numFailed = mDataIdToFailureCount.get(mDataUidToSend);
          numFailed++;
          mDataIdToFailureCount.put(mDataUidToSend, numFailed);
        }
        Log.i(TAG, "telling submit to retry");
        mContext.sendBroadcast(intent);
      } else if (mDataIdToFailureCount.get(mDataUidToSend) >= 
          NUM_RETRIES_BEFORE_STOP) {
        Log.i(TAG, "telling Submit to stop trying to send");
        intent.putExtra(BroadcastExtraKeys.COMMUNICATION_STATE, 
            CommunicationState.FAILURE_NO_RETRY.toString());
//            (Parcelable) CommunicationState.FAILURE_NO_RETRY);
        mContext.sendBroadcast(intent);
      } else {
        Log.e(TAG, "somehow logic on retry count is invalid, not sending" +
            " correct message back to submit.");
      }
      mCurrentlySynching = false;
      return result;
    }
  }

}