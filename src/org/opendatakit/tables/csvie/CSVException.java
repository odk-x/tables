package org.opendatakit.tables.csvie;

/**
 * An exception for any issues with the CSV importing and exporting.
 */
public class CSVException extends Exception {
	
	private static final long serialVersionUID = 1L;
	
	public CSVException(String message) {
		super(message);
	}
	
	public CSVException(Throwable cause) {
		super(cause);
	}
	
	public CSVException(String message, Throwable cause) {
		super(message, cause);
	}
	
}
