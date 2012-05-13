package org.opendatakit.tables.sync.exception;

public class InvalidAuthTokenException extends Exception {

  private static final long serialVersionUID = 1L;

  public InvalidAuthTokenException() {
    super();
  }

  /**
   * @param detailMessage
   * @param throwable
   */
  public InvalidAuthTokenException(String detailMessage, Throwable throwable) {
    super(detailMessage, throwable);
  }

  /**
   * @param detailMessage
   */
  public InvalidAuthTokenException(String detailMessage) {
    super(detailMessage);
  }

  /**
   * @param throwable
   */
  public InvalidAuthTokenException(Throwable throwable) {
    super(throwable);
  }

}
