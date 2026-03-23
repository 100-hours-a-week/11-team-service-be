CREATE TABLE ai_message_outbox (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    exchange VARCHAR(255),
    routing_key VARCHAR(255),
    payload TEXT,
    status VARCHAR(50),
    retry_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
