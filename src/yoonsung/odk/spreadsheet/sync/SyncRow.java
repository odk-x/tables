package yoonsung.odk.spreadsheet.sync;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor(suppressConstructorProperties=true)
@Data
public class SyncRow {
  private String rowId;
  private String syncTag;
  private boolean deleted;
  private Map<String, String> values;
}
