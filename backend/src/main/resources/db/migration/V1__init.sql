CREATE TABLE users
(
    id         BIGINT IDENTITY PRIMARY KEY,
    username      VARCHAR(255) NOT NULL UNIQUE,
    pwd_hash   VARCHAR(255),
    google_sub VARCHAR(255) UNIQUE,
    created_at DATETIME2 DEFAULT GETDATE()
);
