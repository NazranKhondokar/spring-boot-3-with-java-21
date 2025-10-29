-- Seed user data for initial users
DO $$
DECLARE
    user_id_val INT;
BEGIN
    -- Insert first user (Super Admin) if not already exists
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

    -- Get the first user ID
    SELECT id INTO user_id_val
    FROM chat.users
    WHERE firebase_user_id = 'xwtpq8lOCZRD05DkrvSyucGf8T33';

    -- Assign SUPER_ADMIN role (role_id = 3) to first user
    IF user_id_val IS NOT NULL THEN
        INSERT INTO chat.user_role (user_id, role_id)
        VALUES (user_id_val, 3)
        ON CONFLICT (user_id, role_id) DO NOTHING;
    END IF;

    -- Insert second user if not already exists
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
        'Nazran',
        'Khondokar',
        'nazran91@gmail.com',
        true,
        'VggTUyi4RlMhtJ78oE9ZsnuWa232',
        'ACTIVE'::chat.user_status,
        NOW() AT TIME ZONE 'UTC',
        NULL
    WHERE NOT EXISTS (
        SELECT 1 FROM chat.users
        WHERE firebase_user_id = 'VggTUyi4RlMhtJ78oE9ZsnuWa232'
           OR email = 'nazran91@gmail.com'
    );

    -- Get the second user ID
    SELECT id INTO user_id_val
    FROM chat.users
    WHERE firebase_user_id = 'VggTUyi4RlMhtJ78oE9ZsnuWa232';

    -- Assign role (role_id = 1) to second user
    IF user_id_val IS NOT NULL THEN
        INSERT INTO chat.user_role (user_id, role_id)
        VALUES (user_id_val, 1)
        ON CONFLICT (user_id, role_id) DO NOTHING;
    END IF;
END $$;