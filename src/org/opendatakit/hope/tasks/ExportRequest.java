package org.opendatakit.hope.tasks;

import java.io.File;

import org.opendatakit.hope.data.TableProperties;

public class ExportRequest {

    private final TableProperties tp;
    private final File file;
    private final boolean includeProperties;
    private final boolean includeTimestamps;
    private final boolean includeUriUsers;
    private final boolean includeInstanceNames;
    private final boolean includeFormIds;
    private final boolean includeLocales;

    public ExportRequest(TableProperties tp, File file,
    		boolean includeTimestamps, boolean includeUriUsers,
    		boolean includeInstanceNames, boolean includeFormIds,
    		boolean includeLocales,
            boolean includeProperties ) {
        this.tp = tp;
        this.file = file;
        this.includeProperties = includeProperties;
        this.includeTimestamps = includeTimestamps;
        this.includeUriUsers = includeUriUsers;
        this.includeInstanceNames = includeInstanceNames;
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

    public boolean getIncludeUriUsers() {
        return includeUriUsers;
    }

    public boolean getIncludeInstanceNames() {
        return includeInstanceNames;
    }

    public boolean getIncludeFormIds() {
        return includeFormIds;
    }

    public boolean getIncludeLocales() {
        return includeLocales;
    }
}