CREATE TABLE advice_translations (
    template_id INT NOT NULL REFERENCES calendar_templates(template_id),
    language    VARCHAR(5) NOT NULL CHECK (language IN ('sw', 'luo')),
    advice      TEXT NOT NULL,
    reviewed    BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (template_id, language)
);

-- DRAFT written by an AI assistant, NOT reviewed. Must be checked by a Swahili-speaking
-- extension officer before reviewed is set to TRUE.
INSERT INTO advice_translations (template_id, language, advice)
SELECT template_id, 'sw',
       'Ondoa magugu sasa ili kulinda ukuaji wa mizizi. Angalia mashina kama kuna nzi weupe.'
FROM calendar_templates
WHERE crop = 'Cassava' AND milestone = 'First Weeding';