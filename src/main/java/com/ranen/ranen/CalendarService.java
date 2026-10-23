package com.ranen.ranen;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CalendarService {

    private static final RowMapper<Milestone> TO_MILESTONE = (rs, rowNum) -> new Milestone(
            rs.getString("milestone"),
            rs.getString("advice"),
            rs.getInt("dap_start"),
            rs.getInt("dap_end"));

    private final JdbcClient jdbc;

    public CalendarService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    // The milestone whose window contains today, if any.
    public Optional<Milestone> currentMilestone(String crop, long dap) {
        return jdbc.sql("""
                SELECT milestone, advice, dap_start, dap_end
                FROM calendar_templates
                WHERE crop = :crop AND :dap BETWEEN dap_start AND dap_end
                LIMIT 1
                """)
                .param("crop", crop)
                .param("dap", (int) dap)
                .query(TO_MILESTONE)
                .optional();
    }

    // The soonest milestone that hasn't started yet, if any.
    public Optional<Milestone> nextMilestone(String crop, long dap) {
        return jdbc.sql("""
                SELECT milestone, advice, dap_start, dap_end
                FROM calendar_templates
                WHERE crop = :crop AND dap_start > :dap
                ORDER BY dap_start
                LIMIT 1
                """)
                .param("crop", crop)
                .param("dap", (int) dap)
                .query(TO_MILESTONE)
                .optional();
    }
}
