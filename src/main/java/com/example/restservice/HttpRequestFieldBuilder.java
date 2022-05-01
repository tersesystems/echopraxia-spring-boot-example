package com.example.restservice;

import com.tersesystems.echopraxia.api.*;

import javax.servlet.http.HttpServletRequest;

public class HttpRequestFieldBuilder implements FieldBuilder {

  public static final HttpRequestFieldBuilder instance = new HttpRequestFieldBuilder();

  public FieldBuilderResult requestFields(HttpServletRequest request) {
    Field urlField = string("request_uri", request.getRequestURI());
    Field methodField = string("request_method", request.getMethod());
    Field remoteAddressField = string("request_remote_addr", request.getRemoteAddr());
    return list(urlField, methodField, remoteAddressField);
  }
}
