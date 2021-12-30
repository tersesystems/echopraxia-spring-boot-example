package com.example.restservice;

import com.tersesystems.echopraxia.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;

public class HttpRequestFieldBuilder implements Field.Builder {
  public List<Field> requestFields(HttpServletRequest request) {
    Field urlField = string("request_uri", request.getRequestURI());
    Field methodField = string("request_method", request.getMethod());
    Field remoteAddressField = string("request_remote_addr", request.getRemoteAddr());
    return Arrays.asList(urlField, methodField, remoteAddressField);
  }
}
