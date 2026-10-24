package com.ranen.ranen;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdviceRepository {

    private final JdbcClient jdbc;

    public AdviceRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    // Plots whose milestone window is open today and haven't been messaged yet.
    public List<DueAdvice> findDue() {
        return jdbc.sql("""
            SELECT p.plot_id, t.template_id, p.phone_number, p.crop, t.milestone, t.advice
            FROM crop_plots p
            JOIN calendar_templates t ON t.crop = p.crop
            LEFT JOIN farmers f ON f.phone_number = p.phone_number
            WHERE ((now() AT TIME ZONE 'Africa/Nairobi')::date - p.planted_date)
                      BETWEEN t.dap_start AND t.dap_end
              AND COALESCE(f.sms_opt_out, FALSE) = FALSE
              AND NOT EXISTS (
                  SELECT 1 FROM sms_log s
                  WHERE s.plot_id = p.plot_id AND s.template_id = t.template_id)
             """)
                .query((rs, rowNum) -> new DueAdvice(
                        rs.getInt("plot_id"),
                        rs.getInt("template_id"),
                        rs.getString("phone_number"),
                        rs.getString("crop"),
                        rs.getString("milestone"),
                        rs.getString("advice")))
                .list();
    }

    // Try to record "I am sending this". Returns true only for the caller that wins.
    public boolean claim(int plotId, int templateId) {
        int rows = jdbc.sql("""
                INSERT INTO sms_log (plot_id, template_id, status)
                VALUES (:plot, :template, 'pending')
                ON CONFLICT (plot_id, template_id) DO NOTHING
                """)
                .param("plot", plotId)
                .param("template", templateId)
                .update();
        return rows == 1;
    }

    // Confirm the send and keep the provider's receipt.
    public void markSent(int plotId, int templateId, String providerRef) {
        jdbc.sql("""
                UPDATE sms_log SET status = 'sent', provider_ref = :ref
                WHERE plot_id = :plot AND template_id = :template
                """)
                .param("ref", providerRef)
                .param("plot", plotId)
                .param("template", templateId)
                .update();
    }

    // Crash recovery: forget claims that have been 'pending' too long so they can be retried.
    public int requeueStale(int olderThanMinutes) {
        return jdbc.sql("""
                DELETE FROM sms_log
                WHERE status = 'pending'
                  AND sent_at < now() - (:minutes * interval '1 minute')
                """)
                .param("minutes", olderThanMinutes)
                .update();
    }

    // Undo a claim if sending failed, so tomorrow's run can retry.
    public void release(int plotId, int templateId) {
        jdbc.sql("DELETE FROM sms_log WHERE plot_id = :plot AND template_id = :template")
                .param("plot", plotId)
                .param("template", templateId)
                .update();
    }
}
