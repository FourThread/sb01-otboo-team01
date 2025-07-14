-- 의존성 순서에 따른 테이블 삭제 (자식 테이블부터)
DROP TABLE IF EXISTS clothes_attributes;
DROP TABLE IF EXISTS feed_clothes;
DROP TABLE IF EXISTS feed_comments;
DROP TABLE IF EXISTS feed_likes;
DROP TABLE IF EXISTS direct_messages;
DROP TABLE IF EXISTS user_oauth_providers;
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS feeds;
DROP TABLE IF EXISTS clothes;
DROP TABLE IF EXISTS profiles;
DROP TABLE IF EXISTS clothes_attribute_definitions;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS weathers;

-- 사용자 테이블
CREATE TABLE users
(
    id                      UUID                  NOT NULL,
    created_at              TIMESTAMP(6)          NOT NULL,
    updated_at              TIMESTAMP(6)          NULL,
    email                   VARCHAR(100)          NOT NULL,
    locked                  BOOLEAN DEFAULT FALSE NOT NULL,
    name                    VARCHAR(100)          NOT NULL,
    password                VARCHAR(100)          NOT NULL,
    role                    VARCHAR(255)          NOT NULL,
    temp_password_issued_at TIMESTAMP(6)          NULL,
    CONSTRAINT users_pkey PRIMARY KEY (id),
    CONSTRAINT uk_users_name UNIQUE (name),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT users_role_check CHECK (role IN ('ADMIN', 'USER'))
);

-- 프로필 테이블
CREATE TABLE profiles
(
    id                      UUID          NOT NULL,
    created_at              TIMESTAMP(6)  NOT NULL,
    updated_at              TIMESTAMP(6)  NULL,
    birth_date              DATE          NULL,
    gender                  VARCHAR(10)   NULL,
    latitude                FLOAT8        NULL,
    longitude               FLOAT8        NULL,
    x                       INTEGER       NULL,
    y                       INTEGER       NULL,
    location_names          VARCHAR(500)  NULL,
    name                    VARCHAR(50)   NULL,
    profile_image_url       VARCHAR(2048) NULL,
    temperature_sensitivity INTEGER       NULL,
    user_id                 UUID          NULL,
    CONSTRAINT profiles_pkey PRIMARY KEY (id),
    CONSTRAINT uk_profiles_user_id UNIQUE (user_id),
    CONSTRAINT profiles_gender_check CHECK (gender IN ('MALE', 'FEMALE', 'ETC')),
    CONSTRAINT fk_profiles_user_id FOREIGN KEY (user_id) REFERENCES users (id)
);

-- 의상 속성 정의 테이블
CREATE TABLE clothes_attribute_definitions
(
    id                UUID         NOT NULL,
    created_at        TIMESTAMP(6) NOT NULL,
    updated_at        TIMESTAMP(6) NULL,
    name              VARCHAR(255) NOT NULL,
    selectable_values TEXT         NULL,
    CONSTRAINT clothes_attribute_definitions_pkey PRIMARY KEY (id),
    CONSTRAINT uk_clothes_attribute_definitions_name UNIQUE (name)
);

-- 의상 테이블
CREATE TABLE clothes
(
    id         UUID          NOT NULL,
    created_at TIMESTAMP(6)  NOT NULL,
    updated_at TIMESTAMP(6)  NULL,
    image_url  VARCHAR(2048) NULL,
    name       VARCHAR(255)  NOT NULL,
    owner_id   UUID          NOT NULL,
    type       VARCHAR(255)  NULL,
    CONSTRAINT clothes_pkey PRIMARY KEY (id),
    CONSTRAINT clothes_type_check CHECK (type IN ('TOP', 'BOTTOM', 'DRESS', 'OUTER', 'UNDERWEAR',
                                                  'ACCESSORY', 'SHOES', 'SOCKS', 'HAT', 'BAG',
                                                  'SCARF', 'ETC')),
    CONSTRAINT fk_clothes_owner_id FOREIGN KEY (owner_id) REFERENCES users (id)
);

