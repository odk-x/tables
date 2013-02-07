package org.opendatakit.tables.Task;

import java.io.File;

import org.opendatakit.tables.data.TableProperties;

public class ExportRequest {
    
    private final TableProperties tp;
    private final File file;
    private final boolean includeProperties;
    private final boolean includeTimestamps;
    private final boolean includePhoneNums;
    
    public ExportRequest(TableProperties tp, File file,
            boolean includeProperties, boolean includeTimestamps,
            boolean includePhoneNums) {
        this.tp = tp;
        this.file = file;
        this.includeProperties = includeProperties;
        this.includeTimestamps = includeTimestamps;
        this.includePhoneNums = includePhoneNums;
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
    
    public boolean getIncludePhoneNums() {
        return includePhoneNums;
    }
}