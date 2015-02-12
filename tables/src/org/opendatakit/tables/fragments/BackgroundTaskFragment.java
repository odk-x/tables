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

import org.opendatakit.common.android.listener.LicenseReaderListener;
import org.opendatakit.tables.R;
import org.opendatakit.tables.tasks.LicenseReaderTask;

import android.app.Activity;
import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

/**
 * Wrapper that holds all the background tasks that might be in-progress at any
 * time.
 *
 * @author mitchellsundt@gmail.com
 *
 */
public class BackgroundTaskFragment extends Fragment implements LicenseReaderListener {

  public static final class BackgroundTasks {
    LicenseReaderTask mLicenseReaderTask = null;

    BackgroundTasks() {
    };
  }

  public BackgroundTasks mBackgroundTasks; // handed across orientation
  // changes

  public LicenseReaderListener mLicenseReaderListener = null;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mBackgroundTasks = new BackgroundTasks();

    setRetainInstance(true);
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    setRetainInstance(true);
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return new View(getActivity());
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
  }

  private <T> void executeTask(AsyncTask<T, ?, ?> task, T... args) {

    int androidVersion = android.os.Build.VERSION.SDK_INT;
    if (androidVersion < 11) {
      task.execute(args);
    } else {
      // TODO: execute on serial executor in version 11 onward...
      task.execute(args);
      // task.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, (Void[]) null);
    }

  }

  @Override
  public void onStart() {
    super.onStart();
    // run the disk sync task once...
    // startDiskSyncListener(((ODKActivity) getActivity()).getAppName(), null);
  }

  @Override
  public void onPause() {
    mLicenseReaderListener = null;

    if (mBackgroundTasks.mLicenseReaderTask != null) {
      mBackgroundTasks.mLicenseReaderTask.setLicenseReaderListener(null);
    }
    super.onPause();
  }

  @Override
  public void onResume() {
    super.onResume();
    if (mBackgroundTasks.mLicenseReaderTask != null) {
      mBackgroundTasks.mLicenseReaderTask.setLicenseReaderListener(this);
    }
  }

  // /////////////////////////////////////////////////////////////////////////
  // registrations

  public void establishReadLicenseListener(LicenseReaderListener listener) {
    mLicenseReaderListener = listener;
    // async task may have completed while we were reorienting...
    if (mBackgroundTasks.mLicenseReaderTask != null
        && mBackgroundTasks.mLicenseReaderTask.getStatus() == AsyncTask.Status.FINISHED) {
      this.readLicenseComplete(mBackgroundTasks.mLicenseReaderTask.getResult());
    }
  }

  // ///////////////////////////////////////////////////
  // actions

  public void readLicenseFile(String appName, LicenseReaderListener listener) {
    mLicenseReaderListener = listener;
    if (mBackgroundTasks.mLicenseReaderTask != null
        && mBackgroundTasks.mLicenseReaderTask.getStatus() != AsyncTask.Status.FINISHED) {
      Toast.makeText(this.getActivity(), getString(R.string.still_reading_license_file), Toast.LENGTH_LONG).show();
    } else {
      LicenseReaderTask lrt = new LicenseReaderTask();
      lrt.setApplication(getActivity().getApplication());
      lrt.setAppName(appName);
      lrt.setLicenseReaderListener(this);
      mBackgroundTasks.mLicenseReaderTask = lrt;
      executeTask(mBackgroundTasks.mLicenseReaderTask);
    }
  }


  // /////////////////////////////////////////////////////////////////////////
  // clearing tasks
  //
  // NOTE: clearing these makes us forget that they are running, but it is
  // up to the task itself to eventually shutdown. i.e., we don't quite
  // know when they actually stop.


  // /////////////////////////////////////////////////////////////////////////
  // cancel requests
  //
  // These maintain communications paths, so that we get a failure
  // completion callback eventually.


  // /////////////////////////////////////////////////////////////////////////
  // callbacks

  @Override
  public void readLicenseComplete(String result) {
    if (mLicenseReaderListener != null) {
      mLicenseReaderListener.readLicenseComplete(result);
    }
    mBackgroundTasks.mLicenseReaderTask = null;
  }

}
