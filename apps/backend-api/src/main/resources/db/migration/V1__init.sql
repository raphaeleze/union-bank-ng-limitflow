CREATE TABLE users (
    id              UUID PRIMARY KEY,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(30)  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE accounts (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL REFERENCES users(id),
    account_number  VARCHAR(30) NOT NULL UNIQUE,
    daily_limit     NUMERIC(15, 2) NOT NULL,
    used_today      NUMERIC(15, 2) NOT NULL DEFAULT 0,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_accounts_user_id ON accounts(user_id);

CREATE TABLE limit_requests (
    id                      UUID PRIMARY KEY,
    account_id              UUID NOT NULL REFERENCES accounts(id),
    current_limit           NUMERIC(15, 2) NOT NULL,
    requested_limit         NUMERIC(15, 2) NOT NULL,
    reason                  VARCHAR(500) NOT NULL,
    status                  VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    risk_level              VARCHAR(10),
    known_device            BOOLEAN NOT NULL DEFAULT TRUE,
    otp_verified_at         TIMESTAMPTZ,
    biometric_verified_at   TIMESTAMPTZ,
    resolved_by             UUID REFERENCES users(id),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_limit_requests_account_id ON limit_requests(account_id);
CREATE INDEX idx_limit_requests_status_risk ON limit_requests(status, risk_level);

CREATE TABLE otp_codes (
    id                  UUID PRIMARY KEY,
    limit_request_id    UUID NOT NULL REFERENCES limit_requests(id),
    code_hash           VARCHAR(255) NOT NULL,
    expires_at          TIMESTAMPTZ NOT NULL,
    verified_at         TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_otp_codes_limit_request_id ON otp_codes(limit_request_id);

CREATE TABLE notifications (
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES users(id),
    type        VARCHAR(40) NOT NULL,
    title       VARCHAR(255) NOT NULL,
    message     VARCHAR(1000) NOT NULL,
    read_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_notifications_user_id ON notifications(user_id);

CREATE TABLE audit_logs (
    id              UUID PRIMARY KEY,
    actor_user_id   UUID NOT NULL REFERENCES users(id),
    action          VARCHAR(100) NOT NULL,
    entity_type     VARCHAR(60) NOT NULL,
    entity_id       VARCHAR(60),
    metadata        TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);

CREATE TABLE support_notes (
    id                  UUID PRIMARY KEY,
    limit_request_id    UUID NOT NULL REFERENCES limit_requests(id),
    author_user_id      UUID NOT NULL REFERENCES users(id),
    note                TEXT NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_support_notes_limit_request_id ON support_notes(limit_request_id);
