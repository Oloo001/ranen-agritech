package com.ranen.ranen.notifications;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "sms.provider", havingValue = "console", matchIfMissing = true)
public class ConsoleSmsSender implements SmsSender {

    @Override
    public String send(String phone, String message) {
        System.out.println("[FAKE SMS] to " + phone + ": " + message);
        return "console";
    }
}
