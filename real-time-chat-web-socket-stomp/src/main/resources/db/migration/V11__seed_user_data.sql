-- Seed user data for initial super admin
DO $$
DECLARE
    user_id_val INT;
BEGIN
    -- Insert user if not already exists
    INSERT INTO chat.users (
        first_name,
        last_name,
        email,
        is_email_verified,
        firebase_user_id,
        status,
        created_at,
        updated_at
    )
    SELECT
        'Khondokar Nazran',
        'Ahmod',
        'nazran.ahmod@gmail.com',
        false,
        'xwtpq8lOCZRD05DkrvSyucGf8T33',
        'ACTIVE'::chat.user_status,
        NOW() AT TIME ZONE 'UTC',
        NULL
    WHERE NOT EXISTS (
        SELECT 1 FROM chat.users
        WHERE firebase_user_id = 'xwtpq8lOCZRD05DkrvSyucGf8T33'
           OR email = 'nazran.ahmod@gmail.com'
    );

    -- Get the user ID (either newly inserted or existing)
    SELECT id INTO user_id_val
    FROM chat.users
    WHERE firebase_user_id = 'xwtpq8lOCZRD05DkrvSyucGf8T33';

    -- Insert role assignment if user exists and role not already assigned
    IF user_id_val IS NOT NULL THEN
        INSERT INTO chat.user_role (user_id, role_id)
        VALUES (user_id_val, 3)
        ON CONFLICT (user_id, role_id) DO NOTHING;
    END IF;
END $$;