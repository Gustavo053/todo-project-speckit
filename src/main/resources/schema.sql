CREATE TABLE IF NOT EXISTS card (
    id         UUID         NOT NULL,
    title      VARCHAR(120) NOT NULL,
    note       VARCHAR(2000),
    stage      VARCHAR(20)  NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    revision   BIGINT       NOT NULL,
    CONSTRAINT pk_card PRIMARY KEY (id),
    CONSTRAINT ck_card_stage CHECK (stage IN ('BACKLOG', 'IN_PROGRESS', 'PAUSED', 'DONE'))
);

-- The board reads every card grouped by stage, most-recently-changed first. That is the only
-- read pattern that matters at this scale (data-model.md).
CREATE INDEX IF NOT EXISTS idx_card_stage_updated_at ON card (stage, updated_at DESC);
