-- Soft-delete support for gold_images.
-- Rows are never physically deleted so credibility_records FK is never violated.
-- Existing rows are treated as active.
ALTER TABLE gold_images
    ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE;
