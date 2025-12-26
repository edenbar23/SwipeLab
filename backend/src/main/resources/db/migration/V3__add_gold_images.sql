-- GOLD IMAGES EXTENSION
-- This table stores specific metadata for gold standard images, 
-- though the 'images' table already has the 'is_gold_standard' flag and 'correct_label_id'.
-- This table is for extra metadata like difficulty or explanation.

CREATE TABLE gold_images (
    id BIGSERIAL PRIMARY KEY,
    image_id BIGINT NOT NULL UNIQUE,
    difficulty_level VARCHAR(50) DEFAULT 'MEDIUM', -- EASY, MEDIUM, HARD
    explanation TEXT, -- Why acts as the gold standard (e.g. expert notes)
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_gold_image_main FOREIGN KEY (image_id) REFERENCES images(id)
);
