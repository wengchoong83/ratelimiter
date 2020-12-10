package com.at.ratelimiter.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Data
public class RateLimiterService {

  @Value("${cache.size:1000}")
  private int cacheSize;

  @Value("${cache.expiry:10}")
  private int cacheExpiry;

  @Value("${rate.limit:2}")
  private int rateLimit;

  private final AcceptedRequestsTracker acceptedRequestsTracker;
  private final Map<String, LoadingCache<String, String>> requestCaches = new ConcurrentHashMap<>();

  public RateLimiterService(
      AcceptedRequestsTracker acceptedRequestsTracker) {
    this.acceptedRequestsTracker = acceptedRequestsTracker;
  }

  public boolean isWithinRateLimits(String ipAddress) {
    log.info("---");
    log.info("request ip address: " + ipAddress);
    log.trace("requestMap: {}, {}", requestCaches.toString(), requestCaches.size());

    if (requestCaches.containsKey(ipAddress)) {
      LoadingCache<String, String> cacheForIp = requestCaches.get(ipAddress);
      log.info("cache exists, size: {}", cacheForIp.size());

      // force eviction of expired keys
      cacheForIp.cleanUp();
      if (cacheForIp.size() >= rateLimit) {
        return false;
      } else {
        long currentTimeMillis = System.currentTimeMillis();
        cacheForIp.getUnchecked(UUID.randomUUID() + "*" + ipAddress + "_" + currentTimeMillis);

        acceptedRequestsTracker.addToAcceptedRequests(ipAddress, currentTimeMillis);
        return true;
      }
    } else {
      LoadingCache<String, String> newCacheForIp = createCache();
      log.info("cache not found, adding the first entry now");

      long currentTimeMillis = System.currentTimeMillis();
      newCacheForIp.getUnchecked(UUID.randomUUID() + "*" + ipAddress + "_" + currentTimeMillis);

      acceptedRequestsTracker.addToAcceptedRequests(ipAddress, currentTimeMillis);
      requestCaches.putIfAbsent(ipAddress, newCacheForIp);

      return true;
    }
  }

  public LoadingCache<String, String> createCache() {
    RemovalListener<String, String> removalListener = notification -> {
      String key = notification.getKey();
      String ipAddress = key.substring(key.indexOf("*") + 1, key.indexOf("_"));
      long currentTimeMillis = Long.parseLong(key.substring(key.indexOf("_") + 1));
      log.info("key, ipAddress, currentTimeMillis: {}, {}, {}", key, ipAddress,
          currentTimeMillis);

      acceptedRequestsTracker.removeFromAcceptedRequests(ipAddress, currentTimeMillis);
    };

    return CacheBuilder.newBuilder()
        .maximumSize(cacheSize)
        .expireAfterWrite(cacheExpiry, TimeUnit.SECONDS)
        .removalListener(removalListener)
        .build(
            new CacheLoader<String, String>() {
              public String load(String key) {
                return key;
              }
            });
  }
}
