package org.opendatakit.tables.activities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opendatakit.aggregate.odktables.rest.ConflictType;
import org.opendatakit.common.android.data.ConflictTable;
import org.opendatakit.common.android.data.DbTable;
import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.common.android.data.UserTable;
import org.opendatakit.common.android.data.UserTable.Row;
import org.opendatakit.common.android.provider.DataTableColumns;
import org.opendatakit.tables.R;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.TableFileUtils;
import org.opendatakit.tables.views.components.ConcordantColumn;
import org.opendatakit.tables.views.components.ConflictColumn;
import org.opendatakit.tables.views.components.ConflictResolutionListAdapter;
import org.opendatakit.tables.views.components.ConflictResolutionListAdapter.Resolution;
import org.opendatakit.tables.views.components.ConflictResolutionListAdapter.Section;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Activity for resolving the conflicts in a row. This is the native version,
 * which presents a UI and does not support HTML or js rules.
 * @author sudar.sam@gmail.com
 *
 */
public class ConflictResolutionRowActivity extends ListActivity
    implements ConflictResolutionListAdapter.UICallbacks {

  private static final String TAG =
      ConflictResolutionRowActivity.class.getSimpleName();

  public static final String INTENT_KEY_ROW_ID = "rowId";

  private static final String BUNDLE_KEY_SHOWING_LOCAL_DIALOG =
      "showingLocalDialog";
  private static final String BUNDLE_KEY_SHOWING_SERVER_DIALOG =
      "showingServerDialog";
  private static final String BUNDLE_KEY_SHOWING_RESOLVE_DIALOG =
      "showingResolveDialog";
  private static final String BUNDLE_KEY_VALUE_KEYS = "valueValueKeys";
  private static final String BUNDLE_KEY_CHOSEN_VALUES = "chosenValues";
  private static final String BUNDLE_KEY_RESOLUTION_KEYS = "resolutionKeys";
  private static final String BUNDLE_KEY_RESOLUTION_VALUES =
      "resolutionValues";

  private ConflictTable mConflictTable;
  private ConflictResolutionListAdapter mAdapter;
  /** The row number of the row in conflict within the {@link ConflictTable}.*/
  private int mRowNumber;
  private String mRowId;
  private String mServerRowETag;
  private UserTable mLocal;
  private UserTable mServer;

  private Button mButtonTakeLocal;
  private Button mButtonTakeServer;
  private Button mButtonResolveRow;
  private List<ConflictColumn> mConflictColumns;

  /**
   * The message to the user as to why they're getting extra options. Will be
   * either thing to the effect of "someone has deleted something you've
   * changed", or "you've deleted something someone has changed". They'll then
   * have to choose either to delete or to go ahead and actually restore and
   * then resolve it.
   */
  private TextView mTextViewConflictMessage;

  private boolean mIsShowingTakeLocalDialog;
  private boolean mIsShowingTakeServerDialog;
  private boolean mIsShowingResolveDialog;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    String appName = getIntent().getStringExtra(Constants.IntentKeys.APP_NAME);
    if ( appName == null ) {
      appName = TableFileUtils.getDefaultAppName();
    }
    this.setContentView(R.layout.conflict_resolution_row_activity);
    this.mTextViewConflictMessage = (TextView)
        findViewById(R.id.conflict_overview_message);

    this.mButtonTakeLocal =
        (Button) findViewById(R.id.conflict_take_local);
    this.mButtonTakeServer =
        (Button) findViewById(R.id.conflict_take_server);

    this.mButtonResolveRow =
        (Button) findViewById(R.id.conflict_resolution_button_resolve_row);
    this.mButtonResolveRow.setOnClickListener(new ResolveRowClickListener());

    String tableId =
        getIntent().getStringExtra(Constants.IntentKeys.TABLE_ID);
    this.mRowId = getIntent().getStringExtra(INTENT_KEY_ROW_ID);
    TableProperties tableProperties =
        TableProperties.getTablePropertiesForTable(this, appName, tableId);
    DbTable dbTable = DbTable.getDbTable(tableProperties);
    this.mConflictTable = dbTable.getConflictTable();
    this.mLocal = mConflictTable.getLocalTable();
    this.mServer = mConflictTable.getServerTable();
    //
    // And now we need to construct up the adapter.
    // There are several things to do be aware of. We need to get all the
    // section headings, which will be the column names. We also need to get
    // all the values which are in conflict, as well as those that are not.
    // We'll present them in user-defined order, as they may have set up the
    // useful information together.
    this.mRowNumber = this.mLocal.getRowNumFromId(mRowId);
    Row localRow = this.mLocal.getRowAtIndex(mRowNumber);
    Row serverRow = this.mServer.getRowAtIndex(mRowNumber);
    this.mServerRowETag = serverRow.getDataOrMetadataByElementKey(DataTableColumns.ROW_ETAG);
    TableProperties tp = mConflictTable.getLocalTable().getTableProperties();
    List<String> columnOrder = tp.getPersistedColumns();
    // This will be the number of rows down we are in the adapter. Each
    // heading and each cell value gets its own row. Columns in conflict get
    // two, as we'll need to display each one to the user.
    int adapterOffset = 0;
    List<Section> sections = new ArrayList<Section>();
    this.mConflictColumns = new ArrayList<ConflictColumn>();
    List<ConcordantColumn> noConflictColumns =
        new ArrayList<ConcordantColumn>();
    for (int i = 0; i < columnOrder.size(); i++) {
      String elementKey = columnOrder.get(i);
      String columnDisplayName =
          tp.getColumnByElementKey(elementKey).getLocalizedDisplayName();
      Section newSection = new Section(adapterOffset, columnDisplayName);
      ++adapterOffset;
      sections.add(newSection);
      String localRawValue = localRow.getDataOrMetadataByElementKey(elementKey);
      String localDisplayValue = localRow.getDisplayTextOfData(this, elementKey, true);
      String serverRawValue = serverRow.getDataOrMetadataByElementKey(elementKey);
      String serverDisplayValue = serverRow.getDisplayTextOfData(this, elementKey, true);
      if ((localRawValue == null && serverRawValue == null) ||
    	  (localRawValue != null && localRawValue.equals(serverRawValue))) {
        // TODO: this doesn't compare actual equality of blobs if their display
        // text is the same.
        // We only want to display a single row, b/c there are no choices to
        // be made by the user.
        ConcordantColumn concordance = new ConcordantColumn(adapterOffset,
            localDisplayValue);
        noConflictColumns.add(concordance);
        ++adapterOffset;
      } else {
        // We need to display both the server and local versions.
        ConflictColumn conflictColumn = new ConflictColumn(adapterOffset,
            elementKey, localRawValue, localDisplayValue, serverRawValue, serverDisplayValue);
        ++adapterOffset;
        mConflictColumns.add(conflictColumn);
      }
    }
    // Now that we have the appropriate lists, we need to construct the
    // adapter that will display the information.
    this.mAdapter = new ConflictResolutionListAdapter(
        this.getActionBar().getThemedContext(), this, sections,
        noConflictColumns, mConflictColumns);
    this.setListAdapter(mAdapter);
    this.onDecisionMade();
    // Here we'll handle the cases of whether or not rows were deleted. There
    // are three cases to consider:
    // 1) both rows were updated, neither is deleted. This is the normal case
    // 2) the server row was deleted, the local was updated (thus a conflict)
    // 3) the local was deleted, the server was updated (thus a conflict)
    // To Figure this out we'll first need the state of each version.
    // Note that these calls should never return nulls, as whenever a row is in
    // conflict, there should be a conflict type. Therefore if we throw an
    // error that is fine, as we've violated an invariant.


    int localConflictType = Integer.parseInt(mLocal.getRowAtIndex(mRowNumber)
        .getDataOrMetadataByElementKey(DataTableColumns.CONFLICT_TYPE));
    int serverConflictType =
        Integer.parseInt(mServer.getRowAtIndex(mRowNumber)
            .getDataOrMetadataByElementKey(DataTableColumns.CONFLICT_TYPE));
    if (localConflictType ==
          ConflictType.LOCAL_UPDATED_UPDATED_VALUES &&
        serverConflictType ==
          ConflictType.SERVER_UPDATED_UPDATED_VALUES) {
      // Then it's a normal conflict. Hide the elements of the view relevant
      // to deletion restoration.
      mTextViewConflictMessage.setText(getString(R.string.conflict_resolve_or_choose));

      this.mButtonTakeLocal.setOnClickListener(new TakeLocalClickListener());
      this.mButtonTakeLocal.setText(getString(R.string.conflict_take_local_updates));
      this.mButtonTakeServer.setOnClickListener(new TakeServerClickListener());
      this.mButtonTakeServer.setText(getString(R.string.conflict_take_server_updates));
      this.mButtonResolveRow.setVisibility(View.VISIBLE);
      this.onDecisionMade();
    } else if (localConflictType ==
          ConflictType.LOCAL_DELETED_OLD_VALUES &&
        serverConflictType ==
          ConflictType.SERVER_UPDATED_UPDATED_VALUES) {
      // Then the local row was deleted, but someone had inserted a newer
      // updated version on the server.
      this.mTextViewConflictMessage.setText(
          getString(R.string.conflict_local_was_deleted_explanation));
      this.mButtonTakeServer.setOnClickListener(
          new TakeServerClickListener());
      this.mButtonTakeServer.setText(
          getString(R.string.conflict_restore_with_server_changes));
      this.mButtonTakeLocal.setOnClickListener(
          new SetRowToDeleteOnServerListener());
      this.mButtonTakeLocal.setText(
          getString(R.string.conflict_enforce_local_delete));

      mButtonResolveRow.setEnabled(false);
      mButtonResolveRow.setVisibility(View.GONE);
      mAdapter.setConflictColumnsEnabled(false);
      mAdapter.notifyDataSetChanged();
    } else if (localConflictType ==
          ConflictType.LOCAL_UPDATED_UPDATED_VALUES &&
        serverConflictType ==
          ConflictType.SERVER_DELETED_OLD_VALUES) {
      // Then the row was updated locally but someone had deleted it on the
      // server.
      this.mTextViewConflictMessage.setText(
          getString(R.string.conflict_server_was_deleted_explanation));
      this.mButtonTakeLocal.setOnClickListener(
          new TakeLocalClickListener());
      this.mButtonTakeLocal.setText(
          getString(R.string.conflict_restore_with_local_changes));
      this.mButtonTakeServer.setText(
          getString(R.string.conflict_apply_delete_from_server));
      this.mButtonTakeServer.setOnClickListener(
          new DiscardChangesAndDeleteLocalListener());

      mButtonResolveRow.setEnabled(false);
      mButtonResolveRow.setVisibility(View.GONE);
      mAdapter.setConflictColumnsEnabled(false);
      mAdapter.notifyDataSetChanged();
    } else {
      // We should never get here, because it breaks an invariant.
      // We know the vers
      Log.e(TAG, "server and local versions of the row did not match a known" +
      		" pair of conflict types. local: " + localConflictType +
      		", sever: " + serverConflictType);
    }
  }

  /*
   * (non-Javadoc)
   * @see org.opendatakit.tables.views.components.ConflictResolutionListAdapter.UICallbacks#onDecisionMade(boolean)
   */
  @Override
  public void onDecisionMade() {
    // set the listview enabled in case it'd been down due to deletion
    // resolution.
    mAdapter.setConflictColumnsEnabled(true);
    mAdapter.notifyDataSetChanged();
    if (isResolvable()) {
      Log.e(TAG, "isResolvable returns true!");
      this.mButtonResolveRow.setEnabled(true);
    } else {
      this.mButtonResolveRow.setEnabled(false);
    }
  }

  /**
   * True if all the conflict columns have entries that have been chosen by the
   * user in the adapter.
   * @return
   */
  private boolean isResolvable() {
    // We'll check if a decision has been made on every conflict column. If it
    // has we'll return true, otherwise we won't.
    Map<String, String> currentlyResolvedValues = mAdapter.getResolvedValues();
    for (ConflictColumn cc : this.mConflictColumns) {
      if (!currentlyResolvedValues.containsKey(cc.getElementKey())) {
        this.mButtonResolveRow.setEnabled(false);
        return false;
      }
    }
    return true;
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putBoolean(BUNDLE_KEY_SHOWING_LOCAL_DIALOG,
        mIsShowingTakeLocalDialog);
    outState.putBoolean(BUNDLE_KEY_SHOWING_SERVER_DIALOG,
        mIsShowingTakeServerDialog);
    outState.putBoolean(BUNDLE_KEY_SHOWING_RESOLVE_DIALOG,
        mIsShowingResolveDialog);
    // We also need to get the chosen values and decisions and save them so
    // that we don't lose information if they rotate the screen.
    Map<String, String> chosenValuesMap = mAdapter.getResolvedValues();
    Map<String, Resolution> userResolutions = mAdapter.getResolutions();
    if (chosenValuesMap.size() != userResolutions.size()) {
      Log.e(TAG, "[onSaveInstanceState] chosen values and user resolutions" +
      		" are not the same size. This should be impossible, so not " +
      		"saving state.");
      return;
    }
    String[] valueKeys = new String[chosenValuesMap.size()];
    String[] chosenValues = new String[chosenValuesMap.size()];
    String[] resolutionKeys = new String[userResolutions.size()];
    String[] resolutionValues = new String[userResolutions.size()];
    int i = 0;
    for (Map.Entry<String, String> valueEntry : chosenValuesMap.entrySet()) {
      valueKeys[i] = valueEntry.getKey();
      chosenValues[i] = valueEntry.getValue();
      ++i;;
    }
    i = 0;
    for (Map.Entry<String, Resolution> resolutionEntry :
        userResolutions.entrySet()) {
      resolutionKeys[i] = resolutionEntry.getKey();
      resolutionValues[i] = resolutionEntry.getValue().name();
      ++i;
    }
    outState.putStringArray(BUNDLE_KEY_VALUE_KEYS, valueKeys);
    outState.putStringArray(BUNDLE_KEY_CHOSEN_VALUES, chosenValues);
    outState.putStringArray(BUNDLE_KEY_RESOLUTION_KEYS, resolutionKeys);
    outState.putStringArray(BUNDLE_KEY_RESOLUTION_VALUES, resolutionValues);
  }

  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);
    Log.e(TAG, "onRestoreInstanceState");
    if (savedInstanceState.containsKey(BUNDLE_KEY_SHOWING_LOCAL_DIALOG)) {
      boolean wasShowingLocal =
          savedInstanceState.getBoolean(BUNDLE_KEY_SHOWING_LOCAL_DIALOG);
      if (wasShowingLocal) this.mButtonTakeLocal.performClick();
    }
    if (savedInstanceState.containsKey(BUNDLE_KEY_SHOWING_SERVER_DIALOG)) {
      boolean wasShowingServer =
          savedInstanceState.getBoolean(BUNDLE_KEY_SHOWING_SERVER_DIALOG);
      if (wasShowingServer) this.mButtonTakeServer.performClick();
    }
    if (savedInstanceState.containsKey(BUNDLE_KEY_SHOWING_RESOLVE_DIALOG)) {
      boolean wasShowingResolve =
          savedInstanceState.getBoolean(BUNDLE_KEY_SHOWING_RESOLVE_DIALOG);
      if (wasShowingResolve) this.mButtonResolveRow.performClick();
    }
    String[] valueKeys =
        savedInstanceState.getStringArray(BUNDLE_KEY_VALUE_KEYS);
    String[] chosenValues =
        savedInstanceState.getStringArray(BUNDLE_KEY_CHOSEN_VALUES);
    String[] resolutionKeys =
        savedInstanceState.getStringArray(BUNDLE_KEY_RESOLUTION_KEYS);
    String[] resolutionValues =
        savedInstanceState.getStringArray(BUNDLE_KEY_RESOLUTION_VALUES);
    if (valueKeys != null) {
      // Then we know that we should have the chosenValues as well, or else
      // there is trouble. We're not doing a null check here, but if we didn't
      // get it then we know there is an error. We'll throw a null pointer
      // exception, but that is better than restoring bad state.
      // Same thing goes for the resolution keys. Those and the map should
      // always go together.
      Map<String, String> chosenValuesMap = new HashMap<String, String>();
      for (int i = 0; i < valueKeys.length; i++) {
        chosenValuesMap.put(valueKeys[i], chosenValues[i]);
      }
      Map<String, Resolution> userResolutions =
          new HashMap<String, Resolution>();
      for (int i = 0; i < resolutionKeys.length; i++) {
        userResolutions.put(resolutionKeys[i],
            Resolution.valueOf(resolutionValues[i]));
      }
      mAdapter.setRestoredState(chosenValuesMap, userResolutions);
    }
    // And finally, call this to make sure we update the button as appropriate.
    Log.e(TAG, "going to call onDecisionMade");
    this.onDecisionMade();

  }

  private class DiscardChangesAndDeleteLocalListener
      implements View.OnClickListener {

    @Override
    public void onClick(View v) {
      // We should do a popup.
      AlertDialog.Builder builder = new AlertDialog.Builder(
          ConflictResolutionRowActivity.this.getActionBar()
          .getThemedContext());
      builder.setMessage(
          getString(R.string.conflict_delete_local_confirmation_warning));
      builder.setPositiveButton(getString(R.string.yes),
          new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
              // TODO: delete the local version.
              // this will be a simple matter of deleting all the rows with the
              // same rowid on the local device.
              mIsShowingTakeServerDialog = false;
              DbTable dbTable =
                  DbTable.getDbTable(mLocal.getTableProperties());
              dbTable.deleteRowActual(mRowId);
              ConflictResolutionRowActivity.this.finish();
              Log.d(TAG, "deleted local and server versions");
            }
          });
      builder.setCancelable(true);
      builder.setNegativeButton(getString(R.string.cancel),
          new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
              dialog.cancel();
            }
          });
      builder.setOnCancelListener(new OnCancelListener() {

        @Override
        public void onCancel(DialogInterface dialog) {
          mIsShowingTakeServerDialog = false;
          dialog.dismiss();
        }
      });
      mIsShowingTakeServerDialog = true;
      builder.create().show();
    }
  }

  private class SetRowToDeleteOnServerListener
      implements View.OnClickListener {

    @Override
    public void onClick(View v) {
      // We should do a popup.
      AlertDialog.Builder builder = new AlertDialog.Builder(
          ConflictResolutionRowActivity.this.getActionBar()
          .getThemedContext());
      builder.setMessage(
          getString(R.string.conflict_delete_on_server_confirmation_warning));
      builder.setPositiveButton(getString(R.string.yes),
          new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
              // We're going to discard the local changes by acting as if
              // takeServer was pressed. Then we're going to flag row as
              // deleted.
              mIsShowingTakeLocalDialog = false;
              DbTable dbTable =
                  DbTable.getDbTable(mLocal.getTableProperties());
              Map<String, String> valuesToUse = new HashMap<String, String>();
              for (ConflictColumn cc : mConflictColumns) {
                valuesToUse.put(cc.getElementKey(), cc.getServerRawValue());
              }
              dbTable.resolveConflict(mRowId, mServerRowETag, valuesToUse);
              dbTable.markDeleted(mRowId);
              Log.d(TAG, "deleted the local version and marked the server" +
              		" version as deleting.");
              ConflictResolutionRowActivity.this.finish();
            }
          });
      builder.setCancelable(true);
      builder.setNegativeButton(getString(R.string.cancel),
          new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
              dialog.cancel();
            }
          });
      builder.setOnCancelListener(new OnCancelListener() {

        @Override
        public void onCancel(DialogInterface dialog) {
          mIsShowingTakeLocalDialog = false;
          dialog.dismiss();
        }
      });
      mIsShowingTakeLocalDialog = true;
      builder.create().show();
    }
}

  private class TakeLocalClickListener implements View.OnClickListener {

    @Override
    public void onClick(View v) {
      AlertDialog.Builder builder = new AlertDialog.Builder(
          ConflictResolutionRowActivity.this.getActionBar()
          .getThemedContext());
      builder.setMessage(getString(R.string.take_local_warning));
      builder.setPositiveButton(getString(R.string.yes),
          new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
              mIsShowingTakeLocalDialog = false;
              DbTable dbTable =
                  DbTable.getDbTable(mLocal.getTableProperties());
              Map<String, String> valuesToUse = new HashMap<String, String>();
              for (ConflictColumn cc : mConflictColumns) {
                valuesToUse.put(cc.getElementKey(), cc.getLocalRawValue());
              }
              dbTable.resolveConflict(mRowId, mServerRowETag, valuesToUse);
              ConflictResolutionRowActivity.this.finish();
            }
          });
      builder.setCancelable(true);
      builder.setNegativeButton(getString(R.string.cancel),
          new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
              dialog.cancel();
            }
          });
      builder.setOnCancelListener(new OnCancelListener() {

        @Override
        public void onCancel(DialogInterface dialog) {
          mIsShowingTakeLocalDialog = false;
        }
      });
      mIsShowingTakeLocalDialog = true;
      builder.create().show();
    }

  }

  private class TakeServerClickListener implements View.OnClickListener {


    @Override
    public void onClick(View v) {
      AlertDialog.Builder builder = new AlertDialog.Builder(
          ConflictResolutionRowActivity.this.getActionBar()
          .getThemedContext());
      builder.setMessage(getString(R.string.take_server_warning));
      builder.setPositiveButton(getString(R.string.yes),
          new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
              mIsShowingTakeServerDialog = false;
              DbTable dbTable =
                  DbTable.getDbTable(mLocal.getTableProperties());
              Map<String, String> valuesToUse = new HashMap<String, String>();
              for (ConflictColumn cc : mConflictColumns) {
                valuesToUse.put(cc.getElementKey(), cc.getServerRawValue());
              }
              dbTable.resolveConflict(mRowId, mServerRowETag, valuesToUse);
              ConflictResolutionRowActivity.this.finish();
            }
          });
      builder.setCancelable(true);
      builder.setNegativeButton(getString(R.string.cancel),
          new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
              dialog.cancel();
            }
          });
      builder.setOnCancelListener(new OnCancelListener() {

        @Override
        public void onCancel(DialogInterface dialog) {
          mIsShowingTakeServerDialog = false;
        }
      });
      mIsShowingTakeServerDialog = true;
      builder.create().show();
    }

  }

  private class ResolveRowClickListener implements View.OnClickListener {

    private final String TAG = ResolveRowClickListener.class.getSimpleName();

    @Override
    public void onClick(View v) {
      AlertDialog.Builder builder = new AlertDialog.Builder(
          ConflictResolutionRowActivity.this.getActionBar()
          .getThemedContext());
      builder.setMessage(getString(R.string.resolve_row_warning));
      builder.setPositiveButton(getString(R.string.yes),
          new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
              mIsShowingResolveDialog = false;
              DbTable dbTable =
                  DbTable.getDbTable(mLocal.getTableProperties());
              if (!isResolvable()) {
                // We should never have gotten here! Triz-ouble.
                Log.e(TAG, "[onClick--positive button] the row is not " +
                		"resolvable! The button shouldn't have been enabled.");
                Toast.makeText(ConflictResolutionRowActivity.this
                    .getActionBar().getThemedContext(),
                    getString(R.string.resolve_cannot_complete_message),
                    Toast.LENGTH_SHORT).show();
                return;
              }
              Map<String, String> valuesToUse = mAdapter.getResolvedValues();
              dbTable.resolveConflict(mRowId, mServerRowETag, valuesToUse);
              ConflictResolutionRowActivity.this.finish();
            }
          });
      builder.setCancelable(true);
      builder.setNegativeButton(getString(R.string.cancel),
          new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
              dialog.cancel();
            }
          });
      builder.setOnCancelListener(new OnCancelListener() {

        @Override
        public void onCancel(DialogInterface dialog) {
          mIsShowingResolveDialog = false;
        }
      });
      mIsShowingResolveDialog = true;
      builder.create().show();

    }

  }

}
