CREATE TABLE consensus_results (
    id SERIAL PRIMARY KEY,
    image_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,
    species VARCHAR(255) NOT NULL,
    winning_decision VARCHAR(255) NOT NULL,
    final_score DOUBLE PRECISION NOT NULL,
    reached_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uc_consensus_results_image_species UNIQUE (image_id, species)
);

CREATE INDEX idx_consensus_results_image_id ON consensus_results(image_id);
CREATE INDEX idx_consensus_results_task_id ON consensus_results(task_id);
