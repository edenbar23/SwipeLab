-- ============================================================
-- V16 – Species reference image pool
-- Each species (label) has a pool of reference images that
-- researchers upload. Tasks pick a subset (1-3) from the pool.
-- ============================================================

-- ── 1. Per-species image pool ─────────────────────────────────
-- Stores the globally shared reference images per species.
-- Images are compressed + thumbnailed at upload time.
CREATE TABLE IF NOT EXISTS species_reference_images (
    id               BIGSERIAL PRIMARY KEY,
    label_id         BIGINT        NOT NULL REFERENCES labels(id) ON DELETE CASCADE,
    image_base64     TEXT          NOT NULL,   -- compressed full image stored as Base64
    thumbnail_base64 TEXT          NOT NULL,   -- 200px thumbnail stored as Base64
    file_size_bytes  BIGINT,                   -- post-compression size in bytes
    caption          VARCHAR(255),
    uploaded_by      VARCHAR(255)  NOT NULL,   -- researcher username

    created_at       TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_species_ref_images_label ON species_reference_images(label_id);

-- ── 2. Task-scoped selection join table ───────────────────────
-- Maps which pool images are used for a specific task.
-- Classifiers see only the images selected for their task.
CREATE TABLE IF NOT EXISTS task_species_reference_images (
    id                         BIGSERIAL PRIMARY KEY,
    task_id                    BIGINT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    species_reference_image_id BIGINT NOT NULL REFERENCES species_reference_images(id) ON DELETE CASCADE,

    CONSTRAINT uk_task_species_ref UNIQUE (task_id, species_reference_image_id)
);

CREATE INDEX IF NOT EXISTS idx_tsri_task_id ON task_species_reference_images(task_id);
CREATE INDEX IF NOT EXISTS idx_tsri_image_id ON task_species_reference_images(species_reference_image_id);
