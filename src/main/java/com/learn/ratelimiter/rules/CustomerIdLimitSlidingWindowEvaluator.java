package com.learn.ratelimiter.rules;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CustomerIdLimitSlidingWindowEvaluator extends AbstractRateLimitEvaluator {
    private static CustomerIdLimitSlidingWindowEvaluator evaluator;
    //Map stores (customerId => (time => requestCount))
    //private Map<Integer, ConcurrentSkipListMap<Long, LongAdder>> customerReqs = new ConcurrentHashMap<>();
    private Map<Integer, NavigableMap<Long, LongAdder>> customerReqs = new ConcurrentHashMap<>();
    private static final Logger logger = Logger.getLogger("CustomerIdLimitSlidingWindowEvaluator");

    public CustomerIdLimitSlidingWindowEvaluator(RateLimitRule rule) {
        super(rule);
    }

    public synchronized static CustomerIdLimitSlidingWindowEvaluator getInstance(RateLimitRule rule) {
        if (evaluator == null)
            evaluator = new CustomerIdLimitSlidingWindowEvaluator(rule);
        return evaluator;
    }

    public synchronized static void deReferenceInstance() {
        evaluator = null;
    }

    @Override
    public synchronized boolean evaluateRule(int customerId) {
        long now = System.currentTimeMillis();
        NavigableMap<Long, LongAdder> accessCounts = customerReqs.computeIfAbsent(customerId, k -> new TreeMap<>());
        long windowStart = now - rule.getWindowInSeconds() * 1000;
        Long startKey = accessCounts.ceilingKey(windowStart);
        //no key between the start of the window and now, so just add the key for "now"
        if (startKey == null) {
            //System.out.println("Discarding older keys, windowStart:" + windowStart+". Counts:\n"+accessCounts);
            accessCounts = customerReqs.computeIfPresent(customerId, (k, v) -> new ConcurrentSkipListMap<>());
            accessCounts.computeIfAbsent(now, k -> new LongAdder()).add(1l);
            return true;
        }
        else {
            /*if (startKey != accessCounts.firstKey())
                System.out.println("Found startKey " + startKey + " for " + windowStart + " current: " + now);*/
            accessCounts = (NavigableMap<Long, LongAdder>) accessCounts.tailMap(startKey);
            customerReqs.put(customerId, accessCounts);
            long totalRequests = getTotalRequests(accessCounts, startKey);
            if (totalRequests >= rule.getThreshold()) {
                logger.log(Level.WARNING,
                           "Sliding window limit exceeded for customerId {0}. Total Requests: {1}",
                           new Object[] { customerId, totalRequests});
                return false;
            } else {
                accessCounts.computeIfAbsent(now, k -> new LongAdder()).add(1l);
            }
        }
        return true;
    }

    public Long getTotalRequests(NavigableMap<Long, LongAdder> accessCounts, Long startKey) {
        return accessCounts.tailMap(startKey)
                           .values()
                           .stream()
                           .map(v -> v.longValue())
                           .reduce(0l, Long::sum);
    }
}
