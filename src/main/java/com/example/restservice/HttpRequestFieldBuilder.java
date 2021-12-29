package com.example.restservice;

import com.tersesystems.echopraxia.Field;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;

public class HttpRequestFieldBuilder implements Field.Builder {
    public List<Field> requestFields(HttpServletRequest request) {
        Field urlField = string("request_uri", request.getRequestURI());
        Field methodField = string("request_method", request.getMethod());
        return Arrays.asList(urlField, methodField);
    }
}
