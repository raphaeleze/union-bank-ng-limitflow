-- Demo seed data. All seeded users share the password: Password123!
-- (bcrypt hash below was generated for that password.)

INSERT INTO users (id, first_name, last_name, email, password_hash, role) VALUES
    ('11111111-1111-1111-1111-111111111111', 'Amara', 'Chukwu', 'customer@limitflow.demo',
     '$2b$10$IYpHdLZjc0NW3cC4x.MiCu3U24jXQ4.F6in4YQN2BRV68XtiSeAhq', 'CUSTOMER'),
    ('22222222-2222-2222-2222-222222222222', 'Tunde', 'Bakare', 'support@limitflow.demo',
     '$2b$10$IYpHdLZjc0NW3cC4x.MiCu3U24jXQ4.F6in4YQN2BRV68XtiSeAhq', 'SUPPORT_AGENT'),
    ('33333333-3333-3333-3333-333333333333', 'Ngozi', 'Eze', 'manager@limitflow.demo',
     '$2b$10$IYpHdLZjc0NW3cC4x.MiCu3U24jXQ4.F6in4YQN2BRV68XtiSeAhq', 'MANAGER');

INSERT INTO accounts (id, user_id, account_number, daily_limit, used_today, status) VALUES
    ('44444444-4444-4444-4444-444444444444', '11111111-1111-1111-1111-111111111111',
     '0123456789', 200000.00, 180000.00, 'ACTIVE');

INSERT INTO limit_requests (
    id, account_id, current_limit, requested_limit, reason, status, risk_level,
    known_device, otp_verified_at, biometric_verified_at, created_at, updated_at
) VALUES (
    '55555555-5555-5555-5555-555555555555', '44444444-4444-4444-4444-444444444444',
    200000.00, 500000.00, 'Sending funds to a family member for a medical emergency',
    'UNDER_REVIEW', 'MEDIUM', TRUE, now() - interval '20 minutes', now() - interval '18 minutes',
    now() - interval '25 minutes', now() - interval '18 minutes'
);

INSERT INTO notifications (id, user_id, type, title, message, created_at) VALUES
    ('66666666-6666-6666-6666-666666666666', '11111111-1111-1111-1111-111111111111',
     'VERIFICATION_COMPLETED', 'Under review',
     'Your request needs a quick manual review. We''ll notify you as soon as it''s decided.',
     now() - interval '18 minutes');

INSERT INTO audit_logs (id, actor_user_id, action, entity_type, entity_id, metadata, created_at) VALUES
    ('77777777-7777-7777-7777-777777777777', '11111111-1111-1111-1111-111111111111',
     'LIMIT_REQUESTED', 'LimitRequest', '55555555-5555-5555-5555-555555555555', NULL,
     now() - interval '25 minutes'),
    ('88888888-8888-8888-8888-888888888888', '11111111-1111-1111-1111-111111111111',
     'RISK_ASSESSED', 'LimitRequest', '55555555-5555-5555-5555-555555555555', 'MEDIUM',
     now() - interval '18 minutes');
