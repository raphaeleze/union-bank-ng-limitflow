ALTER TABLE users ADD COLUMN phone VARCHAR(20);

UPDATE users SET phone = '+2348012345678' WHERE email = 'customer@limitflow.demo';
