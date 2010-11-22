package yoonsung.odk.spreadsheet.SMS;

/**
 * An exception for invalid queries
 */
public class InvalidQueryException extends Exception {
	
	private static final long serialVersionUID = 1L;
	
	/**
	 * Constructs a new InvalidQueryException
	 * @param message the detail message
	 */
	protected InvalidQueryException(String message) {
		super(message);
	}
	
}
