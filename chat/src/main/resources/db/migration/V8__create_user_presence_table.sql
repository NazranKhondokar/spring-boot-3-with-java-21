-- =====================================================
-- Migration: V5__create_user_presence_table.sql
-- Description: Create user presence tracking table
-- =====================================================

-- Create user_presence table
CREATE TABLE IF NOT EXISTS chat.user_presence (
    user_id INT PRIMARY KEY,
    is_online BOOLEAN NOT NULL DEFAULT FALSE,
    last_seen TIMESTAMP WITH TIME ZONE NULL,
    device_info VARCHAR(255) NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    updated_at TIMESTAMP WITH TIME ZONE NULL,

    -- Foreign key constraint
    CONSTRAINT fk_user_presence_user FOREIGN KEY (user_id)
        REFERENCES chat.users(id) ON DELETE CASCADE
);

-- Create index
CREATE INDEX idx_user_presence_is_online ON chat.user_presence(is_online);
CREATE INDEX idx_user_presence_last_seen ON chat.user_presence(last_seen DESC);

-- Add comment
COMMENT ON TABLE chat.user_presence IS 'Tracks online/offline status of users in chat';