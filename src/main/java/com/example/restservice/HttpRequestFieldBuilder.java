package com.example.restservice;

import echopraxia.api.*;

import jakarta.servlet.http.HttpServletRequest;

import java.time.Instant;

public class HttpRequestFieldBuilder implements FieldBuilder {

  public static final HttpRequestFieldBuilder instance = new HttpRequestFieldBuilder();

  public FieldBuilderResult requestFields(HttpServletRequest request) {
    var urlField = string("request_uri", request.getRequestURI());
    var methodField = string("request_method", request.getMethod());
    var remoteAddressField = string("request_remote_addr", request.getRemoteAddr());
    var uniqueId = number("unique_id", System.currentTimeMillis());
    return list(urlField, methodField, remoteAddressField, uniqueId);
  }

  public Field keyValue(String name, Instant instant) {
    return keyValue(name, Value.string(instant.toString()));
  }
}
