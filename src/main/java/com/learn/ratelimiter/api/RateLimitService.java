package com.learn.ratelimiter.api;

import com.learn.ratelimiter.rules.RateLimitRule;

public interface RateLimitService {
    //we could return an exception as well instead of a boolean if it's for internal consumption
    //for a web api, returning a boolean response might be more suitable
    boolean rateLimit(int customerId);

    //these apis could belong to a separate rule mgmt service for better segregation of responsibility
    void addRateLimitRule(RateLimitRule rule);
    //void removeRateLimitRule(RateLimitRule rule);
}
