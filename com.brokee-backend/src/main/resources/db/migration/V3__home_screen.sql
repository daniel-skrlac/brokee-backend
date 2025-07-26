CREATE TABLE tx
(
    id            BIGINT IDENTITY PRIMARY KEY,
    user_sub      VARCHAR(60)    NOT NULL,
    type          CHAR(1)        NOT NULL CHECK (type IN ('E', 'I')),
    amount        DECIMAL(18, 2) NOT NULL,
    category_id   BIGINT         NOT NULL,
    tx_time       DATETIME2      NOT NULL DEFAULT SYSUTCDATETIME(),
    latitude      DECIMAL(9, 6) NULL,
    longitude     DECIMAL(9, 6) NULL,
    location_name VARCHAR(255) NULL
    note          VARCHAR(500)
);

CREATE TABLE category
(
    id   BIGINT IDENTITY PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE budget
(
    user_sub    VARCHAR(36)    NOT NULL,
    category_id BIGINT         NOT NULL,
    amount      DECIMAL(18, 2) NOT NULL,
    CONSTRAINT pk_budget PRIMARY KEY (user_sub, category_id, month_key)
);

CREATE TABLE planned_tx
(
    id        BIGINT IDENTITY PRIMARY KEY,
    user_sub  VARCHAR(36)    NOT NULL,
    title     VARCHAR(255)   NOT NULL,
    amount    DECIMAL(18, 2) NOT NULL,
    due_date  DATE           NOT NULL,
    auto_book BIT            NOT NULL DEFAULT 0
);

ALTER TABLE tx
    ADD CONSTRAINT fk_tx_category
        FOREIGN KEY (category_id)
            REFERENCES category (id);

ALTER TABLE budget
    ADD CONSTRAINT fk_budget_category
        FOREIGN KEY (category_id)
            REFERENCES category (id);


INSERT INTO categories (name)
VALUES ('Groceries'),
       ('Dining Out'),
       ('Transportation'),
       ('Fuel'),
       ('Utilities'),
       ('Rent'),
       ('Internet'),
       ('Mobile Phone'),
       ('Entertainment'),
       ('Streaming Services'),
       ('Health & Fitness'),
       ('Insurance'),
       ('Shopping'),
       ('Clothing'),
       ('Travel'),
       ('Education'),
       ('Gifts & Donations'),
       ('Kids'),
       ('Pets'),
       ('Home Maintenance'),
       ('Personal Care'),
       ('Subscriptions'),
       ('Taxes'),
       ('Investments'),
       ('Savings'),
       ('Business'),
       ('Miscellaneous'),
       ('Cash Withdrawal'),
       ('ATM Fee');

INSERT INTO categories (name)
VALUES ('Revolut');

CREATE TABLE savings_goal
(
    user_sub    VARCHAR(36)    NOT NULL PRIMARY KEY,
    target_amt  DECIMAL(18, 2) NOT NULL,
    target_date DATE           NOT NULL
);