-- 날씨 테이블
CREATE TABLE weathers
(
    id                                 UUID         NOT NULL,
    created_at                         TIMESTAMP(6) NOT NULL,
    api_response_hash                  VARCHAR(255) NULL,
    forecast_at                        TIMESTAMP(6) NOT NULL,
    forecasted_at                      TIMESTAMP(6) NOT NULL,
    humidity_compared_to_day_before    FLOAT8       NULL,
    humidity_current                   FLOAT8       NULL,
    latitude                           FLOAT8       NULL,
    location_names                     VARCHAR(500) NULL,
    longitude                          FLOAT8       NULL,
    x                                  INTEGER      NULL,
    y                                  INTEGER      NULL,
    amount                             FLOAT8       NULL,
    probability                        FLOAT8       NULL,
    type                               VARCHAR(255) NULL,
    sky_status                         VARCHAR(255) NOT NULL,
    temperature_compared_to_day_before FLOAT8       NULL,
    temperature_current                FLOAT8       NULL,
    max                                FLOAT8       NULL,
    min                                FLOAT8       NULL,
    direction                          FLOAT8       NULL,
    speed                              FLOAT8       NULL,
    strength                           VARCHAR(255) NULL,
    u_component                        FLOAT8       NULL,
    v_component                        FLOAT8       NULL,
    CONSTRAINT weathers_pkey PRIMARY KEY (id),
    CONSTRAINT uk_weathers_api_response_hash UNIQUE (api_response_hash),
    CONSTRAINT weathers_sky_status_check CHECK (sky_status IN ('CLEAR', 'MOSTLY_CLOUDY', 'CLOUDY')),
    CONSTRAINT weathers_strength_check CHECK (strength IN ('WEAK', 'MODERATE', 'STRONG')),
    CONSTRAINT weathers_type_check CHECK (type IN ('NONE', 'RAIN', 'RAIN_SNOW', 'SNOW', 'SHOWER'))
);

-- 피드 테이블
CREATE TABLE feeds
(
    id            UUID         NOT NULL,
    created_at    TIMESTAMP(6) NOT NULL,
    updated_at    TIMESTAMP(6) NULL,
    comment_count BYTEA,
    content       VARCHAR(255) NULL,
    like_count    BYTEA,
    author_id     UUID         NULL,
    weather_id    UUID         NULL,
    CONSTRAINT feeds_pkey PRIMARY KEY (id),
    CONSTRAINT fk_feeds_weather_id FOREIGN KEY (weather_id) REFERENCES weathers (id),
    CONSTRAINT fk_feeds_author_id FOREIGN KEY (author_id) REFERENCES users (id)
);

-- 의상 속성 테이블
CREATE TABLE clothes_attributes
(
    id              UUID         NOT NULL,
    created_at      TIMESTAMP(6) NOT NULL,
    attribute_value VARCHAR(255) NOT NULL,
    clothes_id      UUID         NULL,
    definition_id   UUID         NULL,
    CONSTRAINT clothes_attributes_pkey PRIMARY KEY (id),
    CONSTRAINT fk_clothes_attributes_clothes_id FOREIGN KEY (clothes_id) REFERENCES clothes (id),
    CONSTRAINT fk_clothes_attributes_definition_id FOREIGN KEY (definition_id) REFERENCES clothes_attribute_definitions (id)
);

-- 다이렉트 메시지 테이블
CREATE TABLE direct_messages
(
    id          UUID         NOT NULL,
    created_at  TIMESTAMP(6) NOT NULL,
    content     VARCHAR(255) NULL,
    dm_key      VARCHAR(255) NULL,
    receiver_id UUID         NULL,
    sender_id   UUID         NULL,
    CONSTRAINT direct_messages_pkey PRIMARY KEY (id),
    CONSTRAINT fk_direct_messages_sender_id FOREIGN KEY (sender_id) REFERENCES users (id),
    CONSTRAINT fk_direct_messages_receiver_id FOREIGN KEY (receiver_id) REFERENCES users (id)
);

