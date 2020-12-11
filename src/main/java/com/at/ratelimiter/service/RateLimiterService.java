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

  @Value("${cache.expiry:3600}")
  private int cacheExpiry;

  @Value("${rate.limit:100}")
  private int rateLimit;

  private final AcceptedRequestsTracker acceptedRequestsTracker;
  private final Map<String, LoadingCache<String, String>> requestCaches = new ConcurrentHashMap<>();

  public RateLimiterService(
      AcceptedRequestsTracker acceptedRequestsTracker) {
    this.acceptedRequestsTracker = acceptedRequestsTracker;
  }

  public boolean isWithinRateLimits(String ipAddress) {
    log.trace("requestMap: {}, {}", requestCaches.toString(), requestCaches.size());

    if (requestCaches.containsKey(ipAddress)) {
      LoadingCache<String, String> cacheForIp = requestCaches.get(ipAddress);
      log.trace("cache exists, size: {}", cacheForIp.size());

      // force eviction of expired keys
      cacheForIp.cleanUp();

      if (cacheForIp.size() >= rateLimit) {
        return false;
      } else {
        long currentTimeMillis = System.currentTimeMillis();

        // add an entry to the cache, format: UUID*IpAddress_TimeMillis
        cacheForIp.getUnchecked(UUID.randomUUID() + "*" + ipAddress + "_" + currentTimeMillis);

        // add an entry to the request tracker as well
        acceptedRequestsTracker.addToAcceptedRequests(ipAddress, currentTimeMillis);

        return true;
      }
    } else {
      log.trace("cache not found, adding the first entry now");

      long currentTimeMillis = System.currentTimeMillis();

      // create a new cache, add an entry to the cache, format: UUID*IpAddress_TimeMillis, then add the cache to the map
      LoadingCache<String, String> newCacheForIp = createCache();
      newCacheForIp.getUnchecked(UUID.randomUUID() + "*" + ipAddress + "_" + currentTimeMillis);
      requestCaches.putIfAbsent(ipAddress, newCacheForIp);

      // add an entry to the request tracker as well
      acceptedRequestsTracker.addToAcceptedRequests(ipAddress, currentTimeMillis);

      return true;
    }
  }

  private LoadingCache<String, String> createCache() {
    RemovalListener<String, String> removalListener = notification -> {
      // extract the ipAddress and currentTimeMillis from the entry
      String key = notification.getKey();
      String ipAddress = key.substring(key.indexOf("*") + 1, key.indexOf("_"));
      long currentTimeMillis = Long.parseLong(key.substring(key.indexOf("_") + 1));
      log.trace("key, ipAddress, currentTimeMillis: {}, {}, {}", key, ipAddress,
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
