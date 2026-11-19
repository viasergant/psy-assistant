-- V78: Add org_id FK to all remaining org-scoped domain tables.
-- Pattern per table: (1) add nullable column, (2) backfill, (3) NOT NULL, (4) index.
-- Also replaces global unique constraints with org-scoped composites where required.

-- =============================================================================
-- therapist_profile
-- =============================================================================
ALTER TABLE therapist_profile ADD COLUMN org_id UUID REFERENCES organizations(id);
UPDATE therapist_profile SET org_id = '00000000-0000-0000-0000-000000000001' WHERE org_id IS NULL;
ALTER TABLE therapist_profile ALTER COLUMN org_id SET NOT NULL;
CREATE INDEX idx_therapist_profile_org_id ON therapist_profile(org_id);

-- Replace global UNIQUE(email) with org-scoped composite
ALTER TABLE therapist_profile DROP CONSTRAINT IF EXISTS therapist_profile_email_key;
ALTER TABLE therapist_profile ADD CONSTRAINT uq_tp_org_email UNIQUE (org_id, email);

-- =============================================================================
-- leads
-- =============================================================================
ALTER TABLE leads ADD COLUMN org_id UUID REFERENCES organizations(id);
UPDATE leads SET org_id = '00000000-0000-0000-0000-000000000001' WHERE org_id IS NULL;
ALTER TABLE leads ALTER COLUMN org_id SET NOT NULL;
CREATE INDEX idx_leads_org_id ON leads(org_id);

-- =============================================================================
-- clients
-- =============================================================================
ALTER TABLE clients ADD COLUMN org_id UUID REFERENCES organizations(id);
UPDATE clients SET org_id = '00000000-0000-0000-0000-000000000001' WHERE org_id IS NULL;
ALTER TABLE clients ALTER COLUMN org_id SET NOT NULL;
CREATE INDEX idx_clients_org_id ON clients(org_id);

-- =============================================================================
-- appointment
-- =============================================================================
ALTER TABLE appointment ADD COLUMN org_id UUID REFERENCES organizations(id);
UPDATE appointment SET org_id = '00000000-0000-0000-0000-000000000001' WHERE org_id IS NULL;
ALTER TABLE appointment ALTER COLUMN org_id SET NOT NULL;
CREATE INDEX idx_appointment_org_id ON appointment(org_id);

-- =============================================================================
-- appointment_series
-- =============================================================================
ALTER TABLE appointment_series ADD COLUMN org_id UUID REFERENCES organizations(id);
UPDATE appointment_series SET org_id = '00000000-0000-0000-0000-000000000001' WHERE org_id IS NULL;
ALTER TABLE appointment_series ALTER COLUMN org_id SET NOT NULL;
CREATE INDEX idx_appointment_series_org_id ON appointment_series(org_id);

-- =============================================================================
-- session_record
-- =============================================================================
ALTER TABLE session_record ADD COLUMN org_id UUID REFERENCES organizations(id);
UPDATE session_record SET org_id = '00000000-0000-0000-0000-000000000001' WHERE org_id IS NULL;
ALTER TABLE session_record ALTER COLUMN org_id SET NOT NULL;
CREATE INDEX idx_session_record_org_id ON session_record(org_id);

-- =============================================================================
-- session_note
-- =============================================================================
ALTER TABLE session_note ADD COLUMN org_id UUID REFERENCES organizations(id);
UPDATE session_note SET org_id = '00000000-0000-0000-0000-000000000001' WHERE org_id IS NULL;
ALTER TABLE session_note ALTER COLUMN org_id SET NOT NULL;
CREATE INDEX idx_session_note_org_id ON session_note(org_id);

-- =============================================================================
-- care_plan
-- =============================================================================
ALTER TABLE care_plan ADD COLUMN org_id UUID REFERENCES organizations(id);
UPDATE care_plan SET org_id = '00000000-0000-0000-0000-000000000001' WHERE org_id IS NULL;
ALTER TABLE care_plan ALTER COLUMN org_id SET NOT NULL;
CREATE INDEX idx_care_plan_org_id ON care_plan(org_id);

-- =============================================================================
-- invoices
-- =============================================================================
ALTER TABLE invoices ADD COLUMN org_id UUID REFERENCES organizations(id);
UPDATE invoices SET org_id = '00000000-0000-0000-0000-000000000001' WHERE org_id IS NULL;
ALTER TABLE invoices ALTER COLUMN org_id SET NOT NULL;
CREATE INDEX idx_invoices_org_id ON invoices(org_id);

