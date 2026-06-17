CREATE TABLE conversation_sequence (
    conversation_id UUID PRIMARY KEY,
    last_sequence_number BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE chats (
    id UUID PRIMARY KEY,
    client_message_id VARCHAR(100) UNIQUE NOT NULL,
    sender_id UUID NOT NULL,
    receiver_id UUID NOT NULL,
    message TEXT NOT NULL, -- Will store AES-256 encrypted string
    type VARCHAR(50) NOT NULL DEFAULT 'TEXT',
    sequence_number BIGINT NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    reply_to_id UUID,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE message_receipts (
    id UUID PRIMARY KEY,
    message_id UUID NOT NULL,
    user_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL, -- SENT, DELIVERED, READ
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE message_attachments (
    id UUID PRIMARY KEY,
    message_id UUID,
    group_message_id UUID,
    file_url VARCHAR(255) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE draft_messages (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    recipient_id UUID NOT NULL,
    is_group BOOLEAN NOT NULL DEFAULT FALSE,
    content TEXT NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE blocked_users (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    blocked_user_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    UNIQUE(user_id, blocked_user_id)
);

CREATE TABLE reports (
    id UUID PRIMARY KEY,
    reporter_id UUID NOT NULL,
    reported_user_id UUID NOT NULL,
    reason TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, RESOLVED
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE archived_conversations (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    conversation_id UUID NOT NULL,
    is_group BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    UNIQUE(user_id, conversation_id)
);

CREATE TABLE scheduled_messages (
    id UUID PRIMARY KEY,
    sender_id UUID NOT NULL,
    recipient_id UUID NOT NULL,
    is_group BOOLEAN NOT NULL DEFAULT FALSE,
    message TEXT NOT NULL,
    scheduled_time TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, SENT, FAILED
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, PROCESSED
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_chats_sender ON chats(sender_id);
CREATE INDEX idx_chats_receiver ON chats(receiver_id);
CREATE INDEX idx_chats_client_msg_id ON chats(client_message_id);
CREATE INDEX idx_message_receipts_message ON message_receipts(message_id);
CREATE INDEX idx_message_attachments_message ON message_attachments(message_id);
CREATE INDEX idx_blocked_users_user ON blocked_users(user_id);
CREATE INDEX idx_reports_reported ON reports(reported_user_id);
CREATE INDEX idx_scheduled_messages_time ON scheduled_messages(scheduled_time);
CREATE INDEX idx_outbox_events_status ON outbox_events(status);
