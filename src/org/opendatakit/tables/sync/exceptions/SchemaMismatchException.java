package org.opendatakit.tables.sync.exceptions;

public class SchemaMismatchException extends Exception {

  private static final long serialVersionUID = 1L;

  public SchemaMismatchException() {
    super();
  }

  /**
   * @param detailMessage
   * @param throwable
   */
  public SchemaMismatchException(String detailMessage, Throwable throwable) {
    super(detailMessage, throwable);
  }

  /**
   * @param detailMessage
   */
  public SchemaMismatchException(String detailMessage) {
    super(detailMessage);
  }

  /**
   * @param throwable
   */
  public SchemaMismatchException(Throwable throwable) {
    super(throwable);
  }


}
