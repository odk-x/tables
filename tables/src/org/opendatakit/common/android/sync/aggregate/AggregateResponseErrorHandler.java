/*
 * Copyright (C) 2012 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.opendatakit.common.android.sync.aggregate;

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

// TODO: decide if using and fix
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
