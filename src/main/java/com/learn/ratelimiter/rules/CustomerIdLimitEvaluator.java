package com.learn.ratelimiter.rules;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CustomerIdLimitEvaluator extends AbstractRateLimitEvaluator {
    private static           CustomerIdLimitEvaluator    evaluator;
    private                  Map<Integer, AccessTracker> customerReqs = new ConcurrentHashMap<>();
    private static final Logger logger = Logger.getLogger("CustomerIdLimitEvaluator");


    private CustomerIdLimitEvaluator(RateLimitRule rule) {
        super(rule);

    }

    public synchronized static AbstractRateLimitEvaluator getInstance(RateLimitRule rule) {
        if (evaluator == null)
            evaluator = new CustomerIdLimitEvaluator(rule);
        return evaluator;
    }

    public synchronized static void deReferenceInstance() {
        evaluator = null;
    }

    @Override
    public boolean evaluateRule(int customerId) {
        long nowSeconds = System.currentTimeMillis()/1000;
        AccessTracker tracker = customerReqs.computeIfAbsent(customerId,
                                                             k -> new AccessTracker(nowSeconds));
        if (tracker.getTime() + rule.getWindowInSeconds() <= nowSeconds) {
            tracker = customerReqs.computeIfPresent(customerId,
                                          (k,v) -> new AccessTracker(nowSeconds));
        }
        if (tracker.incrementCount() > rule.getThreshold()) {
            logger.log(Level.WARNING, "Fixed window limit exceeded for customerId {0}. Total requests {1}", new Object[]{customerId, tracker.count});
            return false;
        }
        return true;
    }

    private class AccessTracker {
        private AtomicLong count;
        private long          time;

        public AccessTracker(long time) {
            this.count = new AtomicLong(0);
            this.time = time;
        }

        public long getTime() {
            return time;
        }

        public long incrementCount() {
            return count.incrementAndGet();
        }
    }
}
