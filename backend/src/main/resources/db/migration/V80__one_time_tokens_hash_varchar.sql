-- V80: Hibernate schema validation expects VARCHAR for one_time_tokens.token_hash;
-- V79 created it as CHAR(64) (bpchar), which makes startup fail with
-- "wrong column type encountered in column [token_hash]".
ALTER TABLE one_time_tokens ALTER COLUMN token_hash TYPE VARCHAR(64);
