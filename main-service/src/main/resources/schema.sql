CREATE TABLE IF NOT EXISTS categories (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS events (
    id SERIAL PRIMARY KEY,
    annotation VARCHAR(2000) NOT NULL,
    description VARCHAR(7000) NOT NULL,
    category_id BIGINT NOT NULL REFERENCES categories(id),
    created_on TIMESTAMP NOT NULL,
    event_date TIMESTAMP NOT NULL,
    published_on TIMESTAMP,
    initiator_id BIGINT NOT NULL REFERENCES users(id),
    lat DOUBLE PRECISION,
    lon DOUBLE PRECISION,
    paid BOOLEAN NOT NULL,
    participant_limit INT NOT NULL,
    request_moderation BOOLEAN NOT NULL,
    title VARCHAR(255) NOT NULL,
    state VARCHAR(32) NOT NULL
);

CREATE TABLE IF NOT EXISTS requests (
    id SERIAL PRIMARY KEY,
    created TIMESTAMP NOT NULL,
    event_id BIGINT NOT NULL REFERENCES events(id),
    requester_id BIGINT NOT NULL REFERENCES users(id),
    status VARCHAR(32) NOT NULL,
    CONSTRAINT uq_request UNIQUE(event_id, requester_id)
);

CREATE TABLE IF NOT EXISTS compilations (
    id SERIAL PRIMARY KEY,
    pinned BOOLEAN NOT NULL DEFAULT FALSE,
    title VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS compilation_events (
    compilation_id BIGINT NOT NULL REFERENCES compilations(id) ON DELETE CASCADE,
    event_id BIGINT NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    PRIMARY KEY (compilation_id, event_id)
);
