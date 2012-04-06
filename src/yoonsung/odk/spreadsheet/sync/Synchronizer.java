package yoonsung.odk.spreadsheet.sync;

import java.util.List;
import java.util.Map;

public interface Synchronizer {

  public String createTable(String tableId, Map<String, Integer> cols);

  public void deleteTable(String tableId);

  public IncomingModification getUpdates(String tableId, String currentSyncTag);

  public Modification insertRows(String tableId, List<SyncRow> rowsToInsert);

  public Modification updateRows(String tableId, List<SyncRow> rowsToUpdate);

  public String deleteRows(String tableId, List<String> rowIds);


}