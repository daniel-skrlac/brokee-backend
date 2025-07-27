CREATE TABLE binance_tokens
(
    id         BIGINT IDENTITY PRIMARY KEY,
    user_sub   VARCHAR(36)  NOT NULL,
    api_key    VARCHAR(255) NOT NULL,
    secret_key VARCHAR(255) NOT NULL,
    created_at DATETIME2    NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2    NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uq_binance_tokens_user UNIQUE (user_sub)
);

CREATE INDEX idx_binance_tokens_user_sub
    ON binance_tokens (user_sub);
