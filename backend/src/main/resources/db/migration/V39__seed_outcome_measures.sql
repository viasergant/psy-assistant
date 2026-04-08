-- V39: Seed standard outcome measure definitions (PA-44)

INSERT INTO outcome_measure_definition
    (code, display_name, description, min_score, max_score, alert_threshold, alert_label, alert_severity, sort_order)
VALUES
    ('PHQ9',      'PHQ-9 (Patient Health Questionnaire-9)',
     'Measures depression severity',                        0,  27, 15, 'Severe Depression Risk',   'ALERT',   1),
    ('GAD7',      'GAD-7 (Generalized Anxiety Disorder-7)',
     'Measures anxiety severity',                           0,  21, 15, 'Severe Anxiety',           'ALERT',   2),
    ('DASS21_DEP','DASS-21: Depression Subscale',
     'Part of DASS-21 battery',                             0,  42, 28, 'Extremely Severe Depression','ALERT', 3),
    ('DASS21_ANX','DASS-21: Anxiety Subscale',
     'Part of DASS-21 battery',                             0,  42, 20, 'Extremely Severe Anxiety', 'ALERT',   4),
    ('DASS21_STR','DASS-21: Stress Subscale',
     'Part of DASS-21 battery',                             0,  42, 34, 'Extremely Severe Stress',  'WARNING', 5),
    ('WHODAS',    'WHODAS 2.0',
     'WHO Disability Assessment Schedule',                  0, 100, 75, 'Severe Disability',        'ALERT',   6),
    ('PCL5',      'PCL-5 (PTSD Checklist for DSM-5)',
     'Screens for PTSD',                                    0,  80, 51, 'Probable PTSD',            'ALERT',   7)
ON CONFLICT (code) DO NOTHING;
