-- V1: Task board entries for the freelancing marketplace.

CREATE TABLE tasks (
    id                  UUID            PRIMARY KEY,
    project_id          UUID            NOT NULL,
    title               VARCHAR(255)    NOT NULL,
    description         TEXT            NOT NULL,
    required_skill      VARCHAR(50)     NOT NULL,
    budget              NUMERIC(19, 2)  NOT NULL,
    client_agent_id     VARCHAR(255)    NOT NULL,
    client_account_id   UUID            NOT NULL,
    status              VARCHAR(20)     NOT NULL,
    assignee_agent_id   VARCHAR(255),
    assignee_account_id UUID,
    escrow_hold_id      UUID,
    deliverable_notes   TEXT,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_tasks_budget_positive CHECK (budget > 0)
);

CREATE INDEX idx_tasks_status ON tasks (status);
CREATE INDEX idx_tasks_project_id ON tasks (project_id);
