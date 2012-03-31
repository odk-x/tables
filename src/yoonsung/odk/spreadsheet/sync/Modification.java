package yoonsung.odk.spreadsheet.sync;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor(suppressConstructorProperties = true)
@NoArgsConstructor
@Data
public class Modification {
  Map<String, String> syncTags;
  String tableSyncTag;
}
