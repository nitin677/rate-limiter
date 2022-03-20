package com.learn.ratelimiter.impl;

import com.learn.ratelimiter.api.RateLimitService;
import com.learn.ratelimiter.rules.AbstractRateLimitEvaluator;
import com.learn.ratelimiter.rules.RateLimitRule;
import com.learn.ratelimiter.rules.repository.IRuleRepository;
import com.learn.ratelimiter.rules.repository.RuleRepository;

import java.util.List;
import java.util.stream.Collectors;

public class RateLimiter implements RateLimitService {
    private final IRuleRepository                  ruleRepo;
    private       List<AbstractRateLimitEvaluator> evaluators;

    public RateLimiter() {
        this.ruleRepo = new RuleRepository();
    }

    private void initialize(List<RateLimitRule> rules) {
        this.evaluators = rules.stream().map(r -> r.getEvaluator()).collect(Collectors.toList());
    }

    @Override
    public boolean rateLimit(int customerId) {
        boolean allowed = true;
        for (RateLimitRule rule : this.ruleRepo.getRules()) {
            allowed = rule.evaluateRequest(customerId);
            if (!allowed)
                return false;
        }
        return allowed;
    }

    @Override
    public void addRateLimitRule(RateLimitRule rule) {
        this.ruleRepo.addRule(rule);
    }
}
