package com.example.restservice;

import java.util.concurrent.atomic.AtomicLong;

import com.tersesystems.echopraxia.*;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@RestController
public class GreetingController {

	private static final String template = "Hello, %s!";
	private final AtomicLong counter = new AtomicLong();

	 private final Logger<HttpRequestFieldBuilder> logger = LoggerFactory.getLogger(getClass())
			 .withFieldBuilder(HttpRequestFieldBuilder.class);
	 // XXX need to be able to add fields and not evaluate them until log time.

	@GetMapping("/greeting")
	public Greeting greeting(@RequestParam(value = "name", defaultValue = "World") String name) {
		logger.withFields(fb -> {
			HttpServletRequest request =
					((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
							.getRequest();
			return fb.requestFields(request);
		}).info("Greetings {}", fb -> fb.onlyString("greeting_name", name));

		return new Greeting(counter.incrementAndGet(), String.format(template, name));
	}
}
