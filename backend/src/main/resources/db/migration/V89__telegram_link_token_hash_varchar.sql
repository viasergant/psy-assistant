-- Same bpchar vs varchar validation failure as V88, for telegram_link_token.token_hash (created in V83).
ALTER TABLE telegram_link_token ALTER COLUMN token_hash TYPE VARCHAR(64);
