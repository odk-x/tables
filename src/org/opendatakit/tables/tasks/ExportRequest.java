package org.opendatakit.tables.tasks;

import java.io.File;

import org.opendatakit.common.android.data.TableProperties;

public class ExportRequest {

    private final TableProperties tp;
    private final File file;
    private final boolean includeProperties;
    private final boolean includeTimestamps;
    private final boolean includeAccessControl;
    private final boolean includeFormIds;
    private final boolean includeLocales;

    public ExportRequest(TableProperties tp, File file,
    		boolean includeTimestamps, boolean includeAccessControl,
    		boolean includeFormIds,	boolean includeLocales,
            boolean includeProperties ) {
        this.tp = tp;
        this.file = file;
        this.includeProperties = includeProperties;
        this.includeTimestamps = includeTimestamps;
        this.includeAccessControl = includeAccessControl;
        this.includeFormIds = includeFormIds;
        this.includeLocales = includeLocales;
    }

    public TableProperties getTableProperties() {
        return tp;
    }

    public File getFile() {
        return file;
    }

    public boolean getIncludeProperties() {
        return includeProperties;
    }

    public boolean getIncludeTimestamps() {
        return includeTimestamps;
    }

    public boolean getIncludeAccessControl() {
        return includeAccessControl;
    }

    public boolean getIncludeFormIds() {
        return includeFormIds;
    }

    public boolean getIncludeLocales() {
        return includeLocales;
    }
}