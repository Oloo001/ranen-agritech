package com.ranen.ranen;

public interface SmsSender {
    /** Sends the message and returns the provider's reference (maybe null). */
    String send(String phone, String message);
}
