package com.ranen.ranen;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SmsWebhookController {

    private static final Logger log = LoggerFactory.getLogger(SmsWebhookController.class);

    private final FarmerRepository farmers;

    public SmsWebhookController(FarmerRepository farmers) {
        this.farmers = farmers;
    }

    // Africa's Talking POSTs incoming SMS here: form fields include from, to, text, date, id.
    @PostMapping("/sms/incoming")
    public String incoming(@RequestParam String from,
                           @RequestParam(defaultValue = "") String text) {
        if (text.trim().equalsIgnoreCase("STOP")) {
            farmers.optOut(from);
            // Log only the last digits: phone numbers are personal data.
            log.info("Opt-out recorded for number ending {}", from.substring(Math.max(0, from.length() - 3)));
        }
        return "OK";
    }
}
