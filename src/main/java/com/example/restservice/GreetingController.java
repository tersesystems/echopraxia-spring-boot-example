package com.example.restservice;

import com.tersesystems.echopraxia.Logger;
import com.tersesystems.echopraxia.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.atomic.AtomicLong;

@RestController
public class GreetingController {

  private static final String template = "Hello, %s!";
  private final AtomicLong counter = new AtomicLong();

  private final Logger<HttpRequestFieldBuilder> logger =
      LoggerFactory.getLogger(getClass())
          .withFieldBuilder(HttpRequestFieldBuilder.class)
          .withFields(
              fb -> {
                  // Any fields that you set in context you can set conditions on later,
                  // i.e. on the URI path, content type, or extra headers.
                  HttpServletRequest request =
                    ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
                        .getRequest();
                return fb.requestFields(request);
              });

  // Creates a debug logger that will filter out any requests that doesn't meet the condition.
  private final Logger<HttpRequestFieldBuilder> debugLogger = logger.withCondition(Conditions.debugCondition);

  @GetMapping("/greeting")
  public Greeting greeting(@RequestParam(value = "name", defaultValue = "World") String name) {
    logger.info("Greetings {}", fb -> fb.onlyString("greeting_name", name));

    // the logger must be set to DEBUG level and also meet the condition.
    debugLogger.debug(
        "This message only shows up when request_remote_addr is 127.0.0.1 and level>=DEBUG");

    return new Greeting(counter.incrementAndGet(), String.format(template, name));
  }
}
