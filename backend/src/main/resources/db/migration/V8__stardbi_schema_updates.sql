-- V8: All columns added here (tasks.title, images.parent_image_id,
-- images.external_box_id, images.src_path, images.image_url nullable)
-- are now part of the initial schema (V1).
-- This migration is intentionally a no-op to preserve version numbering.
SELECT 1;
