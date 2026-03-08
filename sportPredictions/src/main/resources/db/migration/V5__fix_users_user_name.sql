-- Make user_name NOT NULL (was missing in V1) and add unique constraint
ALTER TABLE users ALTER COLUMN user_name SET NOT NULL;
ALTER TABLE users ADD CONSTRAINT uq_users_user_name UNIQUE (user_name);
