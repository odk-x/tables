package org.opendatakit.tables.submit;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opendatakit.common.android.utilities.ODKFileUtils;
import org.opendatakit.submit.address.HttpAddress;
import org.opendatakit.submit.data.DataPropertiesObject;
import org.opendatakit.submit.data.SendObject;
import org.opendatakit.submit.flags.DataSize;
import org.opendatakit.submit.flags.HttpFlags;
import org.opendatakit.submit.service.ClientRemote;
import org.opendatakit.tables.sync.aggregate.AggregateSynchronizer;
import org.opendatakit.tables.utils.TableFileUtils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

/**
 * Implementation to integrate with Submit service. Based on Waylon's
 * implementation in org.opendatakit.submittest.Tester.java.
 * @author sudar.sam@gmail.com
 *
 */
public class ServiceConnectionImpl implements ServiceConnection {

  /** The package name of submit. */
  private static final String SUBMIT_PACKAGE_NAME =
      "org.opendatakit.submit";
  /** The class name of the submit service. */
  private static final String SUBMIT_SERVICE_CLASS_NAME =
      "org.opendatakit.submit.service.SubmitService";

  private static final String TAG =
      ServiceConnectionImpl.class.getSimpleName();

  private Context mComponentContext;
  private boolean mIsBoundToService;
  private ClientRemote mSubmitService; // RPCs, baby
  private String mAppUuid;

  public ServiceConnectionImpl(String appUuid, Context context) {
    // First we'll set the state.
    this.mAppUuid = appUuid;
    this.mIsBoundToService = false;
    this.mComponentContext = context;

    Log.d(TAG, "[ServiceConnectionImpl] going to bind service");
    // Now bind to the service.
    Intent bindIntent = new Intent();
    bindIntent.setClassName(SUBMIT_PACKAGE_NAME, SUBMIT_SERVICE_CLASS_NAME);
    this.mComponentContext.bindService(bindIntent, this,
        Context.BIND_AUTO_CREATE);
    Log.d(TAG, "[ServiceConnectionImpl] called bindService");
  }

  public String getAppUuid() {
    return this.mAppUuid;
  }

  /**
   * Register the app with submit.
   * @return
   * @throws Exception if the service is not already bound.
   */
  public String registerApp() throws Exception {
    if (!this.mIsBoundToService) {
      Log.e(TAG, "[registerApp] not bound to service");
      throw new Exception("Not bound to submit service but trying to " +
            "register app!");
    }
    return this.mSubmitService.registerApplication(this.mAppUuid);
  }

  /**
   * Register data with the submit service.
   * @return
   * @throws Exception if we're not already bound to the service.
   */
  public String registerData() throws Exception {
    if (!this.mIsBoundToService) {
      Log.e(TAG, "[registerData] not bound to service");
      throw new Exception("Not bound to service but trying to register data!");
    }
    DataPropertiesObject data = new DataPropertiesObject();
    // I'm not sure why we're doing SMALL. Presumably because we're just
    // waiting for an ok-to-sync message and nothing else? Following Waylon's
    // example in the test project.
    data.setDataSize(DataSize.SMALL);
    return this.mSubmitService.register(this.mAppUuid, data);
  }

  public List<String> registerMediaFiles(String tableId, String aggregateUri)
      throws Exception {
    Log.d(TAG, "[registerMediaFiles]");
    if (!this.mIsBoundToService) {
      Log.e(TAG, "[registerMediaFiles] not bound to servce.");
      throw new Exception("Not bound to service but trying to register " +
      		"media files!");
    }
    Map<String, String> absolutePathToUploadUri =
        getFileInfoForSubmit(tableId, aggregateUri);
    Log.d(TAG, "[registerMediaFiles] path->uploadUrl: " + absolutePathToUploadUri);
    List<String> submitFileUuids = new ArrayList<String>();
    for (Map.Entry<String, String> entry :
        absolutePathToUploadUri.entrySet()) {
      submitFileUuids.add(giveFileToSubmit(entry.getKey(), entry.getValue()));
    }
    return submitFileUuids;
  }