-- 피드-의상 연결 테이블
CREATE TABLE feed_clothes
(
    id         UUID         NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NULL,
    clothes_id UUID         NULL,
    feed_id    UUID         NULL,
    CONSTRAINT feed_clothes_pkey PRIMARY KEY (id),
    CONSTRAINT fk_feed_clothes_clothes_id FOREIGN KEY (clothes_id) REFERENCES clothes (id),
    CONSTRAINT fk_feed_clothes_feed_id FOREIGN KEY (feed_id) REFERENCES feeds (id)
);

-- 피드 댓글 테이블
CREATE TABLE feed_comments
(
    id         UUID         NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    content    VARCHAR(255) NULL,
    author_id  UUID         NULL,
    feed_id    UUID         NULL,
    CONSTRAINT feed_comments_pkey PRIMARY KEY (id),
    CONSTRAINT fk_feed_comments_feed_id FOREIGN KEY (feed_id) REFERENCES feeds (id) ON DELETE CASCADE,
    CONSTRAINT fk_feed_comments_author_id FOREIGN KEY (author_id) REFERENCES users (id)
);

-- 피드 좋아요 테이블
CREATE TABLE feed_likes
(
    id         UUID         NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    feed_id    UUID         NULL,
    user_id    UUID         NULL,
    CONSTRAINT feed_likes_pkey PRIMARY KEY (id),
    CONSTRAINT fk_feed_likes_feed_id FOREIGN KEY (feed_id) REFERENCES feeds (id),
    CONSTRAINT fk_feed_likes_user_id FOREIGN KEY (user_id) REFERENCES users (id)
);

-- OAuth 제공자 테이블
CREATE TABLE user_oauth_providers
(
    user_id  UUID        NOT NULL,
    provider VARCHAR(20) NULL,
    CONSTRAINT user_oauth_providers_provider_check CHECK (provider IN ('KAKAO', 'GOOGLE')),
    CONSTRAINT fk_user_oauth_providers_user_id FOREIGN KEY (user_id) REFERENCES users (id)
);

-- 알림 테이블
CREATE TABLE notifications
(
    id          UUID         NOT NULL,
    created_at  TIMESTAMP(6) NOT NULL,
    receiver_id UUID         NOT NULL,
    title       VARCHAR(255) NOT NULL,
    content     VARCHAR(255) NOT NULL,
    level       VARCHAR(255) NOT NULL,
    CONSTRAINT notifications_pkey PRIMARY KEY (id),
    CONSTRAINT fk_notifications_receiver_id FOREIGN KEY (receiver_id) REFERENCES users (id),
    CONSTRAINT notifications_level_check CHECK (level IN ('INFO', 'WARNING', 'ERROR', 'SUCCESS'))
);

-- 인덱스 추가
-- 자주 조회되는 컬럼들에 대한 인덱스 추가
-- CREATE INDEX idx_clothes_owner_id ON clothes (owner_id);
-- CREATE INDEX idx_clothes_type ON clothes (type);
-- CREATE INDEX idx_feeds_author_id ON feeds (author_id);
-- CREATE INDEX idx_feeds_weather_id ON feeds (weather_id);
-- CREATE INDEX idx_feeds_created_at ON feeds (created_at);
-- CREATE INDEX idx_feed_likes_feed_id_user_id ON feed_likes (feed_id, user_id);
-- CREATE INDEX idx_feed_comments_feed_id ON feed_comments (feed_id);
-- CREATE INDEX idx_direct_messages_sender_receiver ON direct_messages (sender_id, receiver_id);
-- CREATE INDEX idx_weathers_forecast_at ON weathers (forecast_at);
-- CREATE INDEX idx_notifications_receiver_id ON notifications (receiver_id);