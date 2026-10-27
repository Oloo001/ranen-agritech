ALTER TABLE crop_plots
    ADD COLUMN status VARCHAR(10) NOT NULL DEFAULT 'active'
    CHECK (status IN ('active', 'harvested', 'failed'));

CREATE TABLE harvests (
    harvest_id   SERIAL PRIMARY KEY,
    plot_id      INT NOT NULL REFERENCES crop_plots(plot_id),
    quantity_kg  INT NOT NULL CHECK (quantity_kg > 0),
    harvested_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_harvests_plot ON harvests (plot_id);