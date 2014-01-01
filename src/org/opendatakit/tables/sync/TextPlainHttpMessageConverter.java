package org.opendatakit.tables.sync;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;

public class TextPlainHttpMessageConverter extends StringHttpMessageConverter {

  @Override
  public List<MediaType> getSupportedMediaTypes() {
    // The default implementation of this includes */* according to the doc at:
    // http://static.springsource.org/spring/docs/3.0.x/javadoc-api/org/springframework/http/converter/StringHttpMessageConverter.html
    // This is bad, as we are using xml message converters as well. So instead
    // we're going to ONLY support Text/Plain.
    List<MediaType> onlyType = new ArrayList<MediaType>();
    onlyType.add(MediaType.TEXT_PLAIN);
    return onlyType;
  }

  @Override
  protected String readInternal(Class<? extends String> clazz, HttpInputMessage inputMessage)
      throws IOException {
    return super.readInternal(clazz, inputMessage);
  }

  @Override
  protected void writeInternal(String s, HttpOutputMessage outputMessage) throws IOException {
    super.writeInternal(s, outputMessage);
  }

}
