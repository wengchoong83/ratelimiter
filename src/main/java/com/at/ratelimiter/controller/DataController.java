package com.at.ratelimiter.controller;

import com.at.ratelimiter.service.AcceptedRequestsTracker;
import com.at.ratelimiter.service.RateLimiterService;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class DataController {

  private final RateLimiterService rateLimiterService;
  private final AcceptedRequestsTracker acceptedRequestsTracker;

  public DataController(RateLimiterService rateLimiterService,
      AcceptedRequestsTracker acceptedRequestsTracker) {
    this.rateLimiterService = rateLimiterService;
    this.acceptedRequestsTracker = acceptedRequestsTracker;
  }

  @GetMapping("/data")
  public ResponseEntity<String> getData(HttpServletRequest request) {
    log.info("---");

    if (rateLimiterService.isWithinRateLimits(request.getRemoteAddr())) {
      return ResponseEntity.ok("Data as requested");
    } else {
      log.info("responding 429");
      return new ResponseEntity<>(
          "Rate limit exceeded. Try again in " + acceptedRequestsTracker
              .getWaitTime(request.getRemoteAddr()) + " seconds",
          HttpStatus.TOO_MANY_REQUESTS);
    }
  }
}
