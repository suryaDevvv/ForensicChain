CREATE TABLE users (
    id UUID PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE cases (
    id UUID PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL,
    created_by UUID NOT NULL REFERENCES users (id),
    created_at TIMESTAMPTZ NOT NULL
);