  /**
   * Get a map of absolute path to upload url for each file in the table's
   * instances folder.
   * TODO: this is a deeply unsatisfying hack. It doesn't do any checking to
   * get the correct media files or save sync state or anything, it just says
   * sync all. This will have to be corrected.
   * @param tableId
   * @return
   */
  private Map<String, String> getFileInfoForSubmit(String tableId,
      String aggregateUri) {
    String appFolder =
        ODKFileUtils.getAppFolder(TableFileUtils.ODK_TABLES_APP_NAME);
    String relativePathToInstancesFolder = TableFileUtils.DIR_TABLES +
        File.separator + tableId + File.separator + "instances";
//        TableFileUtils.DIR_INSTANCES;
    String instancesFolderFullPath = appFolder + File.separator +
        relativePathToInstancesFolder;
    List<String> relativePathsToAppFolderOnDevice =
        TableFileUtils.getAllFilesUnderFolder(instancesFolderFullPath, null,
            appFolder);
    Map<String, String> absolutePathToUploadUrl =
        new HashMap<String, String>();
    for (String relativePath : relativePathsToAppFolderOnDevice) {
      String absolutePath = appFolder + File.separator + relativePath;
      String uploadUri =
          AggregateSynchronizer.getFilePathURI(aggregateUri).toString();
      uploadUri += relativePath;
      absolutePathToUploadUrl.put(absolutePath, uploadUri);
    }
    return absolutePathToUploadUrl;
  }

  /**
   * Give a file to submit and return the data uuid for that file that Submit
   * has assigned to it. Must be bound to the service.
   * @param absolutePathToFile
   * @param uploadUrl
   */
  private String giveFileToSubmit(String absolutePathToFile, String uploadUrl)
      throws Exception {
    Log.d(TAG, "[giveFileToSubmit] path; url: " + absolutePathToFile + "; "
      + uploadUrl);
    // Following the example in Morgan's test app for files.
    DataPropertiesObject dataPropertiesObject = new DataPropertiesObject();
    dataPropertiesObject.setDataSize(DataSize.LARGE);
    HttpAddress addr = new HttpAddress(uploadUrl, HttpFlags.POST);
    // Don't think we need the headers Morgan is using.
    SendObject send = new SendObject(absolutePathToFile);
    send.addAddress(addr);
    return this.mSubmitService.submit(mAppUuid, dataPropertiesObject, send);
  }

  @Override
  public void onServiceConnected(ComponentName name, IBinder service) {
    Log.d(TAG, "[onServiceConnected] bound to submit service");
    this.mSubmitService = ClientRemote.Stub.asInterface(service);
    this.mIsBoundToService = true;
    // This call relies on the CAR being set up BEFORE the call to the
    // service.
    TablesCommunicationActionReceiver receiver =
        TablesCommunicationActionReceiver.getInstance(null, null, null, null,
            null, null);
    String broadcastChannel;
    String syncRequestId;
    List<String> submitFileUploadUids;
    try {
      broadcastChannel = this.registerApp();
    } catch (Exception e) {
      Log.e(TAG, "trouble registering app");
      return;
    }
    try {
      syncRequestId = this.registerData();
      submitFileUploadUids = new ArrayList<String>();
      for (String tableId : receiver.getTableIdsPendingForSubmit()) {
        Map<String, String> absolutePathToUploadUrl =
            getFileInfoForSubmit(tableId, receiver.getAggregateServerUri());
        Log.e(TAG, "[onServiceConnected] giving file to submit: " + absolutePathToUploadUrl);
        for (Map.Entry<String, String> entry :
            absolutePathToUploadUrl.entrySet()) {
          String absolutePath = entry.getKey();
          String uploadUrl = entry.getValue();
          submitFileUploadUids.add(giveFileToSubmit(absolutePath, uploadUrl));
        }
      }
    } catch (Exception e) {
      Log.e(TAG, "trouble registering data");
      return;
    }
    receiver.setSubmitId(broadcastChannel);
    receiver.listenForSyncRequestUid(syncRequestId);
    receiver.listenForFileUids(submitFileUploadUids);
  }

  @Override
  public void onServiceDisconnected(ComponentName name) {
    Log.d(TAG, "[onServiceDisconnected] unbound to submit service");
    this.mIsBoundToService = false;
  }

}