-- =============================================================================
-- prepaid_package_instance
-- =============================================================================
ALTER TABLE prepaid_package_instance ADD COLUMN org_id UUID REFERENCES organizations(id);
UPDATE prepaid_package_instance SET org_id = '00000000-0000-0000-0000-000000000001' WHERE org_id IS NULL;
ALTER TABLE prepaid_package_instance ALTER COLUMN org_id SET NOT NULL;
CREATE INDEX idx_prepaid_pkg_inst_org_id ON prepaid_package_instance(org_id);

-- =============================================================================
-- prepaid_package_definition
-- =============================================================================
ALTER TABLE prepaid_package_definition ADD COLUMN org_id UUID REFERENCES organizations(id);
UPDATE prepaid_package_definition SET org_id = '00000000-0000-0000-0000-000000000001' WHERE org_id IS NULL;
ALTER TABLE prepaid_package_definition ALTER COLUMN org_id SET NOT NULL;
CREATE INDEX idx_prepaid_pkg_def_org_id ON prepaid_package_definition(org_id);

-- Replace global UNIQUE(name) with org-scoped composite
ALTER TABLE prepaid_package_definition DROP CONSTRAINT IF EXISTS prepaid_package_definition_name_key;
ALTER TABLE prepaid_package_definition ADD CONSTRAINT uq_pkg_def_org_name UNIQUE (org_id, name);

-- =============================================================================
-- service_catalog
-- =============================================================================
ALTER TABLE service_catalog ADD COLUMN org_id UUID REFERENCES organizations(id);
UPDATE service_catalog SET org_id = '00000000-0000-0000-0000-000000000001' WHERE org_id IS NULL;
ALTER TABLE service_catalog ALTER COLUMN org_id SET NOT NULL;
CREATE INDEX idx_service_catalog_org_id ON service_catalog(org_id);

-- Replace global UNIQUE(name, category) with org-scoped composite
ALTER TABLE service_catalog DROP CONSTRAINT IF EXISTS uq_catalog_name_category;
ALTER TABLE service_catalog ADD CONSTRAINT uq_catalog_org_name_cat UNIQUE (org_id, name, category);

-- =============================================================================
-- service_catalog_price_history
-- =============================================================================
ALTER TABLE service_catalog_price_history ADD COLUMN org_id UUID REFERENCES organizations(id);
UPDATE service_catalog_price_history SET org_id = '00000000-0000-0000-0000-000000000001' WHERE org_id IS NULL;
ALTER TABLE service_catalog_price_history ALTER COLUMN org_id SET NOT NULL;
CREATE INDEX idx_svc_price_hist_org_id ON service_catalog_price_history(org_id);

-- =============================================================================
-- service_catalog_therapist_override
-- =============================================================================
ALTER TABLE service_catalog_therapist_override ADD COLUMN org_id UUID REFERENCES organizations(id);
UPDATE service_catalog_therapist_override SET org_id = '00000000-0000-0000-0000-000000000001' WHERE org_id IS NULL;
ALTER TABLE service_catalog_therapist_override ALTER COLUMN org_id SET NOT NULL;
CREATE INDEX idx_svc_override_org_id ON service_catalog_therapist_override(org_id);

-- =============================================================================
-- discount_rules
-- =============================================================================
ALTER TABLE discount_rules ADD COLUMN org_id UUID REFERENCES organizations(id);
UPDATE discount_rules SET org_id = '00000000-0000-0000-0000-000000000001' WHERE org_id IS NULL;
ALTER TABLE discount_rules ALTER COLUMN org_id SET NOT NULL;
CREATE INDEX idx_discount_rules_org_id ON discount_rules(org_id);

-- =============================================================================
-- caseload_snapshot
-- =============================================================================
ALTER TABLE caseload_snapshot ADD COLUMN org_id UUID REFERENCES organizations(id);
UPDATE caseload_snapshot SET org_id = '00000000-0000-0000-0000-000000000001' WHERE org_id IS NULL;
ALTER TABLE caseload_snapshot ALTER COLUMN org_id SET NOT NULL;
CREATE INDEX idx_caseload_snapshot_org_id ON caseload_snapshot(org_id);

