CREATE TABLE farmers (
    farmer_id    SERIAL PRIMARY KEY,
    phone_number VARCHAR(15) NOT NULL UNIQUE,
    name         VARCHAR(100),
    language     VARCHAR(5) NOT NULL DEFAULT 'en' CHECK (language IN ('en', 'sw', 'luo')),
    sms_opt_out  BOOLEAN NOT NULL DEFAULT FALSE,
    consent_at   TIMESTAMPTZ,
    opted_out_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Existing plots belong to farmers too. consent_at stays NULL for them: consent unknown.
INSERT INTO farmers (phone_number)
SELECT DISTINCT phone_number FROM crop_plots
ON CONFLICT (phone_number) DO NOTHING;