-- password for both users: secret123
INSERT INTO users (id, username, password, name) VALUES
    (1001, 'alice', '$2a$10$sxzP07CkAIZ8e4Ahu0CSK.J64JvsUvxINm8PGbsit7fvz.j7nfqJe', 'Alice'),
    (1002, 'bob', '$2a$10$sxzP07CkAIZ8e4Ahu0CSK.J64JvsUvxINm8PGbsit7fvz.j7nfqJe', 'Bob');
