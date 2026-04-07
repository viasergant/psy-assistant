-- V35: Seed the standard SOAP-style session note template
--
-- Inserts the single system-defined structured template used by therapists.
-- Additional templates should be added via future migrations.

INSERT INTO note_template (id, name, description, template_fields)
VALUES (
    gen_random_uuid(),
    'Standard Session Note',
    'Standard clinical session note template covering presenting problem, interventions, client response, and plan.',
    '[
        {"key": "presentingProblem",   "label": "Presenting Problem",    "required": true},
        {"key": "interventionsUsed",   "label": "Interventions Used",    "required": true},
        {"key": "clientResponse",      "label": "Client Response",       "required": true},
        {"key": "planForNextSession",  "label": "Plan for Next Session", "required": true}
    ]'
);
