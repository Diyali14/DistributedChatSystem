CREATE TABLE groups (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    owner_id UUID NOT NULL,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE group_members (
    id UUID PRIMARY KEY,
    group_id UUID NOT NULL,
    user_id UUID NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'MEMBER', -- OWNER, ADMIN, MEMBER
    joined_at TIMESTAMP NOT NULL,
    UNIQUE(group_id, user_id)
);

CREATE TABLE group_messages (
    id UUID PRIMARY KEY,
    client_message_id VARCHAR(100) UNIQUE NOT NULL,
    group_id UUID NOT NULL,
    sender_id UUID NOT NULL,
    message TEXT NOT NULL, -- Will store AES-256 encrypted string
    type VARCHAR(50) NOT NULL DEFAULT 'TEXT',
    sequence_number BIGINT NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    reply_to_id UUID,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE group_invites (
    id UUID PRIMARY KEY,
    group_id UUID NOT NULL,
    invite_code VARCHAR(100) UNIQUE NOT NULL,
    creator_id UUID NOT NULL,
    max_uses INT NOT NULL DEFAULT 0, -- 0 for unlimited
    used_count INT NOT NULL DEFAULT 0,
    expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_groups_owner ON groups(owner_id);
CREATE INDEX idx_group_members_group ON group_members(group_id);
CREATE INDEX idx_group_members_user ON group_members(user_id);
CREATE INDEX idx_group_messages_group ON group_messages(group_id);
CREATE INDEX idx_group_invites_code ON group_invites(invite_code);
CREATE INDEX idx_group_outbox_events_status ON outbox_events(status);
