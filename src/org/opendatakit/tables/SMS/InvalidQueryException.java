package org.opendatakit.tables.SMS;

/**
 * An exception for invalid queries
 */
public class InvalidQueryException extends Exception {
	
    private static final long serialVersionUID = 1L;
    
    enum Type {
        INVALID_FORMAT,
        NONEXISTENT_TARGET
    }
    
    private Type type;
    
    public InvalidQueryException(Type type) {
        this(type, null);
    }
    
    public InvalidQueryException(Type type, String msg) {
        super(msg);
        this.type = type;
    }
    
    public Type getType() {
        return type;
    }
}
