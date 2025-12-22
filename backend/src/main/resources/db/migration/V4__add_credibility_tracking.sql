-- Add credibility tracking columns to users table

-- Expert agreement score (Cohen's Kappa)
ALTER TABLE users ADD COLUMN IF NOT EXISTS agreement_with_experts DOUBLE PRECISION DEFAULT 0.0;

-- Majority vote agreement score
ALTER TABLE users ADD COLUMN IF NOT EXISTS majority_agreement_score DOUBLE PRECISION DEFAULT 0.0;

-- Classification counters
ALTER TABLE users ADD COLUMN IF NOT EXISTS total_classifications INTEGER DEFAULT 0;

-- Gold standard image tracking (for future implementation)
ALTER TABLE users ADD COLUMN IF NOT EXISTS correct_gold_classifications INTEGER DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS total_gold_classifications INTEGER DEFAULT 0;

-- Add indexes for performance
CREATE INDEX IF NOT EXISTS idx_users_credibility_score ON users(credibility_score);
CREATE INDEX IF NOT EXISTS idx_users_agreement_with_experts ON users(agreement_with_experts);

-- Add comments for documentation
COMMENT ON COLUMN users.agreement_with_experts IS 'Cohen''s Kappa score showing agreement with expert classifications (-1 to 1)';
COMMENT ON COLUMN users.majority_agreement_score IS 'Percentage of times user agrees with majority vote (0 to 1)';
COMMENT ON COLUMN users.total_classifications IS 'Total number of classifications submitted by user';
COMMENT ON COLUMN users.correct_gold_classifications IS 'Number of correct gold standard classifications';
COMMENT ON COLUMN users.total_gold_classifications IS 'Total number of gold standard images classified';