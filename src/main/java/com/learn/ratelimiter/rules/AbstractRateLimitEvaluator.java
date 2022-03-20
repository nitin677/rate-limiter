package com.learn.ratelimiter.rules;

public abstract class AbstractRateLimitEvaluator {
    RateLimitRule rule;

    public AbstractRateLimitEvaluator(RateLimitRule rule) {
        this.rule = rule;
    }

    public abstract boolean evaluateRule(int customerId);
}