-- =============================================================================
-- client_risk_flags
-- =============================================================================
ALTER TABLE client_risk_flags ADD COLUMN org_id UUID REFERENCES organizations(id);
UPDATE client_risk_flags SET org_id = '00000000-0000-0000-0000-000000000001' WHERE org_id IS NULL;
ALTER TABLE client_risk_flags ALTER COLUMN org_id SET NOT NULL;
CREATE INDEX idx_client_risk_flags_org_id ON client_risk_flags(org_id);

-- =============================================================================
-- session_follow_up_tasks
-- =============================================================================
ALTER TABLE session_follow_up_tasks ADD COLUMN org_id UUID REFERENCES organizations(id);
UPDATE session_follow_up_tasks SET org_id = '00000000-0000-0000-0000-000000000001' WHERE org_id IS NULL;
ALTER TABLE session_follow_up_tasks ALTER COLUMN org_id SET NOT NULL;
CREATE INDEX idx_follow_up_tasks_org_id ON session_follow_up_tasks(org_id);

-- =============================================================================
-- therapist_recurring_schedule
-- =============================================================================
ALTER TABLE therapist_recurring_schedule ADD COLUMN org_id UUID REFERENCES organizations(id);
UPDATE therapist_recurring_schedule SET org_id = '00000000-0000-0000-0000-000000000001' WHERE org_id IS NULL;
ALTER TABLE therapist_recurring_schedule ALTER COLUMN org_id SET NOT NULL;
CREATE INDEX idx_therapist_sched_org_id ON therapist_recurring_schedule(org_id);

-- =============================================================================
-- therapist_schedule_override
-- =============================================================================
ALTER TABLE therapist_schedule_override ADD COLUMN org_id UUID REFERENCES organizations(id);
UPDATE therapist_schedule_override SET org_id = '00000000-0000-0000-0000-000000000001' WHERE org_id IS NULL;
ALTER TABLE therapist_schedule_override ALTER COLUMN org_id SET NOT NULL;
CREATE INDEX idx_therapist_sched_ovr_org_id ON therapist_schedule_override(org_id);

-- =============================================================================
-- therapist_leave
-- =============================================================================
ALTER TABLE therapist_leave ADD COLUMN org_id UUID REFERENCES organizations(id);
UPDATE therapist_leave SET org_id = '00000000-0000-0000-0000-000000000001' WHERE org_id IS NULL;
ALTER TABLE therapist_leave ALTER COLUMN org_id SET NOT NULL;
CREATE INDEX idx_therapist_leave_org_id ON therapist_leave(org_id);

-- =============================================================================
-- notification_templates
-- =============================================================================
ALTER TABLE notification_templates ADD COLUMN org_id UUID REFERENCES organizations(id);
UPDATE notification_templates SET org_id = '00000000-0000-0000-0000-000000000001' WHERE org_id IS NULL;
ALTER TABLE notification_templates ALTER COLUMN org_id SET NOT NULL;
CREATE INDEX idx_notif_templates_org_id ON notification_templates(org_id);

-- Replace partial index on (event_type, channel, language) with org-scoped version
DROP INDEX IF EXISTS uq_notification_template_active;
CREATE UNIQUE INDEX uq_notification_template_active
    ON notification_templates (org_id, event_type, channel, language)
    WHERE status = 'ACTIVE';

-- =============================================================================
-- outcome_measure_definition
-- =============================================================================
ALTER TABLE outcome_measure_definition ADD COLUMN org_id UUID REFERENCES organizations(id);
UPDATE outcome_measure_definition SET org_id = '00000000-0000-0000-0000-000000000001' WHERE org_id IS NULL;
ALTER TABLE outcome_measure_definition ALTER COLUMN org_id SET NOT NULL;
CREATE INDEX idx_outcome_measure_org_id ON outcome_measure_definition(org_id);

-- Replace global UNIQUE(code) with org-scoped composite
ALTER TABLE outcome_measure_definition DROP CONSTRAINT IF EXISTS uq_outcome_measure_code;
ALTER TABLE outcome_measure_definition ADD CONSTRAINT uq_outcome_measure_org_code UNIQUE (org_id, code);

-- =============================================================================
-- note_template
-- =============================================================================
ALTER TABLE note_template ADD COLUMN org_id UUID REFERENCES organizations(id);
UPDATE note_template SET org_id = '00000000-0000-0000-0000-000000000001' WHERE org_id IS NULL;
ALTER TABLE note_template ALTER COLUMN org_id SET NOT NULL;
CREATE INDEX idx_note_template_org_id ON note_template(org_id);
