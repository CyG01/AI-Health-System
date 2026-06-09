-- H2 test schema for unit/integration tests
-- Adapted from production init.sql for H2 compatibility

CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    username    VARCHAR(50)  NOT NULL,
    password    VARCHAR(100) NOT NULL,
    phone       VARCHAR(20)  NOT NULL,
    nickname    VARCHAR(50)  DEFAULT NULL,
    avatar      VARCHAR(255) DEFAULT NULL,
    gender      TINYINT      DEFAULT NULL,
    age         INT          DEFAULT NULL,
    role        VARCHAR(20)  DEFAULT 'user',
    status      TINYINT      DEFAULT 1,
    notification_enabled TINYINT DEFAULT 1,
    reminder_time VARCHAR(10) DEFAULT NULL,
    create_time TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    is_deleted  TINYINT      DEFAULT 0,
    version     INT          NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    UNIQUE (username),
    UNIQUE (phone)
);

CREATE TABLE IF NOT EXISTS health_record (
    id              BIGINT  NOT NULL AUTO_INCREMENT,
    user_id         BIGINT  NOT NULL,
    height          INT     NOT NULL,
    weight          INT     NOT NULL,
    target_weight   INT         DEFAULT NULL,
    bmi             DECIMAL(4,1) DEFAULT NULL,
    bmr             INT         DEFAULT NULL,
    daily_calorie   INT         DEFAULT NULL,
    goal            VARCHAR(50) DEFAULT NULL,
    disease_history VARCHAR(500) DEFAULT NULL,
    is_latest       TINYINT     DEFAULT 1,
    create_time     TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS daily_checkin (
    id          BIGINT NOT NULL AUTO_INCREMENT,
    user_id     BIGINT NOT NULL,
    check_date  DATE   NOT NULL,
    check_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE (user_id, check_date)
);

CREATE TABLE IF NOT EXISTS sys_notification (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    user_id     BIGINT      NOT NULL,
    title       VARCHAR(100) NOT NULL,
    content     VARCHAR(500) DEFAULT NULL,
    type        VARCHAR(20)  DEFAULT 'system',
    target_type VARCHAR(20)  DEFAULT NULL,
    target_id   BIGINT       DEFAULT NULL,
    is_read     TINYINT      DEFAULT 0,
    create_time TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);