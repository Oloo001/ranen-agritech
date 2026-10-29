package com.ranen.ranen.channels;

import com.ranen.ranen.common.PhoneNumbers;
import com.ranen.ranen.identity.FarmerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
public class SmsWebhookController {

    private static final Logger log = LoggerFactory.getLogger(SmsWebhookController.class);

    private final FarmerRepository farmers;
    private final String expectedToken;

    public SmsWebhookController(FarmerRepository farmers,
                                @Value("${africastalking.webhook-token:}") String expectedToken) {
        this.farmers = farmers;
        this.expectedToken = expectedToken;
    }

    @PostMapping("/sms/incoming")
    public ResponseEntity<String> incoming(@RequestParam String from,
                                           @RequestParam(defaultValue = "") String text,
                                           @RequestParam(defaultValue = "") String token) {
        if (!tokenIsValid(token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Forbidden");
        }

        if (text.trim().equalsIgnoreCase("STOP")) {
            PhoneNumbers.normalize(from).ifPresent(phone -> {
                farmers.optOut(phone);
                log.info("Opt-out recorded for number ending {}", phone.substring(phone.length() - 3));
            });
        }
        return ResponseEntity.ok("OK");
    }

    // No token configured means the endpoint is closed to everyone. Fail safe, not open.
    // MessageDigest.isEqual compares in constant time, so response timing can't leak the token.
    private boolean tokenIsValid(String given) {
        if (expectedToken.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(
                expectedToken.getBytes(StandardCharsets.UTF_8),
                given.getBytes(StandardCharsets.UTF_8));
    }
}