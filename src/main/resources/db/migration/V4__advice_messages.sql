CREATE TABLE advice_messages (
    template_id INT NOT NULL REFERENCES calendar_templates(template_id),
    language    VARCHAR(5) NOT NULL CHECK (language IN ('en', 'sw', 'luo')),
    message     TEXT NOT NULL,
    reviewed    BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (template_id, language)
);

-- English is the source text, so it counts as reviewed.
INSERT INTO advice_messages (template_id, language, message, reviewed)
SELECT template_id, 'en',
       'Hello, ' || crop || ' - ' || milestone || ': ' || advice,
       TRUE
FROM calendar_templates;

-- Carry over the Swahili draft as a COMPLETE message. Still a draft, still unreviewed.
-- "Muhogo" = cassava, "Palizi ya kwanza" = first weeding: a Swahili speaker must confirm.
INSERT INTO advice_messages (template_id, language, message, reviewed)
SELECT template_id, language, 'Habari, Muhogo - Palizi ya kwanza: ' || advice, FALSE
FROM advice_translations
WHERE language = 'sw';

DROP TABLE advice_translations;