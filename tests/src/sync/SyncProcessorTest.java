package sync;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;

import yoonsung.odk.spreadsheet.data.DbTable;
import yoonsung.odk.spreadsheet.data.Table;
import yoonsung.odk.spreadsheet.sync.SyncRow;
import yoonsung.odk.spreadsheet.sync.SyncUtil;

@RunWith(SyncTestRunner.class)
public class SyncProcessorTest extends BaseSyncProcessorTest {

  @Test
  public void testCreateTableCallsSynchronizerAndEndsInRestState() throws IOException {
    this.processor.synchronizeTable(tp);
    verify(synchronizer).createTable(eq(tp.getTableId()), eq(tp.getDisplayName()),
        argThat(containsKeys(dbColumnNames)), anyString());
    tp = this.dm.getTableProperties(tp.getTableId());
    assertEquals(SyncUtil.State.REST, tp.getSyncState());
    assertEquals(false, tp.isTransactioning());
  }

  @Test
  public void testDeleteTableCallsSynchronizerAndDeletesTable() throws IOException {
    this.processor.synchronizeTable(tp);
    tp.deleteTable();
    this.processor.synchronizeTable(tp);
    verify(synchronizer).deleteTable(eq(tp.getTableId()));
    assertEquals(0, dm.getAllTableProperties().length);
  }

  @Test
  public void testInsertRowCallsSynchronizerAndEndsInRestState() throws IOException {
    this.processor.synchronizeTable(tp);
    DbTable table = dm.getDbTable(tp.getTableId());
    table.addRow(Data.Rows.dylan.dataValues());
    this.processor.synchronizeTable(tp);

    verify(synchronizer).insertRows(eq(tp.getTableId()), anyString(),
        argThat(super.<SyncRow> isSize(1)));

    Table rows = table.getRaw(new String[] { DbTable.DB_SYNC_STATE, DbTable.DB_TRANSACTIONING },
        new String[] { Data.Columns.name.name() }, new String[] { Data.Rows.dylan.name() }, null);

    assertEquals(1, rows.getHeight());
    assertEquals(SyncUtil.State.REST, Integer.parseInt(rows.getData(0, 0)));
    assertEquals(SyncUtil.boolToInt(false), Integer.parseInt(rows.getData(0, 1)));
  }

  @Test
  public void testUpdateRowCallsSynchronizerAndEndsInRestState() throws IOException {
    DbTable table = dm.getDbTable(tp.getTableId());
    table.addRow(Data.Rows.dylan.dataValues());
    this.processor.synchronizeTable(tp);

    String[] rowIds = table.getRaw().getRowIds();
    assertEquals(1, rowIds.length);

    Map<String, String> values = new HashMap<String, String>();
    values.put(Data.Columns.age.name(), "99");
    table.updateRow(rowIds[0], values);

    this.processor.synchronizeTable(tp);

    verify(synchronizer).updateRows(eq(tp.getTableId()), anyString(),
        argThat(super.<SyncRow> isSize(1)));

    Table rows = table.getRaw(new String[] { DbTable.DB_SYNC_STATE, DbTable.DB_TRANSACTIONING },
        null, null, null);
    assertEquals(1, rows.getHeight());
    assertEquals(SyncUtil.State.REST, Integer.parseInt(rows.getData(0, 0)));
    assertEquals(SyncUtil.boolToInt(false), Integer.parseInt(rows.getData(0, 1)));
  }

  @Test
  public void testDeleteRowCallsSynchronizerAndDeletesRow() throws InterruptedException,
      IOException {
    DbTable table = dm.getDbTable(tp.getTableId());
    table.addRow(Data.Rows.dylan.dataValues());
    this.processor.synchronizeTable(tp);

    String[] rowIds = table.getRaw().getRowIds();
    assertEquals(1, rowIds.length);

    table.markDeleted(rowIds[0]);
    this.processor.synchronizeTable(tp);

    verify(synchronizer).deleteRows(eq(tp.getTableId()), anyString(),
        argThat(super.<String> isSize(1)));

    Table rows = table.getRaw(new String[] { DbTable.DB_SYNC_STATE }, null, null, null);
    assertEquals(0, rows.getHeight());
  }
}
