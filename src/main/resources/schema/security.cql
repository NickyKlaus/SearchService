CREATE TABLE IF NOT EXISTS persistent_logins(
    username text,
    series_id text PRIMARY KEY,
    security_token text,
    last_used timestamp
);

DROP TABLE IF EXISTS persistent_logins;

CREATE TABLE IF NOT EXISTS persistent_users(
    username text PRIMARY KEY,
    series_id set<text>
);

DROP TABLE IF EXISTS persistent_users;