package sync;

import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.opendatakit.tables.Activity.SpreadSheet;
import org.opendatakit.tables.data.ColumnProperties;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.KeyValueStore;
import org.opendatakit.tables.data.Query;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.sync.IncomingModification;
import org.opendatakit.tables.sync.Modification;
import org.opendatakit.tables.sync.SyncProcessor;
import org.opendatakit.tables.sync.SyncRow;
import org.opendatakit.tables.sync.Synchronizer;
import org.opendatakit.tables.utils.TableFileUtils;

import android.content.Context;
import android.content.SyncResult;

public class BaseSyncProcessorTest {
  protected DbHelper helper;
  protected SyncProcessor processor;
  @Mock
  protected Synchronizer synchronizer;
  protected List<String> dbColumnNames;
  protected TableProperties tp;
  protected Query query;

  @Before
  public void setUp() throws IOException {
    MockitoAnnotations.initMocks(this);
    Context context = new SpreadSheet();
    this.helper = DbHelper.getDbHelper(context, TableFileUtils.extractAppName());
    this.processor = new SyncProcessor(helper, synchronizer);

    setUpSynchronizer();
    createTable();

    this.tp = TableProperties.getTablePropertiesForTable(dbh, tp.getTableId(),
        KeyValueStore.Type.ACTIVE);
    this.query = new Query(TableProperties.getTablePropertiesForAll(dbh,
        KeyValueStore.Type.ACTIVE), tp);
  }

  @After
  public void tearDown() {
    try {
      this.tp = TableProperties.getTablePropertiesForTable(dbh, tp.getTableId(),
          KeyValueStore.Type.ACTIVE);
      this.tp.deleteTableActual();
    } catch (ArrayIndexOutOfBoundsException e) {
      // ignore
    }
  }

  private void setUpSynchronizer() throws IOException {
    when(
        synchronizer.createTable(anyString(), anyString(), anyMapOf(String.class, Integer.class),
            anyString())).thenReturn(uuid());
    when(synchronizer.getUpdates(anyString(), anyString())).then(returnEmptyIncomingModification());
    when(synchronizer.insertRows(anyString(), anyString(), anyListOf(SyncRow.class))).then(
        returnModification());
    when(synchronizer.updateRows(anyString(), anyString(), anyListOf(SyncRow.class))).then(
        returnModification());
    when(synchronizer.deleteRows(anyString(), anyString(), anyListOf(String.class))).thenReturn(
        uuid());
  }

  private void createTable() {
    this.tp = TableProperties.addTable(helper, Data.tableName, Data.tableName,
        TableProperties.TableType.DATA);
    this.tp.setSynchronized(true);

    dbColumnNames = new ArrayList<String>();
    for (Entry<String, Integer> entry : Data.columns.entrySet()) {
      ColumnProperties colProps = tp.addColumn(entry.getKey(), entry.getKey());
      colProps.setColumnType(entry.getValue());
      dbColumnNames.add(colProps.getColumnDbName());
    }
  }

  protected String uuid() {
    return UUID.randomUUID().toString();
  }

  protected Answer<Modification> returnModification() {
    return new Answer<Modification>() {
      @Override
      public Modification answer(InvocationOnMock invocation) throws Throwable {
        List<SyncRow> rows = (List<SyncRow>) invocation.getArguments()[2];
        Map<String, String> syncTags = new HashMap<String, String>();
        for (SyncRow row : rows) {
          syncTags.put(row.getRowId(), uuid());
        }
        String tableSyncTag = uuid();
        return new Modification(syncTags, tableSyncTag);
      }
    };

  }

  protected Answer<IncomingModification> returnEmptyIncomingModification() {
    return new Answer<IncomingModification>() {
      @Override
      public IncomingModification answer(InvocationOnMock invocation) throws Throwable {
        String currentSyncTag = (String) invocation.getArguments()[1];
        IncomingModification modification = new IncomingModification(new ArrayList<SyncRow>(),
            false, null, currentSyncTag);
        return modification;
      }
    };
  }

  protected ArgumentMatcher<Map<String, Integer>> containsKeys(final List<String> keys) {
    return new ArgumentMatcher<Map<String, Integer>>() {
      @Override
      public boolean matches(Object argument) {
        Map<String, Integer> given = (Map<String, Integer>) argument;
        for (String key : keys) {
          if (!given.containsKey(key))
            return false;
        }
        return true;
      }
    };
  }

  protected <T> ArgumentMatcher<List<T>> isSize(final int size) {
    return new ArgumentMatcher<List<T>>() {
      @Override
      public boolean matches(Object argument) {
        List<T> list = (List<T>) argument;
        return list.size() == size;
      }
    };
  }

  protected ArgumentMatcher<List<SyncRow>> containsRowIds(final List<String> rowIds) {
    return new ArgumentMatcher<List<SyncRow>>() {
      @Override
      public boolean matches(Object argument) {
        List<SyncRow> rows = (List<SyncRow>) argument;
        for (SyncRow row : rows) {
          String rowId = row.getRowId();
          if (!rowIds.remove(rowId))
            return false;
        }
        if (!rowIds.isEmpty())
          return false;
        else
          return true;
      }
    };
  }
}
