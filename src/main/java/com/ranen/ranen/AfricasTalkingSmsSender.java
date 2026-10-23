package com.ranen.ranen;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import java.time.Duration;

import java.util.regex.Pattern;

@Service
@ConditionalOnProperty(name = "sms.provider", havingValue = "africastalking")
public class AfricasTalkingSmsSender implements SmsSender {

    private static final Pattern SUCCESS = Pattern.compile("\"status\"\\s*:\\s*\"Success\"");

    private final RestClient client;
    private final String username;

    public AfricasTalkingSmsSender(@Value("${africastalking.base-url}") String baseUrl,
                                   @Value("${africastalking.username}") String username,
                                   @Value("${africastalking.api-key}") String apiKey) {
        this.username = username;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));

        this.client = RestClient.builder()
                .requestFactory(factory)
                .baseUrl(baseUrl)
                .defaultHeader("apiKey", apiKey)
                .defaultHeader("Accept", "application/json")
                .build();
    }

    @Override
    public void send(String phone, String message) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("username", username);
        form.add("to", phone);
        form.add("message", message);

        // retrieve() throws on 4xx/5xx, which AdviceJob catches and treats as a failed send.
        String body = client.post()
                .uri("/messaging")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(String.class);

        // A 200 response can still contain a per-recipient failure, so check the status text too.
        if (body == null || !SUCCESS.matcher(body).find()) {
            throw new IllegalStateException("SMS not accepted: " + body);
        }
        System.out.println("[AT SANDBOX] " + body);
    }
}
