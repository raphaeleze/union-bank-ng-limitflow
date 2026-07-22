CREATE TABLE push_tokens (
    id                 UUID PRIMARY KEY,
    user_id            UUID NOT NULL REFERENCES users(id),
    expo_push_token    VARCHAR(255) NOT NULL,
    platform           VARCHAR(20) NOT NULL,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, expo_push_token)
);
