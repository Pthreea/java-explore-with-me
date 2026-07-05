-- Пользователи
CREATE TABLE IF NOT EXISTS users (
                                     id BIGSERIAL PRIMARY KEY,
                                     name VARCHAR(250) NOT NULL,
    email VARCHAR(254) NOT NULL UNIQUE
    );

-- Категории
CREATE TABLE IF NOT EXISTS categories (
                                          id BIGSERIAL PRIMARY KEY,
                                          name VARCHAR(50) NOT NULL UNIQUE
    );

CREATE TABLE IF NOT EXISTS locations (
                                         id BIGSERIAL PRIMARY KEY,
                                         name VARCHAR(255) UNIQUE,
    lat REAL NOT NULL,
    lon REAL NOT NULL,
    description VARCHAR(1000),
    radius REAL DEFAULT 1000.0
    );

-- События (теперь можем ссылаться на locations)
CREATE TABLE IF NOT EXISTS events (
                                      id BIGSERIAL PRIMARY KEY,
                                      annotation VARCHAR(2000) NOT NULL,
    category_id BIGINT NOT NULL REFERENCES categories(id),
    description VARCHAR(7000) NOT NULL,
    event_date TIMESTAMP NOT NULL,
    initiator_id BIGINT NOT NULL REFERENCES users(id),
    location_id BIGINT REFERENCES locations(id), -- ✅ НОВОЕ: ссылка на локацию
    lat REAL, -- ✅ Оставляем для обратной совместимости (nullable)
    lon REAL, -- ✅ Оставляем для обратной совместимости (nullable)
    paid BOOLEAN NOT NULL DEFAULT FALSE,
    participant_limit INTEGER DEFAULT 0,
    request_moderation BOOLEAN NOT NULL DEFAULT TRUE,
    state VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    title VARCHAR(120) NOT NULL,
    created_on TIMESTAMP NOT NULL DEFAULT NOW(),
    published_on TIMESTAMP
    );

CREATE INDEX IF NOT EXISTS idx_events_category ON events(category_id);
CREATE INDEX IF NOT EXISTS idx_events_initiator ON events(initiator_id);
CREATE INDEX IF NOT EXISTS idx_events_state ON events(state);
CREATE INDEX IF NOT EXISTS idx_events_event_date ON events(event_date);
CREATE INDEX IF NOT EXISTS idx_events_location ON events(location_id); -- ✅ Индекс для локаций

-- Заявки на участие
CREATE TABLE IF NOT EXISTS requests (
                                        id BIGSERIAL PRIMARY KEY,
                                        event_id BIGINT NOT NULL REFERENCES events(id),
    requester_id BIGINT NOT NULL REFERENCES users(id),
    created TIMESTAMP NOT NULL DEFAULT NOW(),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    CONSTRAINT uq_request UNIQUE (event_id, requester_id)
    );

CREATE INDEX IF NOT EXISTS idx_requests_event ON requests(event_id);
CREATE INDEX IF NOT EXISTS idx_requests_requester ON requests(requester_id);
CREATE INDEX IF NOT EXISTS idx_requests_status ON requests(status);

-- Подборки событий
CREATE TABLE IF NOT EXISTS compilations (
                                            id BIGSERIAL PRIMARY KEY,
                                            title VARCHAR(50) NOT NULL,
    pinned BOOLEAN NOT NULL DEFAULT FALSE
    );

-- Связь подборок и событий
CREATE TABLE IF NOT EXISTS compilation_events (
                                                  compilation_id BIGINT NOT NULL REFERENCES compilations(id) ON DELETE CASCADE,
    event_id BIGINT NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    PRIMARY KEY (compilation_id, event_id)
    );