package yoonsung.odk.spreadsheet.sync.aggregate;

import java.io.IOException;
import java.net.URI;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.HttpRequestWrapper;

public class AggregateRequestInterceptor implements ClientHttpRequestInterceptor {

  private String accessToken;

  public AggregateRequestInterceptor(String accessToken) {
    this.accessToken = accessToken;
  }

  @Override
  public ClientHttpResponse intercept(HttpRequest request, byte[] body,
      ClientHttpRequestExecution execution) throws IOException {
    HttpRequest wrappedRequest = new AddAccessTokenHttpRequestWrapper(request);
    return execution.execute(wrappedRequest, body);
  }

  private class AddAccessTokenHttpRequestWrapper extends HttpRequestWrapper {

    public AddAccessTokenHttpRequestWrapper(HttpRequest request) {
      super(request);
    }

    @Override
    public URI getURI() {
      String uriString = super.getURI().toString();
      String accessTokenQuery = "?access_token=" + accessToken;
      URI uri = URI.create(uriString);
      if (uri.getQuery() != null)
        accessTokenQuery = "&" + accessTokenQuery;
      uri = URI.create(uriString + accessTokenQuery);
      return uri;
    }

  }

}
