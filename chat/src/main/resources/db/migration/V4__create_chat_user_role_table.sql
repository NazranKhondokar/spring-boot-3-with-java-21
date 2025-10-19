-- =====================================================
-- Migration: V4__create_chat_user_role_table.sql
-- Description: Create user_role join table in chat schema
-- =====================================================

-- Create user_role join table
CREATE TABLE IF NOT EXISTS chat.user_role (
    user_id INT NOT NULL,
    role_id INT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_chat_user_role_user FOREIGN KEY (user_id)
        REFERENCES chat.users(id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_user_role_role FOREIGN KEY (role_id)
        REFERENCES chat.roles(id) ON DELETE CASCADE
);

-- Create indexes
CREATE INDEX idx_user_role_user_id ON chat.user_role(user_id);
CREATE INDEX idx_user_role_role_id ON chat.user_role(role_id);

-- Add comment
COMMENT ON TABLE chat.user_role IS 'Many-to-many relationship between users and roles';