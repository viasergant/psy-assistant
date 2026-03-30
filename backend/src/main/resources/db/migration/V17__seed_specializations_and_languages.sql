-- V17: Seed specializations and languages reference data.
-- Provides common specializations and languages for therapist profiles.

-- Seed specializations (common therapeutic specializations)
INSERT INTO specialization (id, name, description) VALUES
    (gen_random_uuid(), 'Anxiety Disorders', 'Treatment of anxiety, panic attacks, and related conditions'),
    (gen_random_uuid(), 'Depression', 'Treatment of depression and mood disorders'),
    (gen_random_uuid(), 'Trauma and PTSD', 'Treatment of trauma, post-traumatic stress disorder'),
    (gen_random_uuid(), 'Relationship Issues', 'Couples therapy and relationship counseling'),
    (gen_random_uuid(), 'Family Therapy', 'Family counseling and family systems therapy'),
    (gen_random_uuid(), 'Child and Adolescent Therapy', 'Specialized therapy for children and teenagers'),
    (gen_random_uuid(), 'Substance Abuse', 'Treatment of addiction and substance use disorders'),
    (gen_random_uuid(), 'Eating Disorders', 'Treatment of anorexia, bulimia, and binge eating'),
    (gen_random_uuid(), 'OCD', 'Treatment of obsessive-compulsive disorder'),
    (gen_random_uuid(), 'ADHD', 'Treatment and support for attention-deficit/hyperactivity disorder'),
    (gen_random_uuid(), 'Grief and Loss', 'Support for bereavement and loss'),
    (gen_random_uuid(), 'Stress Management', 'Stress reduction and coping strategies'),
    (gen_random_uuid(), 'Self-Esteem', 'Building confidence and self-worth'),
    (gen_random_uuid(), 'Career Counseling', 'Career guidance and work-life balance'),
    (gen_random_uuid(), 'Life Transitions', 'Support during major life changes')
ON CONFLICT (name) DO NOTHING;

-- Seed languages (common languages for therapy)
INSERT INTO language (id, name, language_code) VALUES
    (gen_random_uuid(), 'English', 'en'),
    (gen_random_uuid(), 'Ukrainian', 'uk'),
    (gen_random_uuid(), 'Russian', 'ru'),
    (gen_random_uuid(), 'Spanish', 'es'),
    (gen_random_uuid(), 'French', 'fr'),
    (gen_random_uuid(), 'German', 'de'),
    (gen_random_uuid(), 'Italian', 'it'),
    (gen_random_uuid(), 'Polish', 'pl'),
    (gen_random_uuid(), 'Portuguese', 'pt'),
    (gen_random_uuid(), 'Chinese (Mandarin)', 'zh')
ON CONFLICT (name) DO NOTHING;
