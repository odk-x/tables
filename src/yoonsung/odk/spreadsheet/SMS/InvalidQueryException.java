package yoonsung.odk.spreadsheet.SMS;

/**
 * An exception for invalid queries
 */
public class InvalidQueryException extends Exception {
	
	private static final long serialVersionUID = 1L;
	
	/** the code for an invalid or missing sheet specification */
	protected static final int INVALID_SHEET = 1;
	/** the code for an invalid limit specification */
	protected static final int INVALID_LIMIT = 2;
	
	/**
	 * Constructs a new InvalidQueryException
	 * @param message the detail message
	 */
	protected InvalidQueryException(String message) {
		super(message);
	}
	
}
