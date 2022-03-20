package com.learn.ratelimiter.rules;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.LongAdder;

public class CustomerIdLimitSlidingWindowEvaluatorTest {
    @Test
    public void testGetTotalRequests() {
        CustomerIdLimitSlidingWindowEvaluator evaluator = CustomerIdLimitSlidingWindowEvaluator.getInstance(
                RateLimitRule.builder().
                        limitBy(RateLimitRule.Aspect.CUSTOMER_ID_SLIDING_WINDOW).threshold(10).windowInSeconds(1).build());
        ConcurrentSkipListMap<Long, LongAdder> map = new ConcurrentSkipListMap<>();
        map.computeIfAbsent(1l, k -> new LongAdder()).add(3);
        map.computeIfAbsent(3l, k -> new LongAdder()).add(4);
        map.computeIfAbsent(4l, k -> new LongAdder()).add(5);
        map.computeIfAbsent(7l, k -> new LongAdder()).add(6);
        Assertions.assertEquals(15, evaluator.getTotalRequests(map, 3l));
        Assertions.assertEquals(11, evaluator.getTotalRequests(map, 4l));
    }

    @Test
    public void testEvaluateRuleBasic() throws InterruptedException {
        CustomerIdLimitSlidingWindowEvaluator evaluator = CustomerIdLimitSlidingWindowEvaluator.getInstance(
                RateLimitRule.builder().
                        limitBy(RateLimitRule.Aspect.CUSTOMER_ID_SLIDING_WINDOW).threshold(2).windowInSeconds(1).build());
        Assertions.assertTrue(evaluator.evaluateRule(123));
        Assertions.assertTrue(evaluator.evaluateRule(123));
        Assertions.assertFalse(evaluator.evaluateRule(123));
    }

    @Test
    public void testEvaluateRuleAtEndOfWindow() throws InterruptedException {
        CustomerIdLimitSlidingWindowEvaluator evaluator = CustomerIdLimitSlidingWindowEvaluator.getInstance(
                RateLimitRule.builder().
                        limitBy(RateLimitRule.Aspect.CUSTOMER_ID_SLIDING_WINDOW).threshold(2).windowInSeconds(1).build());
        Assertions.assertTrue(evaluator.evaluateRule(234));
        Assertions.assertTrue(evaluator.evaluateRule(234));
        Assertions.assertFalse(evaluator.evaluateRule(234));

        Thread.sleep(900);
        Assertions.assertFalse(evaluator.evaluateRule(234));
    }

    @Test
    public void testEvaluateRuleInNextWindow() throws InterruptedException {
        CustomerIdLimitSlidingWindowEvaluator evaluator = CustomerIdLimitSlidingWindowEvaluator.getInstance(
                RateLimitRule.builder().
                        limitBy(RateLimitRule.Aspect.CUSTOMER_ID_SLIDING_WINDOW).threshold(2).windowInSeconds(1).build());
        Assertions.assertTrue(evaluator.evaluateRule(345));
        Assertions.assertTrue(evaluator.evaluateRule(345));
        Assertions.assertFalse(evaluator.evaluateRule(345));

        Thread.sleep(900);
        Assertions.assertFalse(evaluator.evaluateRule(345));
        Thread.sleep(100);
        Assertions.assertTrue(evaluator.evaluateRule(345));
        Assertions.assertTrue(evaluator.evaluateRule(345));
        Assertions.assertFalse(evaluator.evaluateRule(345));
    }
}
