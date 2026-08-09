-- V21__rename_src_path_to_image_data.sql
-- Rename the src_path column to image_data since it now exclusively holds Base64 strings or URLs instead of local file paths

ALTER TABLE images RENAME COLUMN src_path TO image_data;
