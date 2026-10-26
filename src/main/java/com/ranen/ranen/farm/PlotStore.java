package com.ranen.ranen.farm;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class PlotStore {

    private final JdbcClient jdbc;

    public PlotStore(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void addPlot(String phone, CropPlot plot) {
        jdbc.sql("""
                INSERT INTO crop_plots (phone_number, crop, planted_date)
                VALUES (:phone, :crop, :planted)
                """)
                .param("phone", phone)
                .param("crop", plot.crop())
                .param("planted", plot.plantedDate())
                .update();
    }

    public List<CropPlot> getPlots(String phone) {
        return jdbc.sql("""
                SELECT crop, planted_date
                FROM crop_plots
                WHERE phone_number = :phone
                ORDER BY planted_date DESC, plot_id DESC
                """)
                .param("phone", phone)
                .query((rs, rowNum) -> new CropPlot(
                        rs.getString("crop"),
                        rs.getObject("planted_date", LocalDate.class)))
                .list();
    }
}
