package yoonsung.odk.spreadsheet.Activity;

import yoonsung.odk.spreadsheet.R;
import yoonsung.odk.spreadsheet.data.DataManager;
import yoonsung.odk.spreadsheet.data.DbHelper;
import yoonsung.odk.spreadsheet.data.TableProperties;
import android.app.ListActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class AggregateChooseTablesActivity extends ListActivity {

  ListView tablesView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.aggregate_choose_tables_activity);

    setListAdapter(new ArrayAdapter<TableProperties>(this,
        android.R.layout.simple_list_item_multiple_choice, getTables()));

    final ListView listView = getListView();

    listView.setItemsCanFocus(false);
    listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

    int count = listView.getCount();
    for (int i = 0; i < count; i++) {
      TableProperties tp = (TableProperties) listView.getItemAtPosition(i);
      if (tp.isSynchronized()) {
        listView.setItemChecked(i, true);
      }
    }
  }

  private TableProperties[] getTables() {
    DbHelper dbh = DbHelper.getDbHelper(this);
    DataManager dm = new DataManager(dbh);
    return dm.getDataTableProperties();
  }

  @Override
  protected void onListItemClick(ListView l, View v, int position, long id) {
    ListView listView = getListView();
    TableProperties tp = (TableProperties) listView.getItemAtPosition(position);
    tp.setSynchronized(listView.isItemChecked(position));
  }
}
