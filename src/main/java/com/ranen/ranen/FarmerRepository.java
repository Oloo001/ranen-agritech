package com.ranen.ranen;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

@Service
public class FarmerRepository {

    private final JdbcClient jdbc;

    public FarmerRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    // Called when a farmer registers a plot over USSD. Never undoes an opt-out.
    public void registerConsent(String phone) {
        jdbc.sql("""
                INSERT INTO farmers (phone_number, consent_at) VALUES (:phone, now())
                ON CONFLICT (phone_number)
                DO UPDATE SET consent_at = COALESCE(farmers.consent_at, now())
                """)
                .param("phone", phone)
                .update();
    }

    // Works even if we've never seen this number: a STOP always sticks.
    public void optOut(String phone) {
        jdbc.sql("""
                INSERT INTO farmers (phone_number, sms_opt_out, opted_out_at)
                VALUES (:phone, TRUE, now())
                ON CONFLICT (phone_number)
                DO UPDATE SET sms_opt_out = TRUE, opted_out_at = now()
                """)
                .param("phone", phone)
                .update();
    }
}
