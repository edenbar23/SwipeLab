-- ============================================================
-- V17 – Soft delete for species reference images
-- ============================================================

ALTER TABLE species_reference_images ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT FALSE;
