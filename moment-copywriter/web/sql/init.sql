CREATE DATABASE MomentCopywriter;
GO

USE MomentCopywriter;
GO

CREATE TABLE users (
    id INT IDENTITY(1,1) PRIMARY KEY,
    username NVARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(64) NOT NULL,
    password_salt VARCHAR(32) NOT NULL,
    phone NVARCHAR(20) NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'user',
    create_time DATETIME NOT NULL DEFAULT GETDATE()
);
GO

CREATE TABLE user_tags (
    id INT IDENTITY(1,1) PRIMARY KEY,
    user_id INT NOT NULL,
    name NVARCHAR(20) NOT NULL,
    create_time DATETIME NOT NULL DEFAULT GETDATE(),
    CONSTRAINT fk_user_tags_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE,
    CONSTRAINT uq_user_tags_user_name
        UNIQUE (user_id, name)
);
GO

CREATE INDEX idx_user_tags_user_time
    ON user_tags(user_id, create_time ASC);
GO

CREATE TABLE copywriting_records (
    id INT IDENTITY(1,1) PRIMARY KEY,
    user_id INT NULL,
    scene NVARCHAR(200) NOT NULL,
    mood NVARCHAR(50) NULL,
    style NVARCHAR(50) NULL,
    keywords NVARCHAR(500) NULL,
    generated_content NVARCHAR(MAX) NOT NULL,
    ai_model NVARCHAR(100) NULL,
    create_time DATETIME NOT NULL DEFAULT GETDATE(),
    CONSTRAINT fk_copywriting_records_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE SET NULL
);
GO

CREATE TABLE copywriting_record_steps (
    id INT IDENTITY(1,1) PRIMARY KEY,
    record_id INT NOT NULL,
    step_no INT NOT NULL,
    user_message NVARCHAR(500) NULL,
    generated_content NVARCHAR(MAX) NOT NULL,
    create_time DATETIME NOT NULL DEFAULT GETDATE(),
    CONSTRAINT fk_copywriting_record_steps_record
        FOREIGN KEY (record_id) REFERENCES copywriting_records(id)
        ON DELETE CASCADE,
    CONSTRAINT uq_copywriting_record_steps_no
        UNIQUE (record_id, step_no)
);
GO

CREATE INDEX idx_copywriting_record_steps_record_no
    ON copywriting_record_steps(record_id, step_no ASC);
GO

CREATE INDEX idx_copywriting_records_user_time
    ON copywriting_records(user_id, create_time DESC);
GO

CREATE TABLE favorites (
    id INT IDENTITY(1,1) PRIMARY KEY,
    user_id INT NOT NULL,
    record_id INT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT GETDATE(),
    CONSTRAINT fk_favorites_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_favorites_record
        FOREIGN KEY (record_id) REFERENCES copywriting_records(id)
        ON DELETE CASCADE,
    CONSTRAINT uq_favorites_user_record
        UNIQUE (user_id, record_id)
);
GO

CREATE INDEX idx_favorites_user_time
    ON favorites(user_id, create_time DESC);
GO
