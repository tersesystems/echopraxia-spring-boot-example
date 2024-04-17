package com.example.restservice;

import com.tersesystems.echopraxia.Logger;
import com.tersesystems.echopraxia.LoggerFactory;
import com.tersesystems.echopraxia.api.Condition;
import org.jetbrains.annotations.NotNull;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

@RestController
public class GreetingController {

  private static final String template = "Hello, %s!";
  private final AtomicLong counter = new AtomicLong();

  private final Logger<HttpRequestFieldBuilder> logger =
    LoggerFactory.getLogger(getClass(), HttpRequestFieldBuilder.instance)
      .withFields(
        fb ->
          // Any fields that you set in context you can set conditions on later,
          // i.e. on the URI path, content type, or extra headers.
          // These fields will be visible in the JSON file, not shown in console.
          fb.requestFields(httpServletRequest())
      );

  @NotNull
  private HttpServletRequest httpServletRequest() {
    return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
  }

  @GetMapping("/")
  public Greeting greeting(@RequestParam(value = "name", defaultValue = "World") String name) {
    // Log using a field builder to add a greeting_name field to JSON
    logger.info("Greetings {}", fb -> fb.string("greeting_name", name).abbreviateAfter(4));

    // Clear MDC on every request...
    MDC.clear();

    // You can put MDC in the current thread, and threadContext/threadLocal methods will work
    MDC.put("contextKey", "contextValue");
    MDC.put("currentInstant", Instant.now().toString());

    // and have it available as conditions and fields
    Condition c = Condition.anyMatch(p -> Objects.equals(p.name(), "contextKey"));
    logger.withThreadContext().info(c, "Calling withThreadContext() adds MDC variables!");

    return new Greeting(counter.incrementAndGet(), String.format(template, name));
  }
}
