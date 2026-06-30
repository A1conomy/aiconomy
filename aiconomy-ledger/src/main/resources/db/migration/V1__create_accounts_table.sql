-- V1: Core account table for the banking ledger.
-- Each row represents one wallet (consumer, firm, or central bank).

CREATE TABLE accounts (
    id          UUID            PRIMARY KEY,
    owner_id    VARCHAR(255)    NOT NULL,
    account_type VARCHAR(50)    NOT NULL,
    balance     NUMERIC(19, 2)  NOT NULL DEFAULT 0,
    version     BIGINT          NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_accounts_balance_non_negative CHECK (balance >= 0)
);

CREATE INDEX idx_accounts_owner_id ON accounts (owner_id);
