package yoonsung.odk.spreadsheet.sync.aggregate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResponseErrorHandler;

import android.util.Log;

public class AggregateResponseErrorHandler implements ResponseErrorHandler {

  private static final String TAG = AggregateResponseErrorHandler.class.getSimpleName();

  @Override
  public void handleError(ClientHttpResponse resp) throws IOException {
    HttpStatus status = resp.getStatusCode();
    String body = readInput(resp.getBody());
    Log.i(TAG, "handleError: " + status.value() + " " + resp.getStatusText());
    // TODO: implement more sophisticated error handling
    if (status.value() / 100 == 4)
      throw new HttpClientErrorException(status, body);
    else if (status.value() / 100 == 5)
      throw new HttpServerErrorException(status, body);
  }

  @Override
  public boolean hasError(ClientHttpResponse resp) throws IOException {
    return resp.getStatusCode().value() / 100 != 2;
  }

  private String readInput(InputStream is) throws IOException {
    BufferedReader br = new BufferedReader(new InputStreamReader(is));
    StringBuilder sb = new StringBuilder();
    String line;
    while ((line = br.readLine()) != null) {
      sb.append(line);
    }
    return sb.toString();
  }

}
