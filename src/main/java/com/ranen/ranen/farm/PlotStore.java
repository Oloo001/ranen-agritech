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

    public void addPlot(String phone, String crop, LocalDate plantedDate) {
        jdbc.sql("""
                INSERT INTO crop_plots (phone_number, crop, planted_date)
                VALUES (:phone, :crop, :planted)
                """)
                .param("phone", phone)
                .param("crop", crop)
                .param("planted", plantedDate)
                .update();
    }

    // Only active plots: harvested ones drop out of the farmer's menus.
    public List<CropPlot> getPlots(String phone) {
        return jdbc.sql("""
                SELECT plot_id, crop, planted_date
                FROM crop_plots
                WHERE phone_number = :phone AND status = 'active'
                ORDER BY planted_date DESC, plot_id DESC
                """)
                .param("phone", phone)
                .query((rs, rowNum) -> new CropPlot(
                        rs.getInt("plot_id"),
                        rs.getString("crop"),
                        rs.getObject("planted_date", LocalDate.class)))
                .list();
    }

    // One statement does both jobs: mark the plot harvested AND record the harvest.
    // If the plot isn't this farmer's, or was already harvested, nothing happens (returns false).
    public boolean recordHarvest(int plotId, String phone, int quantityKg) {
        int rows = jdbc.sql("""
                WITH done AS (
                    UPDATE crop_plots SET status = 'harvested'
                    WHERE plot_id = :plot AND phone_number = :phone AND status = 'active'
                    RETURNING plot_id)
                INSERT INTO harvests (plot_id, quantity_kg)
                SELECT plot_id, :kg FROM done
                """)
                .param("plot", plotId)
                .param("phone", phone)
                .param("kg", quantityKg)
                .update();
        return rows == 1;
    }
}
