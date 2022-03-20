package com.learn.ratelimiter.rules;

public class RateLimitRule {

    public static RateLimitRuleBuilder builder() {
        return new RateLimitRuleBuilderImpl();
    }

    public AbstractRateLimitEvaluator getEvaluator() {
        if (getAspect().equals(Aspect.CUSTOMER_ID)) {
            return CustomerIdLimitEvaluator.getInstance(this);
        } else if (getAspect().equals(Aspect.CUSTOMER_ID_SLIDING_WINDOW)) {
            return CustomerIdLimitSlidingWindowEvaluator.getInstance(this);
        }
        return CustomerIdLimitEvaluator.getInstance(this);
    }

    public boolean evaluateRequest(int customerId) {
        return getEvaluator().evaluateRule(customerId);
    }

    public enum Aspect {
        CUSTOMER_ID, CUSTOMER_ID_SLIDING_WINDOW;
    }

    private RateLimitRule(Aspect aspect, long threshold, int windowInSeconds) {
        this.aspect = aspect;
        this.threshold = threshold;
        this.windowInSeconds = windowInSeconds;
    }

    private Aspect aspect;
    private long   threshold;
    private int windowInSeconds;

    public Aspect getAspect() {
        return aspect;
    }

    public long getThreshold() {
        return threshold;
    }

    public int getWindowInSeconds() {
        return windowInSeconds;
    }

    public interface RateLimitRuleBuilder {
        public RateLimitRuleBuilder threshold(long threshold);
        public RateLimitRuleBuilder windowInSeconds(int windowInSeconds);
        public RateLimitRuleBuilder limitBy(Aspect aspect);
        public RateLimitRule build();
    }

    public static class RateLimitRuleBuilderImpl implements RateLimitRuleBuilder {

        private long threshold;
        private int windowInSeconds;
        private Aspect aspect;

        @Override
        public RateLimitRuleBuilder threshold(long threshold) {
            this.threshold = threshold;
            return this;
        }

        @Override
        public RateLimitRuleBuilder windowInSeconds(int windowInSeconds) {
            this.windowInSeconds = windowInSeconds;
            return this;
        }

        @Override
        public RateLimitRuleBuilder limitBy(Aspect aspect) {
            this.aspect = aspect;
            return this;
        }

        @Override
        public RateLimitRule build() {
            return new RateLimitRule(this.aspect, this.threshold, this.windowInSeconds);
        }
    }
}
