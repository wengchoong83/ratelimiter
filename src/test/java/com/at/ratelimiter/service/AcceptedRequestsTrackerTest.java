package com.at.ratelimiter.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.jupiter.api.Test;

class AcceptedRequestsTrackerTest {

  @Test
  void addToTracker_successfully() {
    AcceptedRequestsTracker acceptedRequestsTracker = new AcceptedRequestsTracker();
    acceptedRequestsTracker
        .addToAcceptedRequests("1.0.0.1", System.currentTimeMillis());
    acceptedRequestsTracker
        .addToAcceptedRequests("1.0.0.2", System.currentTimeMillis());

    assertEquals(2, acceptedRequestsTracker.getAcceptedRequests().size());
    assertEquals(1, acceptedRequestsTracker.getAcceptedRequests().get("1.0.0.1").size());
    assertEquals(1, acceptedRequestsTracker.getAcceptedRequests().get("1.0.0.2").size());
  }

  @Test
  void removeFromTracker_successfully() {
    AcceptedRequestsTracker acceptedRequestsTracker = new AcceptedRequestsTracker();
    acceptedRequestsTracker
        .addToAcceptedRequests("1.0.0.1", 1l);

    assertEquals(1, acceptedRequestsTracker.getAcceptedRequests().get("1.0.0.1").size());
    acceptedRequestsTracker.removeFromAcceptedRequests("1.0.0.1", 1l);
    assertEquals(0, acceptedRequestsTracker.getAcceptedRequests().get("1.0.0.1").size());
  }

  @Test
  void getWaitTime_returnsApproxCorrectTimings() throws InterruptedException {
    AcceptedRequestsTracker acceptedRequestsTracker = new AcceptedRequestsTracker();
    acceptedRequestsTracker.setCacheExpiry(10);

    acceptedRequestsTracker
        .addToAcceptedRequests("1.0.0.1", System.currentTimeMillis());
    Thread.sleep(2000l);
    acceptedRequestsTracker
        .addToAcceptedRequests("1.0.0.1", System.currentTimeMillis());

    long waitTime = acceptedRequestsTracker.getWaitTime("1.0.0.1");
    assertTrue(waitTime >= 7l);
    assertTrue(waitTime <= 10l);
  }
}