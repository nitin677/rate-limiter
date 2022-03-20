package com.learn.ratelimiter.rules.repository;

import com.learn.ratelimiter.rules.RateLimitRule;

import java.util.List;

public interface IRuleRepository {
    void addRule(RateLimitRule rule);

    List<RateLimitRule> getRules();

    //RateLimitRule removeRule(RateLimitRule rule);
}
