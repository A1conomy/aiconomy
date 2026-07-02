-- V2: Escrow holds lock client funds until task acceptance or rejection.

CREATE TABLE escrow_holds (
    id              UUID            PRIMARY KEY,
    from_account_id UUID            NOT NULL,
    to_account_id   UUID            NOT NULL,
    task_id         UUID            NOT NULL,
    amount          NUMERIC(19, 2)  NOT NULL,
    status          VARCHAR(20)     NOT NULL,
    version         BIGINT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_escrow_amount_positive CHECK (amount > 0)
);

CREATE INDEX idx_escrow_holds_task_id ON escrow_holds (task_id);
CREATE INDEX idx_escrow_holds_status ON escrow_holds (status);
