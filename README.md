# ratelimiter

A simple service that ratelimits requests by requestor IP. 

## How do I run this?

1. This service was written in Java, and built on Spring Boot. You'll need to have JDK8 installed.
2. Clone this repo.
3. To build, `cd` into the the root directory of the project and run `./mvnw clean install`
4. To run, simply execute `./mvnw spring-boot:run`
5. The service will start up, and if you see the log line `Started RatelimiterApplication in X seconds...`, the service is up and running!
6. To use the endpoint via cURL: `curl localhost:8080/data`

## Digging in a little...

1. The `DataController` holds the endpoint, and is probably a good place to start.
2. `application.properties` provides some useful configuration points:
- `cache.size`: the maximum size of the caches used in the ratelimiting logic. Should never have to be bigger than the `rate.limit`
- `cache.expiry`: time in seconds, that entries will be kept in the caches
- `rate.limit`: amount of requests permitted within the time period, as specified in `cache.expiry`

## What's the core logic in this?

The main rate limiting logic uses Google Guava Caches (https://github.com/google/guava/wiki/CachesExplained) to store all accepted requests by requestor IP. These stored entries have a TTL (currently set at an hour) and will be evicted automatically. When the service gets a request, it checks to see if the rate limit for that IP (size of the cache) has been exceeded, and allows it through if it hasn't. Otherwise, a `429` is returned to the client, with a hint to retry in X seconds.

Guava Caches were used:
- to keep the implementation simple. As usage of this service scales, its probably a good idea to move to a centralized performant cache like Redis. Redis would also offer persistence.
- to avoid reinventing a home built cache

Additionally, all accepted request times are tracked in a bunch of LinkedLists (one for each IP) defined in `AcceptedRequestsTracker`. This was me trying to prevent a traversal of the cache to find the first entry in order to calculate the 'remaining seconds' needed for the hint. 

## Limitations / Future Improvement

Limitations:
- the concurrent maps used to store caches are currently unbounded. If the service were to be hit by a large number of IPs, it would probably fail. Switching out to Redis (with the right bounds) would probably help here.
- The Guava Cache's `size()` method returns an approximate size (as it doesn't traverse the entire cache to calculate size on each call). In cases where lots of concurrent requests are being processed, it could result in race conditions and the rate limiter allowing in more requests than it should.

Future improvement:
- authentication and validation can be added on the endpoint
- monitoring endpoints can be added on:
  - a health check endpoint
  - a stats endpoint, which might show the current capacity of the caches, for example
- swapping out the Guava Cache implementation for Redis (for reasons mentioned above)
- more edge case testing, and load tests
