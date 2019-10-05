/*
 * Copyright (C) 2012-2013 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.opendatakit.tables.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import org.opendatakit.activities.IAppAwareActivity;
import org.opendatakit.activities.IInitResumeActivity;
import org.opendatakit.fragment.AlertDialogFragment.ConfirmAlertDialog;
import org.opendatakit.fragment.AlertNProgessMsgFragmentMger;
import org.opendatakit.listener.DatabaseConnectionListener;
import org.opendatakit.listener.InitializationListener;
import org.opendatakit.logging.WebLogger;
import org.opendatakit.tables.R;

import java.util.ArrayList;

/**
 * Attempt to initialize data directories using the APK Expansion files.
 *
 * @author mitchellsundt@gmail.com
 */
public class InitializationFragment extends AbsTablesFragment
    implements ConfirmAlertDialog, DatabaseConnectionListener, InitializationListener {

   // used for logging
   private static final String TAG = InitializationFragment.class.getSimpleName();
   // The layout id, used for the view inflater
   private static final int ID = R.layout.copy_expansion_files_layout;

   private static final String INIT_TABLES_DIALOG_TAG = "progressTablesInitFrag";
   private static final String ALERT_DIALOG_TAG = "alertTablesInitFrag";

   private static final String INIT_STATE_KEY = "IF_initStateKey";

   // The types of dialogs we handle
   public enum InitializationState {
      START, IN_PROGRESS, FINISH
   }

   private static String appName;

   private InitializationState initState = InitializationState.START;
   private AlertNProgessMsgFragmentMger msgManager;

   private String mainDialogTitle;

   /**
    * Called when we're recreated
    *
    * @param inflater           an inflater used to create the view
    * @param container          also used to create the view
    * @param savedInstanceState a bundle that we use to pull the dialog title, message and state from
    * @return an inflated view
    */
   @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
       Bundle savedInstanceState) {
      WebLogger.getLogger(appName).d(TAG,
          "in public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle "
              + "savedInstanceState) {");
      appName = ((IAppAwareActivity) getActivity()).getAppName();
      View view = inflater.inflate(ID, container, false);

      mainDialogTitle = getString(R.string.configuring_app,
          getString(getCommonApplication().getApkDisplayNameResourceId()));

      // restore any state
      if (savedInstanceState != null) {
         if (savedInstanceState.containsKey(INIT_STATE_KEY)) {
            initState = InitializationState.valueOf(savedInstanceState.getString(INIT_STATE_KEY));
         }
         msgManager = AlertNProgessMsgFragmentMger
             .restoreInitMessaging(appName, ALERT_DIALOG_TAG, INIT_TABLES_DIALOG_TAG,
                 savedInstanceState);
      }

      // if message manager was not created from saved state, create fresh
      if (msgManager == null) {
         msgManager = new AlertNProgessMsgFragmentMger(appName, ALERT_DIALOG_TAG,
             INIT_TABLES_DIALOG_TAG, false, false);
      }

      return view;
   }

   /**
    * Called on the first setup, just attaches to the tables instance to get task notifications
    */
   @Override public void onStart() {
      WebLogger.getLogger(appName).d(TAG, "in public void onStart() {");
      super.onStart();
      getCommonApplication().possiblyFireDatabaseCallback(getActivity(), this);
   }

   /**
    * Called when we resume from a pause, sets up the app name if it wasn't already set up and
    * restores the dialog if applicable, then attaches itself to the tables instance to receive
    * task notifications
    */
   @Override public void onResume() {
      WebLogger.getLogger(appName).d(TAG, "in public void onResume() {");
      super.onResume();

      if (initState == InitializationState.START) {
         WebLogger.getLogger(appName).i(TAG, "onResume -- calling initializeAppName");
         getCommonApplication()
             .initializeAppName(((IAppAwareActivity) getActivity()).getAppName(), this);
         initState = InitializationState.IN_PROGRESS;
      } else {

         msgManager.restoreDialog(getParentFragmentManager(), getId());

         // re-attach to the task for task notifications...
         getCommonApplication().establishInitializationListener(this);
      }
   }

   /**
    * Called when we're going to be paused, removes the dialogs and sets the state to none
    */
   @Override public void onPause() {
      WebLogger.getLogger(appName).d(TAG, "in public void onPause() {");
      msgManager.clearDialogsAndRetainCurrentState(getParentFragmentManager());
      super.onPause();
   }

   /**
    * Saves the alert title, message and state to the output bundle
    *
    * @param outState the bundle to be saved across lifetimes
    */
   @Override public void onSaveInstanceState(Bundle outState) {
      WebLogger.getLogger(appName).d(TAG, "in public void onSaveInstanceState(Bundle outState) {");
      super.onSaveInstanceState(outState);

      if (msgManager != null) {
         msgManager.addStateToSaveStateBundle(outState);
      }
      outState.putString(INIT_STATE_KEY, initState.name());
   }

   /**
    * Called when we've successfully extracted all the files and are ready to open tables. Closes
    * any open progress dialogs and creates an alert dialog showing the results to the user
    *
    * @param overallSuccess whether initializing tables worked
    * @param result         a list of the tables we were able to create, forms we were able to
    *                       extract, etc..
    */
   @Override public void initializationComplete(boolean overallSuccess, ArrayList<String> result) {
      WebLogger.getLogger(appName).d(TAG,
          "in public void initializationComplete(boolean overallSuccess, ArrayList<String> result) {");

      initState = InitializationState.FINISH;
      getCommonApplication().clearInitializationTask();

      if (overallSuccess && result.isEmpty()) {
         // do not require an OK if everything went well
         if (msgManager != null) {
            msgManager.dismissProgressDialog(getParentFragmentManager());
         }

         ((IInitResumeActivity) getActivity()).initializationCompleted();
         return;
      }

      StringBuilder b = new StringBuilder();
      for (String k : result) {
         b.append(k);
         b.append("\n\n");
      }

      if (msgManager != null) {
         String revisedTitle = overallSuccess ?
             getString(R.string.initialization_complete) :
             getString(R.string.initialization_failed);
         msgManager
             .createAlertDialog(revisedTitle, b.toString().trim(), getParentFragmentManager(), getId());
      }
   }

   /**
    * Called when the Tables object we attached to sends us a new event to display
    *
    * @param displayString the string to set the ProgressDialog's text to
    */
   @Override public void initializationProgressUpdate(String displayString) {
      WebLogger.getLogger(appName)
          .d(TAG, "in public void initializationProgressUpdate(String displayString) {");
      if (msgManager != null
          && initState == InitializationState.IN_PROGRESS) {
         updateProgressDialog(displayString);
      }
   }

   /**
    * Called when the user clicks the ok button on an alert dialog
    */
   @Override public void okAlertDialog() {
      WebLogger.getLogger(appName).d(TAG, "in public void okAlertDialog() {");
      ((IInitResumeActivity) getActivity()).initializationCompleted();
   }

   /**
    * Called when the database comes up. If we're in a "loading..." mode, tell Tables to initialize
    * the app name
    */
   @Override public void databaseAvailable() {
      WebLogger.getLogger(appName).d(TAG, "in public void databaseAvailable() {");
      if (initState == InitializationState.IN_PROGRESS) {
         getCommonApplication().initializeAppName(appName, this);
      }
   }

   /**
    * Called when the database goes away, if we're in a "loading..." mode, then update the
    * progress dialog with an error message
    */
   @Override public void databaseUnavailable() {
      WebLogger.getLogger(appName).d(TAG, "in public void databaseUnavailable() {");
      if (msgManager != null) {
         updateProgressDialog(getString(R.string.database_unavailable));
      }
   }

   private void updateProgressDialog(String displayString) {
      if (!msgManager.displayingProgressDialog()) {
         msgManager.createProgressDialog(mainDialogTitle, getString(R.string.please_wait),
                 getParentFragmentManager());
      } else {
         if (msgManager.hasDialogBeenCreated()) {
            msgManager.updateProgressDialogMessage(displayString,getParentFragmentManager());
         }
      }
   }
}