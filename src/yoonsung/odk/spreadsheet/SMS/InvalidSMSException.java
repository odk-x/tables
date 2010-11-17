package yoonsung.odk.spreadsheet.SMS;

/**
 * An exception for invalid incoming SMS queries
 * @author hkworden
 */
public class InvalidSMSException extends Exception {
	
	/**
	 * Constructs a new InvalidSMSException
	 * @param message the detail message
	 */
	protected InvalidSMSException(String message) {
		super(message);
	}
	
}
