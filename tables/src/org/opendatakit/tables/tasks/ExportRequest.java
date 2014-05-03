package org.opendatakit.tables.tasks;

import org.opendatakit.common.android.data.TableProperties;

public class ExportRequest {

    private final TableProperties tp;
    private final String fileQualifier;

    /**
     * New style CSV export.
     * Exports two csv files to the output/csv directory under the appName:
     * <ul>
     * <li>tableid.fileQualifier.csv - data table</li>
     * <li>tableid.fileQualifier.properties.csv - metadata definition of this table</li>
     * </ul>
     * If fileQualifier is null or an empty string, then it emits to
     * <ul>
     * <li>tableid.csv - data table</li>
     * <li>tableid.properties.csv - metadata definition of this table</li>
     * </ul>
     *
     * @param tp
     * @param directory
     * @param fileQualifier
     */
    public ExportRequest(TableProperties tp, String fileQualifier) {
      this.tp = tp;
      this.fileQualifier = fileQualifier;
    }

    public TableProperties getTableProperties() {
        return tp;
    }

    public String getFileQualifier() {
      return fileQualifier;
    }
}