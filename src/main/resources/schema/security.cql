CREATE TABLE IF NOT EXISTS persistent_logins(
    username TEXT,
    series_id TEXT PRIMARY KEY,
    security_token TEXT,
    last_used TIMESTAMP
);

DROP TABLE IF EXISTS persistent_logins;