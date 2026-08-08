-- Clears the user_collection table to provide a 'clean slate' and remove legacy records.
-- Previously, image_url stored '/api/v1/images/{id}/content', but it now stores base64 data.
TRUNCATE TABLE user_collection;
