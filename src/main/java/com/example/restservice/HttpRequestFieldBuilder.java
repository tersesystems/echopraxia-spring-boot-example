package com.example.restservice;

import com.tersesystems.echopraxia.api.*;
import com.tersesystems.echopraxia.spi.*;

import java.time.Instant;

import jakarta.servlet.http.HttpServletRequest;

public class HttpRequestFieldBuilder implements PresentationFieldBuilder {

  public static final HttpRequestFieldBuilder instance = new HttpRequestFieldBuilder();

  public FieldBuilderResult requestFields(HttpServletRequest request) {
    var urlField = string("request_uri", request.getRequestURI());
    var methodField = string("request_method", request.getMethod());
    var remoteAddressField = string("request_remote_addr", request.getRemoteAddr());
    var uniqueId = number("unique_id", System.currentTimeMillis());
    return list(urlField, methodField, remoteAddressField, uniqueId);
  }

  public static PresentationField kv(String name, Instant value) {
    return instance.string(name, value.toString());
  }
}
