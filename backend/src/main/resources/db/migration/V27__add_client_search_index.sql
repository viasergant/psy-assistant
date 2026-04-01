-- V27: Client search optimization with full-text search support.
-- Adds search_vector tsvector column and GIN index for sub-500ms search on 10k+ records.

-- Add tsvector column for full-text search
ALTER TABLE clients
    ADD COLUMN IF NOT EXISTS search_vector tsvector;

-- Populate search_vector with existing data
-- Combines full_name, preferred_name, email, phone, secondary_phone, client_code
UPDATE clients
SET search_vector = 
    setweight(to_tsvector('simple', COALESCE(full_name, '')), 'A') ||
    setweight(to_tsvector('simple', COALESCE(preferred_name, '')), 'B') ||
    setweight(to_tsvector('simple', COALESCE(email, '')), 'C') ||
    setweight(to_tsvector('simple', COALESCE(phone, '')), 'C') ||
    setweight(to_tsvector('simple', COALESCE(secondary_phone, '')), 'C') ||
    setweight(to_tsvector('simple', COALESCE(client_code, '')), 'B');

-- Create GIN index for fast full-text search (O(log n) lookups)
CREATE INDEX idx_clients_search_vector ON clients USING GIN(search_vector);

-- Trigger function to automatically update search_vector on INSERT/UPDATE
CREATE OR REPLACE FUNCTION update_client_search_vector()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    NEW.search_vector := 
        setweight(to_tsvector('simple', COALESCE(NEW.full_name, '')), 'A') ||
        setweight(to_tsvector('simple', COALESCE(NEW.preferred_name, '')), 'B') ||
        setweight(to_tsvector('simple', COALESCE(NEW.email, '')), 'C') ||
        setweight(to_tsvector('simple', COALESCE(NEW.phone, '')), 'C') ||
        setweight(to_tsvector('simple', COALESCE(NEW.secondary_phone, '')), 'C') ||
        setweight(to_tsvector('simple', COALESCE(NEW.client_code, '')), 'B');
    RETURN NEW;
END;
$$;

-- Attach trigger to clients table
CREATE TRIGGER trg_clients_search_vector
    BEFORE INSERT OR UPDATE OF full_name, preferred_name, email, phone, secondary_phone, client_code
    ON clients
    FOR EACH ROW EXECUTE FUNCTION update_client_search_vector();

-- Also index client_tags for tag-based search
CREATE INDEX IF NOT EXISTS idx_client_tags_tag ON client_tags(tag);

COMMENT ON COLUMN clients.search_vector IS 'Full-text search vector combining name, email, phone, and code fields';
COMMENT ON INDEX idx_clients_search_vector IS 'GIN index for fast full-text search (<500ms for 10k records)';
