-- Insert Roles
INSERT INTO roles (id, name) VALUES (1, 'ROLE_ADMIN');
INSERT INTO roles (id, name) VALUES (2, 'ROLE_APP_USER');
INSERT INTO roles (id, name) VALUES (3, 'ROLE_TEST_USER');

-- Insert Users
INSERT INTO users (id, username, email, password) VALUES (1, 'admin', 'admin@example.com', '$2a$10$YskCUX3.vcyG84fACJKiwO6K53jgn1P8D7L16EfpVAYg.zU6aqSfa');
INSERT INTO users (id, username, email, password) VALUES (2, 'appuser', 'appuser@example.com', '$2a$10$YskCUX3.vcyG84fACJKiwO6K53jgn1P8D7L16EfpVAYg.zU6aqSfa');
INSERT INTO users (id, username, email, password) VALUES (3, 'testuser', 'testuser@example.com', '$2a$10$YskCUX3.vcyG84fACJKiwO6K53jgn1P8D7L16EfpVAYg.zU6aqSfa');

-- Map Users to Roles
INSERT INTO user_roles (user_id, role_id) VALUES (1, 1); -- admin -> ROLE_ADMIN
INSERT INTO user_roles (user_id, role_id) VALUES (2, 2); -- appuser -> ROLE_APP_USER
INSERT INTO user_roles (user_id, role_id) VALUES (3, 3); -- testuser -> ROLE_TEST_USER
