package com.at.ratelimiter.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.jupiter.api.Test;

class RateLimiterServiceTest {

  private AcceptedRequestsTracker acceptedRequestsTracker = new AcceptedRequestsTracker();

  @Test
  void basic_ratelimiting_is_successful() {
    RateLimiterService rateLimiterService = new RateLimiterService(acceptedRequestsTracker);
    rateLimiterService.setRateLimit(2);
    rateLimiterService.setCacheSize(100);
    rateLimiterService.setCacheExpiry(10);

    assertTrue(rateLimiterService.isWithinRateLimits("1.0.0.1"));
    assertTrue(rateLimiterService.isWithinRateLimits("1.0.0.1"));
    assertFalse(rateLimiterService.isWithinRateLimits("1.0.0.1"));
    assertFalse(rateLimiterService.isWithinRateLimits("1.0.0.1"));
  }

  @Test
  void basic_ratelimiting_multiIP_is_successful() throws InterruptedException {
    RateLimiterService rateLimiterService = new RateLimiterService(acceptedRequestsTracker);
    rateLimiterService.setRateLimit(2);
    rateLimiterService.setCacheSize(100);
    rateLimiterService.setCacheExpiry(10);

    assertTrue(rateLimiterService.isWithinRateLimits("1.0.0.1"));
    assertTrue(rateLimiterService.isWithinRateLimits("1.0.0.1"));
    assertFalse(rateLimiterService.isWithinRateLimits("1.0.0.1"));
    assertFalse(rateLimiterService.isWithinRateLimits("1.0.0.1"));

    assertTrue(rateLimiterService.isWithinRateLimits("1.0.0.2"));
    assertTrue(rateLimiterService.isWithinRateLimits("1.0.0.2"));
    assertFalse(rateLimiterService.isWithinRateLimits("1.0.0.2"));
    assertFalse(rateLimiterService.isWithinRateLimits("1.0.0.2"));
  }

  @Test
  void ratelimiter_allows_thru_successfully_after_X_time() throws InterruptedException {
    RateLimiterService rateLimiterService = new RateLimiterService(acceptedRequestsTracker);
    rateLimiterService.setRateLimit(1);
    rateLimiterService.setCacheSize(100);
    rateLimiterService.setCacheExpiry(5);

    assertTrue(rateLimiterService.isWithinRateLimits("1.0.0.1"));
    long initialTime = System.currentTimeMillis();
    assertFalse(rateLimiterService.isWithinRateLimits("1.0.0.1"));

    Thread.sleep(5000 - (System.currentTimeMillis() - initialTime));

    assertTrue(rateLimiterService.isWithinRateLimits("1.0.0.1"));
    assertFalse(rateLimiterService.isWithinRateLimits("1.0.0.1"));
  }

  @Test
  void basic_ratelimiting_large_load_is_successful() throws InterruptedException {
    RateLimiterService rateLimiterService = new RateLimiterService(acceptedRequestsTracker);
    rateLimiterService.setRateLimit(200);
    rateLimiterService.setCacheSize(100000);
    rateLimiterService.setCacheExpiry(60 * 60 * 1000);

    for (int i = 0; i < 200; i++) {
      assertTrue(rateLimiterService.isWithinRateLimits("1.0.0.1"));
    }

    assertFalse(rateLimiterService.isWithinRateLimits("1.0.0.1"));
  }
}
