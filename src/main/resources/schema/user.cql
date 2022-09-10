CREATE KEYSPACE IF NOT EXISTS auth WITH REPLICATION = {'class': 'SimpleStrategy', 'replication_factor': 1};

USE auth;

CREATE TABLE IF NOT EXISTS account(
    id text,
    password text,
    authorities text,
    dateCreated timestamp,
    PRIMARY KEY ((id))
) WITH comment = 'Accounts';

INSERT INTO account (
    id,
    password,
    authorities,
    dateCreated)
VALUES (
    'nickolai.beloglazov@gmail.com',
    '$2b$12$U1Q1p7305QnKTsiGE6VDPOqFkzXrw26zJdmrAcNLsjBRrlUNCTGBu',
    'ROLE_ADMIN, ROLE_USER',
    '2022-09-09T11:53:38+00:00'
);

SELECT * FROM account;