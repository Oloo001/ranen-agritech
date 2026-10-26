package com.ranen.ranen.notifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class AdviceJob {

    private static final Logger log = LoggerFactory.getLogger(AdviceJob.class);

    private final AdviceRepository repository;
    private final SmsSender smsSender;

    public AdviceJob(AdviceRepository repository, SmsSender smsSender) {
        this.repository = repository;
        this.smsSender = smsSender;
    }

    // 07:00 every day, Nairobi time. Spring cron format: second minute hour day month weekday.
    @Scheduled(cron = "0 0 7 * * *", zone = "Africa/Nairobi")
    public void scheduledRun() {
        run();
    }

    public int run() {
        int sent = 0;
        for (DueAdvice advice : repository.findDue()) {
            if (!repository.claim(advice.plotId(), advice.templateId())) {
                continue;
            }

            String ref;
            try {
                ref = smsSender.send(advice.phone(), advice.message());
            } catch (Exception e) {
                //The send failed, so nothing reached the farmer: safe to retry.
                log.error("Failed to send advice for plot {}", advice.plotId(), e);
                repository.release(advice.plotId(), advice.templateId());
                continue;
            }

            // The SMS is already out. Never release the claim here, or we would send it twice.
            try {
                repository.markSent(advice.plotId(), advice.templateId(), ref);
            } catch (Exception e) {
                log.error("SMS sent for plot {} but recording it failed", advice.plotId(), e);
            }
            sent++;
        }
        log.info("Advice job finished: {} message(s) sent", sent);
        return sent;
    }

    // Every 10 minutes, retry messages stuck 'pending' for over 15 minutes.
    @Scheduled(fixedDelay = 600_000, initialDelay = 60_000)
    public int recoverStale() {
        int n = repository.requeueStale(15);
        if (n > 0) {
            log.warn("Requeued {} message(s) stuck in 'pending' (possible crash)", n);
        }
        return n;
    }
}
