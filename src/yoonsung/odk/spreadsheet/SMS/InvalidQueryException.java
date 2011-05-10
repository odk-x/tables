package yoonsung.odk.spreadsheet.SMS;

/**
 * An exception for invalid queries
 */
public class InvalidQueryException extends Exception {
	
	private static final long serialVersionUID = 1L;
	
	public static final int NONEXISTENT_TARGET = 0;
	public static final int NONEXISTENT_COLUMN = 1;
	public static final int INVALID_VALUE = 2; // e.g. "apple" in a date column
	public static final int INVALID_FORMAT = 3;
	public static final int OTHER = 4;
	
	private int type;
	private String target;
	private String column;
	private String value;
	
	/**
	 * Constructs a new InvalidQueryException.
	 */
	protected InvalidQueryException(int type, String target, String column,
	        String value) {
	    super();
	    this.type = type;
	    this.target = target;
	    this.column = column;
	    this.value = value;
	}
	
	public int getType() {
	    return type;
	}
	
	public String getTarget() {
	    return target;
	}
	
	public String getColumn() {
	    return column;
	}
	
	public String getValue() {
	    return value;
	}
	
}
