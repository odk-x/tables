package sync;

import java.util.HashMap;
import java.util.Map;

import org.opendatakit.tables.data.ColumnProperties.ColumnType;

public class Data {

  public static final String tableName = "people";
  public static final Map<String, Integer> columns = new HashMap<String, Integer>() {
    {
      for (Columns col : Columns.values()) {
        put(col.name(), col.type());
      }
    }
  };

  public enum Columns {
    name(ColumnType.TEXT),
    age(ColumnType.NUMBER);

    private int columnType;

    Columns(int columnType) {
      this.columnType = columnType;
    }

    public int type() {
      return columnType;
    }
  }

  public enum Rows {
    dylan(23),
    bob(56);

    private int age;

    Rows(int age) {
      this.age = age;
    }

    public int age() {
      return age;
    }

    public Map<String, String> dataValues() {
      Map<String, String> values = new HashMap<String, String>();
      values.put(Columns.name.name(), name());
      values.put(Columns.age.name(), String.valueOf(age()));
      return values;
    }

  }
}
