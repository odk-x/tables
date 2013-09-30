package org.opendatakit.tables.submit;

import org.opendatakit.submit.data.DataObject;
import org.opendatakit.submit.flags.DataSize;
import org.opendatakit.submit.service.ClientRemote;

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
    
    // Now bind to the service.
    Intent bindIntent = new Intent();
    bindIntent.setClassName(SUBMIT_PACKAGE_NAME, SUBMIT_SERVICE_CLASS_NAME);
    this.mComponentContext.bindService(bindIntent, this, 
        Context.BIND_AUTO_CREATE);
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
      throw new Exception("Not bound to service but tryin to register data!");
    }
    DataObject data = new DataObject();
    // I'm not sure why we're doing SMALL. Presumably because we're just 
    // waiting for an ok-to-sync message and nothing else? Following Waylon's
    // example in the test project.
    data.setDataSize(DataSize.SMALL); 
    return this.mSubmitService.register(this.mAppUuid, data);
  }

  @Override
  public void onServiceConnected(ComponentName name, IBinder service) {
    Log.d(TAG, "[onServiceConnected] bound to submit service");
    this.mSubmitService = ClientRemote.Stub.asInterface(service);
    this.mIsBoundToService = true;
    // This call relies on the CAR being set up BEFORE the call to the 
    // service.
    TablesCommunicationActionReceiver receiver = 
        TablesCommunicationActionReceiver.getInstance(null, null, null, null, null);
    String broadcastChannel;
    String dataId;
    try {
      broadcastChannel = this.registerApp();
    } catch (Exception e) {
      Log.e(TAG, "trouble registering app");
      return;
    }
    try {
      dataId = this.registerData();
    } catch (Exception e) {
      Log.e(TAG, "trouble registering data");
      return;
    }
    receiver.setSubmitId(broadcastChannel);
    receiver.listenForUid(dataId);
    
  }

  @Override
  public void onServiceDisconnected(ComponentName name) {
    Log.d(TAG, "[onServiceDisconnected] unbound to submit service");
    this.mIsBoundToService = false;
  }

}
