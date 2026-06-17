CREATE TABLE users (
    id UUID PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    bio TEXT,
    avatar_url VARCHAR(255),
    status VARCHAR(50) DEFAULT 'OFFLINE',
    enabled BOOLEAN DEFAULT FALSE,
    failed_login_attempts INT DEFAULT 0,
    lock_time TIMESTAMP,
    version INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE device_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    token VARCHAR(255) NOT NULL,
    device_name VARCHAR(255),
    ip_address VARCHAR(100),
    location VARCHAR(255),
    last_active_at TIMESTAMP NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE login_history (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    ip_address VARCHAR(100),
    browser VARCHAR(255),
    location VARCHAR(255),
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE security_events (
    id UUID PRIMARY KEY,
    user_id UUID,
    event_type VARCHAR(100) NOT NULL,
    ip_address VARCHAR(100),
    details TEXT,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_device_sessions_user ON device_sessions(user_id);
CREATE INDEX idx_device_sessions_token ON device_sessions(token);
CREATE INDEX idx_login_history_user ON login_history(user_id);
CREATE INDEX idx_security_events_user ON security_events(user_id);
