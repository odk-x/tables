package org.opendatakit.tables.fragments;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceScreen;
import android.widget.Toast;
import org.opendatakit.data.TableViewType;
import org.opendatakit.data.utilities.TableUtil;
import org.opendatakit.database.LocalKeyValueStoreConstants;
import org.opendatakit.database.service.DbHandle;
import org.opendatakit.database.service.UserDbInterface;
import org.opendatakit.exception.ServicesAvailabilityException;
import org.opendatakit.listener.DatabaseConnectionListener;
import org.opendatakit.logging.WebLogger;
import org.opendatakit.properties.CommonToolProperties;
import org.opendatakit.properties.PropertiesSingleton;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.TablePropertiesActivity;
import org.opendatakit.tables.application.Tables;
import org.opendatakit.utilities.LocalizationUtils;
import org.opendatakit.utilities.ODKFileUtils;

import java.io.File;
import java.io.IOException;

public class TablePropertiesFragment extends PreferenceFragmentCompat implements
    DatabaseConnectionListener {

   public static final String TAG_NAME = TablePropertiesFragment.class.getSimpleName();

   private String appName;
   private String tableId;
   private TablePropertiesActivity tablePropActivity;
   private PropertiesSingleton props;

   @Override public void onActivityCreated (Bundle savedInstanceState){
      super.onActivityCreated(savedInstanceState);
      Activity activity = getActivity();
      if(activity instanceof TablePropertiesActivity) {
         tablePropActivity = (TablePropertiesActivity) activity;
         appName = tablePropActivity.getAppName();
         tableId = tablePropActivity.getTableId();
         props = tablePropActivity.getPropertisSingleton();
      } else {
         throw new IllegalStateException("Not expecting any other activity besides "
             + "TablePropertiesActivity to use the TablePropertiesFragment");
      }

   }

   @Override public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

   }



   private void createFromDatabase() {
      if(props == null || tablePropActivity == null) {
         throw new IllegalStateException("props or activity has not been created yet");
      }
      String userSelectedDefaultLocale = props.getUserSelectedDefaultLocale();
      UserDbInterface dbInterface = Tables.getInstance().getDatabase();
      String localizedDisplayName;
      DbHandle db = null;
      try {
         db = dbInterface.openDatabase(appName);
         localizedDisplayName = TableUtil.get()
             .getLocalizedDisplayName(userSelectedDefaultLocale, dbInterface, appName, db, tableId);
      } catch (ServicesAvailabilityException e) {
         WebLogger.getLogger(appName).printStackTrace(e);
         Toast.makeText(tablePropActivity, "Unable to access database", Toast.LENGTH_LONG).show();
         // TODO this is deprecated
         PreferenceScreen root = getPreferenceManager().createPreferenceScreen(tablePropActivity);
         setPreferenceScreen(root);
         return;
      } finally {
         if (db != null) {
            try {
               dbInterface.closeDatabase(appName, db);
            } catch (ServicesAvailabilityException e) {
               WebLogger.getLogger(appName).printStackTrace(e);
               Toast.makeText(tablePropActivity, "Unable to close database", Toast.LENGTH_LONG).show();
            }
         }
      }

      tablePropActivity.setTitle(getString(R.string.table_manager_title, localizedDisplayName));
      init();
   }

   private void init() {

      // TODO this is deprecated
      PreferenceScreen root = getPreferenceManager().createPreferenceScreen(tablePropActivity);

      // general category

      PreferenceCategory genCat = new PreferenceCategory(tablePropActivity);
      root.addPreference(genCat);
      genCat.setTitle(getString(R.string.general_settings));

      UserDbInterface dbInterface = Tables.getInstance().getDatabase();

      String rawDisplayName;
      DbHandle db = null;
      try {
         db = dbInterface.openDatabase(appName);
         rawDisplayName = TableUtil.get().getRawDisplayName(dbInterface, appName, db, tableId);
      } catch (ServicesAvailabilityException e) {
         WebLogger.getLogger(appName).printStackTrace(e);
         Toast.makeText(tablePropActivity, "Unable to access database", Toast.LENGTH_LONG).show();
         // TODO this is deprecated
         setPreferenceScreen(root);
         return;
      } finally {
         if (db != null) {
            try {
               dbInterface.closeDatabase(appName, db);
            } catch (ServicesAvailabilityException e) {
               WebLogger.getLogger(appName).printStackTrace(e);
               Toast.makeText(tablePropActivity, "Unable to close database", Toast.LENGTH_LONG).show();
            }
         }
      }

      EditTextPreference dnPref = new EditTextPreference(tablePropActivity);
      dnPref.setTitle(getString(R.string.table_display_name));
      dnPref.setDialogTitle(getString(R.string.change_table_display_name));
      dnPref.setText(rawDisplayName);
      dnPref.setSummary(rawDisplayName);
      dnPref.setOnPreferenceChangeListener(new TableDisplayNameChangeListener());
      genCat.addPreference(dnPref);

      // display category

      {
         PreferenceCategory displayCat = new PreferenceCategory(tablePropActivity);
         root.addPreference(displayCat);
         displayCat.setTitle(getString(R.string.display_settings));
         addViewPreferences(displayCat);
      }
      PreferenceCategory displayListViewCat = new PreferenceCategory(tablePropActivity);
      root.addPreference(displayListViewCat);
      displayListViewCat.setTitle(getString(R.string.display_list_view_settings));
      addListViewPreferences(displayListViewCat);

      PreferenceCategory displayMapViewCat = new PreferenceCategory(tablePropActivity);
      root.addPreference(displayMapViewCat);
      displayMapViewCat.setTitle(getString(R.string.display_map_view_settings));
      //addMapViewPreferences();

      // TODO this is deprecated
      setPreferenceScreen(root);
   }

   public boolean processOnActivityResult(int requestCode, int resultCode, Intent data) {
      UserDbInterface dbInterface = Tables.getInstance().getDatabase();
      Uri uri;
      String filename;
      String relativePath;
      switch (requestCode) {
      case TablePropertiesActivity.RC_DETAIL_VIEW_FILE:
         uri = data.getData();
         filename = uri.getPath();
         // We need to get the relative path under the app name.
         relativePath = getRelativePathOfFile(filename);
         try {
            TableUtil.get().atomicSetDetailViewFilename(dbInterface, appName, tableId, relativePath);
         } catch (ServicesAvailabilityException e) {
            WebLogger.getLogger(appName).printStackTrace(e);
            Toast.makeText(tablePropActivity, "Unable to set Detail View Filename", Toast.LENGTH_LONG)
                .show();
         }
         init();
         return true;
      case TablePropertiesActivity.RC_LIST_VIEW_FILE:
         uri = data.getData();
         filename = uri.getPath();
         // We need to get the relative path under the app name.
         relativePath = getRelativePathOfFile(filename);
         try {
            TableUtil.get().atomicSetListViewFilename(dbInterface, appName, tableId, relativePath);
         } catch (ServicesAvailabilityException e) {
            WebLogger.getLogger(appName).printStackTrace(e);
            Toast.makeText(tablePropActivity, "Unable to set List View Filename", Toast.LENGTH_LONG).show();
         }
         init();
         return true;
      case TablePropertiesActivity.RC_MAP_LIST_VIEW_FILE:
         uri = data.getData();
         filename = uri.getPath();
         // We need to get the relative path under the app name.
         relativePath = getRelativePathOfFile(filename);
         try {
            TableUtil.get().atomicSetMapListViewFilename(dbInterface, appName, tableId, relativePath);
         } catch (ServicesAvailabilityException e) {
            WebLogger.getLogger(appName).printStackTrace(e);
            Toast.makeText(tablePropActivity, "Unable to set Map List View Filename", Toast.LENGTH_LONG)
                .show();
         }
         init();
         return true;
      default:
         return false;
      }
   }

   private void addViewPreferences(PreferenceCategory prefCat) {

      ListPreference viewTypePref = new ListPreference(tablePropActivity);
      viewTypePref.setTitle(getString(R.string.default_view_type));
      viewTypePref.setDialogTitle(getString(R.string.change_default_view_type));

      UserDbInterface dbInterface = Tables.getInstance().getDatabase();

      TableViewType type;
      DbHandle db = null;
      try {
         db = dbInterface.openDatabase(appName);
         type = TableUtil.get().getDefaultViewType(dbInterface, appName, db, tableId);

         viewTypePref.setValue(type.name());
         // TODO: currently throwing an error i think
         // viewTypePref.setSummary(LanguageUtil.getViewTypeLabel(
         // settings.getViewType()));
         viewTypePref.setSummary(type.name());
         viewTypePref.setOnPreferenceChangeListener(new DefaultViewTypeChangeListener());
         prefCat.addPreference(viewTypePref);

         Preference rowColorRulePrefs = new Preference(tablePropActivity);
         rowColorRulePrefs.setTitle(getString(R.string.edit_table_color_rules));

         TableUtil.MapViewColorRuleInfo info = TableUtil.get()
             .getMapListViewColorRuleInfo(dbInterface, appName, db, tableId);

         ListPreference colorRulePref = new ListPreference(tablePropActivity);
         colorRulePref.setTitle("Color Rule for Graph and Map Markers");
         colorRulePref.setDialogTitle("Change which color rule markers adhere to.");
         String[] colorRuleTypes = { LocalKeyValueStoreConstants.Map.COLOR_TYPE_NONE,
             LocalKeyValueStoreConstants.Map.COLOR_TYPE_TABLE,
             LocalKeyValueStoreConstants.Map.COLOR_TYPE_STATUS };
         colorRulePref.setEntryValues(colorRuleTypes);
         colorRulePref.setEntries(colorRuleTypes);
         colorRulePref.setValue(info.colorType);
         colorRulePref.setSummary(info.colorType);
         prefCat.addPreference(colorRulePref);
      } catch (ServicesAvailabilityException e) {
         WebLogger.getLogger(appName).printStackTrace(e);
         Toast.makeText(tablePropActivity, "Unable to access database", Toast.LENGTH_LONG).show();
      } finally {
         if (db != null) {
            try {
               dbInterface.closeDatabase(appName, db);
            } catch (ServicesAvailabilityException e) {
               WebLogger.getLogger(appName).printStackTrace(e);
               Toast.makeText(tablePropActivity, "Unable to close database", Toast.LENGTH_LONG).show();
            }
         }
      }
   }

   private void addListViewPreferences(PreferenceCategory prefCat) {
      Preference listViewPrefs = new Preference(tablePropActivity);
      listViewPrefs.setTitle(getString(R.string.list_view_manager));
      listViewPrefs.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

         @Override
         public boolean onPreferenceClick(Preference preference) {
            return true;
         }

      });
      prefCat.addPreference(listViewPrefs);

      TablePropertiesFragment.FileSelectorPreference detailViewPref = new TablePropertiesFragment
          .FileSelectorPreference(tablePropActivity, TablePropertiesActivity.RC_DETAIL_VIEW_FILE);
      detailViewPref.setTitle(getString(R.string.detail_view_file));
      detailViewPref.setDialogTitle(getString(R.string.change_detail_view_file));
      detailViewPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
         @Override
         public boolean onPreferenceChange(Preference preference, Object newValue) {
            init();
            return false;
         }
      });
      prefCat.addPreference(detailViewPref);

   }

   /**
    * Get the relative filepath under the app directory for the full path as
    * returned from OI file picker.
    *
    * @param fullPath the full path to the file
    * @return the relative path from the odk app directory
    */
   private String getRelativePathOfFile(String fullPath) {
      return ODKFileUtils.asRelativePath(appName, new File(fullPath));
   }


   @Override
   public void databaseAvailable() {
      createFromDatabase();
   }


   @Override
   public void databaseUnavailable() {
      // TODO Auto-generated method stub

   }

   private final class TableDisplayNameChangeListener implements
       Preference.OnPreferenceChangeListener {
      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
         String localizedDisplayName;
         PropertiesSingleton props = CommonToolProperties.get(tablePropActivity
             .getApplicationContext(), appName);
         try {
            localizedDisplayName = LocalizationUtils
                .getLocalizedDisplayName(appName, tableId, props.getUserSelectedDefaultLocale(),
                    TableUtil.get()
                        .atomicSetRawDisplayName(Tables.getInstance().getDatabase(), appName,
                            tableId, (String) newValue));
         } catch (ServicesAvailabilityException e) {
            WebLogger.getLogger(appName).printStackTrace(e);
            Toast.makeText(tablePropActivity, "Unable to change display name", Toast.LENGTH_LONG).show();
            init();
            return false;
         }
         tablePropActivity.setTitle(getString(R.string.table_manager_title, localizedDisplayName));
         init();
         return false;
      }
   }

   private final class DefaultViewTypeChangeListener implements
       Preference.OnPreferenceChangeListener {
      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
         try {
            TableUtil.get()
                .atomicSetDefaultViewType(Tables.getInstance().getDatabase(), appName, tableId,
                    TableViewType.valueOf((String) newValue));
         } catch (ServicesAvailabilityException e) {
            WebLogger.getLogger(appName).printStackTrace(e);
            Toast.makeText(tablePropActivity, "Unable to change default view type", Toast.LENGTH_LONG).show();
         }
         init();
         return false;
      }
   }

   /**
    * This preference allows the user to select a file from their SD card. If the
    * user does not have a file picker installed on their phone, then a toast
    * will indicate so.
    *
    * @author Chris Gelon (cgelon)
    */
   private class FileSelectorPreference extends EditTextPreference {
      /**
       * Indicates which preference we are using the selector for.
       */
      private int mRequestCode;

      FileSelectorPreference(Context context, int requestCode) {
         super(context);
         mRequestCode = requestCode;
      }

      @Override
      protected void onClick() {
         if (tablePropActivity.hasFilePicker()) {
            Intent intent = new Intent("org.openintents.action.PICK_FILE");
            if (getText() != null) {
               File fullFile = ODKFileUtils.asAppFile(appName, getText());
               try {
                  intent.setData(Uri.parse("file://" + fullFile.getCanonicalPath()));
               } catch (IOException e) {
                  // TODO Auto-generated catch block
                  WebLogger.getLogger(appName).printStackTrace(e);
                  Toast.makeText(tablePropActivity,
                      getString(R.string.file_not_found, fullFile.getAbsolutePath()), Toast.LENGTH_LONG)
                      .show();
               }
            }
            try {
               startActivityForResult(intent, mRequestCode);
            } catch (ActivityNotFoundException e) {
               WebLogger.getLogger(appName).printStackTrace(e);
               Toast.makeText(tablePropActivity, getString(R.string.file_picker_not_found),
                   Toast.LENGTH_LONG).show();
            }
         } else {
            super.onClick();
            Toast.makeText(tablePropActivity, getString(R.string.file_picker_not_found),
                Toast.LENGTH_LONG).show();
         }
      }


   }
}
