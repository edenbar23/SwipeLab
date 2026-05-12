-- Migrate existing ADMIN users to RESEARCHER
UPDATE users SET role = 'RESEARCHER' WHERE role = 'ADMIN';

-- Create table to track which researchers a task is shared with
CREATE TABLE task_shared_researchers (
    task_id BIGINT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    username VARCHAR(255) NOT NULL REFERENCES users(username) ON DELETE CASCADE,
    PRIMARY KEY (task_id, username)
);
