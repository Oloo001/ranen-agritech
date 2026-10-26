package com.ranen.ranen.notifications;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class AdviceJobConcurrencyTest {

    // A fake sender that counts how many times each phone was messaged.
    static class CountingSmsSender implements SmsSender {
        private final ConcurrentHashMap<String, AtomicInteger> perPhone = new ConcurrentHashMap<>();

        @Override
        public String send(String phone, String message) {
            perPhone.computeIfAbsent(phone, p -> new AtomicInteger()).incrementAndGet();
            return "test";
        }

        int total() {
            return perPhone.values().stream().mapToInt(AtomicInteger::get).sum();
        }

        int maxPerPhone() {
            return perPhone.values().stream().mapToInt(AtomicInteger::get).max().orElse(0);
        }

        void reset() {
            perPhone.clear();
        }
    }

    @TestConfiguration
    static class Config {
        @Bean
        CountingSmsSender countingSmsSender() {
            return new CountingSmsSender();
        }
    }

    @Autowired
    AdviceJob job;
    @Autowired JdbcClient jdbc;
    @Autowired CountingSmsSender sender;

    @BeforeEach
    void setUp() {
        sender.reset();
        jdbc.sql("DELETE FROM sms_log").update();
        jdbc.sql("DELETE FROM crop_plots").update();
    }

    @Test
    void eachMessageIsSentExactlyOnce_evenWhenJobsRunAtTheSameTime() throws Exception {
        int plots = 50;
        int threads = 8;

        // 50 cassava plots, all on day 35, so all are inside the First Weeding window.
        for (int i = 0; i < plots; i++) {
            jdbc.sql("INSERT INTO crop_plots (phone_number, crop, planted_date) "
                            + "VALUES (:phone, 'Cassava', CURRENT_DATE - 35)")
                    .param("phone", "+254700000" + String.format("%03d", i))
                    .update();
        }

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGun = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            results.add(pool.submit(() -> {
                startGun.await();      // every thread waits here...
                return job.run();      // ...then they all run at the same moment
            }));
        }
        startGun.countDown();          // fire!

        int reportedTotal = 0;
        for (Future<Integer> f : results) {
            reportedTotal += f.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertEquals(plots, sender.total(), "total SMS actually sent");
        assertEquals(1, sender.maxPerPhone(), "no phone should get more than one SMS");
        assertEquals(plots, reportedTotal, "total the job runs reported");
    }
}