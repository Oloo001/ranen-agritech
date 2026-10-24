CREATE TABLE crop_plots (
    plot_id      SERIAL PRIMARY KEY,
    phone_number VARCHAR(15) NOT NULL,
    crop         VARCHAR(30) NOT NULL,
    planted_date DATE NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_plots_phone ON crop_plots (phone_number);

CREATE TABLE calendar_templates (
    template_id SERIAL PRIMARY KEY,
    crop        VARCHAR(30) NOT NULL,
    dap_start   INT NOT NULL,
    dap_end     INT NOT NULL,
    milestone   VARCHAR(50) NOT NULL,
    advice      TEXT NOT NULL
);

CREATE TABLE sms_log (
    id           SERIAL PRIMARY KEY,
    plot_id      INT NOT NULL,
    template_id  INT NOT NULL,
    sent_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    status       VARCHAR(10) NOT NULL DEFAULT 'pending',
    provider_ref TEXT,
    UNIQUE (plot_id, template_id)
);
CREATE INDEX idx_sms_log_status ON sms_log (status);

INSERT INTO calendar_templates (crop, dap_start, dap_end, milestone, advice) VALUES
('Cassava', 1, 14, 'Sprouting', 'Ensure cutting buds point upward. Watch for early weeds.'),
('Cassava', 30, 45, 'First Weeding', 'Clear weeds now to protect root growth. Check stems for whiteflies.'),
('Cassava', 60, 75, 'Second Weeding', 'Weed again before the canopy closes. Remove any plants with mosaic or streaked leaves.'),
('Cassava', 90, 120, 'Tuber Formation', 'Roots are forming. Top-dress with organic fertilizer or ash.'),
('Cassava', 270, 360, 'Harvest Window', 'Harvest on a dry day. Log harvest volume via USSD.'),
('Sweet potato', 1, 7, 'Establishment', 'Keep ridges high and soil damp so vines root securely.'),
('Sweet potato', 30, 30, 'Earthing Up', 'Pull loose soil up around the vines to widen the ridges.'),
('Sweet potato', 60, 75, 'Pest Inspection', 'Check vine bases for weevil holes. Remove yellowing leaves.'),
('Sweet potato', 120, 150, 'Maturity', 'Test-dig 2 hills. If mature, cut vines 7 days before harvest.');