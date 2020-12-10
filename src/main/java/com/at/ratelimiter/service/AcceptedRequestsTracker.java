package com.at.ratelimiter.service;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Data
public class AcceptedRequestsTracker {

  @Value("${cache.expiry:10}")
  private int cacheExpiry;

  private final Map<String, LinkedList<Long>> acceptedRequests = new HashMap<>();

  public void addToAcceptedRequests(String ipAddress, Long currentTimeMillis) {
    LinkedList<Long> acceptedRequestsForIp;

    if (acceptedRequests.containsKey(ipAddress)) {
      acceptedRequestsForIp = acceptedRequests.get(ipAddress);
      acceptedRequestsForIp.add(currentTimeMillis);
    } else {
      acceptedRequestsForIp = new LinkedList<>();
      acceptedRequestsForIp.add(currentTimeMillis);
      acceptedRequests.put(ipAddress, acceptedRequestsForIp);
    }

    log.trace("accepted reqs for ip {}: {}", ipAddress, acceptedRequestsForIp.size());
  }

  public void removeFromAcceptedRequests(String ipAddress, Long currentTimeMillis) {
    List<Long> acceptedRequestsForIp = acceptedRequests.get(ipAddress);
    log.info("accepted reqs for ip {}: size before removal: {}", ipAddress,
        acceptedRequestsForIp.size());
    acceptedRequestsForIp.remove(currentTimeMillis);
    log.info("accepted reqs for ip {}: size after removal: {}", ipAddress,
        acceptedRequestsForIp.size());
  }

  public long getWaitTime(String ipAddress) {
    LinkedList<Long> acceptedRequestsForIp = acceptedRequests.get(ipAddress);
    long elapsedTimeSinceFirstInterval =
        (System.currentTimeMillis() - acceptedRequestsForIp.getFirst()) / 1000;

    return cacheExpiry - elapsedTimeSinceFirstInterval;
  }
}
