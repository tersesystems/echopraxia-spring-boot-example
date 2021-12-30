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
                HttpServletRequest request =
                    ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
                        .getRequest();
                return fb.requestFields(request);
              });

  @GetMapping("/greeting")
  public Greeting greeting(@RequestParam(value = "name", defaultValue = "World") String name) {
    logger.info("Greetings {}", fb -> fb.onlyString("greeting_name", name));

    return new Greeting(counter.incrementAndGet(), String.format(template, name));
  }
}
