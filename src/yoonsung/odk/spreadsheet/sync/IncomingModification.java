package yoonsung.odk.spreadsheet.sync;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor(suppressConstructorProperties = true)
@Data
public class IncomingModification {
  List<SyncRow> rows;
  String tableSyncTag;

  public IncomingModification() {
    this.rows = new ArrayList<SyncRow>();
    this.tableSyncTag = null;
  }
}
