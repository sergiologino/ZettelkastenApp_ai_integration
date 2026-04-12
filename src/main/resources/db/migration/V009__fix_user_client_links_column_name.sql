-- Fix column name mismatch: model expects client_application_id, migration created client_app_id
ALTER TABLE user_client_links 
    RENAME COLUMN client_app_id TO client_application_id;

