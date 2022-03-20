import com.learn.ratelimiter.api.RateLimitService;
import com.learn.ratelimiter.impl.RateLimiter;
import com.learn.ratelimiter.rules.CustomerIdLimitEvaluator;
import com.learn.ratelimiter.rules.CustomerIdLimitSlidingWindowEvaluator;
import com.learn.ratelimiter.rules.RateLimitRule;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public class RateLimitServiceTest {
    @AfterEach
    public void removeRules() {
        CustomerIdLimitEvaluator.deReferenceInstance();
        CustomerIdLimitSlidingWindowEvaluator.deReferenceInstance();
    }

    @Test
    public void testSingleRequest() {
        RateLimitRule rule = RateLimitRule.builder().
                limitBy(RateLimitRule.Aspect.CUSTOMER_ID).threshold(10).windowInSeconds(1).build();
        RateLimitService svc = new RateLimiter();
        svc.addRateLimitRule(rule);
        Assertions.assertTrue(svc.rateLimit(123));
    }

    @Test
    public void testRequestsExceedThreshold() {
        RateLimitRule rule = RateLimitRule.builder().
                limitBy(RateLimitRule.Aspect.CUSTOMER_ID).threshold(10).windowInSeconds(1).build();
        RateLimitService svc = new RateLimiter();
        svc.addRateLimitRule(rule);
        for (int i = 0; i < 12; i++) {
            if (i < 10)
                Assertions.assertTrue(svc.rateLimit(456));
            else
                Assertions.assertFalse(svc.rateLimit(456));
        }
    }

    @Test
    public void testRequestsExceedThresholdInNextSecond() throws InterruptedException {
        RateLimitRule rule = RateLimitRule.builder().
                limitBy(RateLimitRule.Aspect.CUSTOMER_ID).threshold(10).windowInSeconds(1).build();
        RateLimitService svc = new RateLimiter();
        svc.addRateLimitRule(rule);
        for (int i = 0; i < 12; i++) {
            if (i < 10)
                Assertions.assertTrue(svc.rateLimit(789));
            else
                Assertions.assertFalse(svc.rateLimit(789));
        }

        Thread.sleep(1000);

        for (int i = 0; i < 10; i++) {
            Assertions.assertTrue(svc.rateLimit(789));
        }
    }

    @Test
    public void testRequestsExceedThresholdLongerWindow() {
        RateLimitRule rule = RateLimitRule.builder().
                limitBy(RateLimitRule.Aspect.CUSTOMER_ID).threshold(10).windowInSeconds(5).build();
        RateLimitService svc = new RateLimiter();
        svc.addRateLimitRule(rule);
        for (int i = 0; i < 15; i++) {
            if (i < 10)
                Assertions.assertTrue(svc.rateLimit(132));
            else
                Assertions.assertFalse(svc.rateLimit(132));
        }
    }

    @Test
    public void testRequestsExceedThresholdInNextWindow() throws InterruptedException {
        RateLimitRule rule = RateLimitRule.builder().
                limitBy(RateLimitRule.Aspect.CUSTOMER_ID).threshold(10).windowInSeconds(2).build();
        RateLimitService svc = new RateLimiter();
        svc.addRateLimitRule(rule);
        for (int i = 0; i < 15; i++) {
            if (i < 10)
                Assertions.assertTrue(svc.rateLimit(234));
            else
                Assertions.assertFalse(svc.rateLimit(234));
        }

        Thread.sleep(2000);

        for (int i = 0; i < 10; i++) {
            Assertions.assertTrue(svc.rateLimit(234));
        }
    }

    @Test
    public void testConcurrentRequestsExceedThreshold() throws InterruptedException {
        RateLimitRule rule = RateLimitRule.builder().
                limitBy(RateLimitRule.Aspect.CUSTOMER_ID).threshold(500).windowInSeconds(5).build();
        RateLimitService svc = new RateLimiter();
        svc.addRateLimitRule(rule);
        ExecutorService exec = Executors.newFixedThreadPool(4);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        Runnable task = () -> {
            for (int i = 0; i < 150; i++) {
                if (svc.rateLimit(567)) {
                    successCount.incrementAndGet();
                } else {
                    failCount.incrementAndGet();
                }
            }
        };
        //System.out.println("Start at "+System.currentTimeMillis()/1000);
        Future f1 = exec.submit(task);
        Thread.sleep(10);
        Future f2 = exec.submit(task);
        Thread.sleep(10);
        Future f3 = exec.submit(task);
        Future f4 = exec.submit(task);
        while (!(f1.isDone() && f2.isDone() && f3.isDone() && f4.isDone()));
        //System.out.println("End at "+System.currentTimeMillis()/1000);

        Assertions.assertEquals(500, successCount.get());
        Assertions.assertEquals(100, failCount.get());
    }

    @Test
    public void testSlidingWindowBasic() throws InterruptedException {
        RateLimitService svc = new RateLimiter();
        svc.addRateLimitRule(RateLimitRule.builder().
                        limitBy(RateLimitRule.Aspect.CUSTOMER_ID_SLIDING_WINDOW).threshold(2).windowInSeconds(1).build()
        );
        Assertions.assertTrue(svc.rateLimit(113));
        Assertions.assertTrue(svc.rateLimit(113));
        Assertions.assertFalse(svc.rateLimit(113));
    }

    @Test
    public void testSlidingWindowAtEndOfWindow() throws InterruptedException {
        RateLimitService svc = new RateLimiter();
        svc.addRateLimitRule(
                RateLimitRule.builder().
                        limitBy(RateLimitRule.Aspect.CUSTOMER_ID_SLIDING_WINDOW).threshold(2).windowInSeconds(1).build()
        );
        Assertions.assertTrue(svc.rateLimit(224));
        Assertions.assertTrue(svc.rateLimit(224));
        Assertions.assertFalse(svc.rateLimit(224));

        Thread.sleep(900);
        Assertions.assertFalse(svc.rateLimit(224));
    }

    @Test
    public void testSlidingWindowInNextWindow() throws InterruptedException {
        RateLimitService svc = new RateLimiter();
        svc.addRateLimitRule(
                RateLimitRule.builder().
                        limitBy(RateLimitRule.Aspect.CUSTOMER_ID_SLIDING_WINDOW).threshold(50).windowInSeconds(1).build()
        );
        for (int i = 0; i < 52; i++) {
            Thread.sleep(1);
            if (i < 50) Assertions.assertTrue(svc.rateLimit(335));
            else Assertions.assertFalse(svc.rateLimit(335));
        }
        //sleep for 990m and submit new request. Hopefully we would discard/exclude few requests that came during window
        //start i.e., (lastSuccessTime+990ms) - 1000. So 1 request should be served.
        System.out.println("Sleeping for 990 ms at "+System.currentTimeMillis());
        Thread.sleep(970);
        Assertions.assertTrue(svc.rateLimit(335));
        Thread.sleep(30);

        for (int i = 0; i < 20; i++) {
            Assertions.assertTrue(svc.rateLimit(335));
        }
    }

    @Test
    public void testSlidingWindowConcurrentRequestsExceedThreshold() throws InterruptedException {
        RateLimitRule rule = RateLimitRule.builder().
                limitBy(RateLimitRule.Aspect.CUSTOMER_ID_SLIDING_WINDOW).threshold(500).windowInSeconds(5).build();
        RateLimitService svc = new RateLimiter();
        svc.addRateLimitRule(rule);
        ExecutorService exec = Executors.newFixedThreadPool(4);
        AtomicInteger sucCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);
        Runnable task = () -> {
            for (int i = 0; i < 150; i++) {
                if (svc.rateLimit(557)) {
                    sucCount.incrementAndGet();
                } else {
                    failedCount.incrementAndGet();
                }
            }
        };
        Future f1 = exec.submit(task);
        //Thread.sleep(10);
        Future f2 = exec.submit(task);
        Thread.sleep(10);
        Future f3 = exec.submit(task);
        Future f4 = exec.submit(task);
        while (!(f1.isDone() && f2.isDone() && f3.isDone() && f4.isDone()));

        Assertions.assertEquals(500, sucCount.get());
        Assertions.assertEquals(100, failedCount.get());
    }
}
