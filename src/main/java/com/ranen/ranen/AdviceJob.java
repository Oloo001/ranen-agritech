package com.ranen.ranen;

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
            // Only the caller that records the claim may send.
            if (!repository.claim(advice.plotId(), advice.templateId())) {
                continue;
            }
            try {
                String message = "Habari, " + advice.crop() + " - " + advice.milestone()
                        + ": " + advice.advice();
                smsSender.send(advice.phone(), message);
                sent++;
            } catch (Exception e) {
                log.error("Failed to send advice for plot {}", advice.plotId(), e);
                repository.release(advice.plotId(), advice.templateId());
            }
        }
        log.info("Advice job finished: {} message(s) sent", sent);
        return sent;
    }
}
