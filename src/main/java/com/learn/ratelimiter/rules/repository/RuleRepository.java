package com.learn.ratelimiter.rules.repository;

import com.learn.ratelimiter.rules.RateLimitRule;

import java.util.ArrayList;
import java.util.List;

public class RuleRepository implements IRuleRepository {
    private List<RateLimitRule> rules = new ArrayList<>();

    @Override
    public void addRule(RateLimitRule rule) {
        this.rules.add(rule);
    }

    @Override
    public List<RateLimitRule> getRules() {
        return rules;
    }
}
