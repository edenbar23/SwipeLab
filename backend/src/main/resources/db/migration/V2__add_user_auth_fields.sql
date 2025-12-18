-- Add authentication and verification fields to users table

-- Email and authentication
ALTER TABLE users ADD COLUMN IF NOT EXISTS email VARCHAR(255) NOT NULL;
ALTER TABLE users ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT FALSE;

-- OAuth provider information
ALTER TABLE users ADD COLUMN IF NOT EXISTS provider VARCHAR(50) NOT NULL DEFAULT 'LOCAL';
ALTER TABLE users ADD COLUMN IF NOT EXISTS provider_id VARCHAR(255);

-- Refresh token for JWT rotation
ALTER TABLE users ADD COLUMN IF NOT EXISTS refresh_token_hash VARCHAR(255);

-- Password reset tokens
ALTER TABLE users ADD COLUMN IF NOT EXISTS reset_password_token VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS reset_token_expiry TIMESTAMP;

-- Email verification token
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verification_token VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS verification_token_expiry TIMESTAMP;

-- User profile
ALTER TABLE users ADD COLUMN IF NOT EXISTS display_name VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS profile_image_url VARCHAR(500);

-- Credibility and tracking
ALTER TABLE users ADD COLUMN IF NOT EXISTS credibility_score DOUBLE PRECISION DEFAULT 0.0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_login TIMESTAMP;

-- Account status
ALTER TABLE users ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS account_locked BOOLEAN NOT NULL DEFAULT FALSE;

-- Ensure unique email constraint
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- Add index on provider_id for OAuth lookups
CREATE INDEX IF NOT EXISTS idx_users_provider_id ON users(provider_id);

-- Add index on tokens for quick lookup
CREATE INDEX IF NOT EXISTS idx_users_reset_token ON users(reset_password_token);
CREATE INDEX IF NOT EXISTS idx_users_verification_token ON users(email_verification_token);

-- Add comments for documentation
COMMENT ON COLUMN users.email IS 'User email address (unique)';
COMMENT ON COLUMN users.password_hash IS 'Hashed password for local auth (null for OAuth)';
COMMENT ON COLUMN users.provider IS 'Authentication provider: LOCAL or GOOGLE';
COMMENT ON COLUMN users.provider_id IS 'External provider user ID for OAuth';