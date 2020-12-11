package com.at.ratelimiter.service;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Data
public class AcceptedRequestsTracker {

  @Value("${cache.expiry:3600}")
  private int cacheExpiry;

  private final Map<String, LinkedList<Long>> acceptedRequests = new ConcurrentHashMap<>();

  public void addToAcceptedRequests(String ipAddress, Long currentTimeMillis) {
    LinkedList<Long> acceptedRequestsForIp;

    if (acceptedRequests.containsKey(ipAddress)) {
      acceptedRequestsForIp = acceptedRequests.get(ipAddress);
      acceptedRequestsForIp.add(currentTimeMillis);
    } else {
      acceptedRequestsForIp = new LinkedList<>();
      acceptedRequestsForIp.add(currentTimeMillis);
      acceptedRequests.putIfAbsent(ipAddress, acceptedRequestsForIp);
    }

    log.trace("accepted reqs for ip {}: {}", ipAddress, acceptedRequestsForIp.size());
  }

  public void removeFromAcceptedRequests(String ipAddress, Long currentTimeMillis) {
    List<Long> acceptedRequestsForIp = acceptedRequests.get(ipAddress);
    acceptedRequestsForIp.remove(currentTimeMillis);
  }

  public long getWaitTime(String ipAddress) {
    LinkedList<Long> acceptedRequestsForIp = acceptedRequests.get(ipAddress);
    long elapsedTimeSinceFirstReq =
        (System.currentTimeMillis() - acceptedRequestsForIp.getFirst()) / 1000;

    return cacheExpiry - elapsedTimeSinceFirstReq;
  }
}
