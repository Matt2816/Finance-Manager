INSERT INTO users (username, email, password_hash, created_at)
SELECT 'default', 'default@localhost', '$2a$10$defaulthashplaceholder000000000000000000000000000', NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'default');
