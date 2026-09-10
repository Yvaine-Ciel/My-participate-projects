IF DB_ID(N'MomentCopywriter') IS NULL
BEGIN
    CREATE DATABASE MomentCopywriter;
END
GO

USE MomentCopywriter;
GO

IF OBJECT_ID(N'dbo.users', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.users (
        id INT IDENTITY(1,1) PRIMARY KEY,
        username NVARCHAR(50) NOT NULL UNIQUE,
        password_hash VARCHAR(64) NOT NULL,
        password_salt VARCHAR(32) NOT NULL,
        phone NVARCHAR(20) NULL,
        role VARCHAR(20) NOT NULL DEFAULT 'user',
        create_time DATETIME NOT NULL DEFAULT GETDATE()
    );
END
GO

IF OBJECT_ID(N'dbo.user_tags', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.user_tags (
        id INT IDENTITY(1,1) PRIMARY KEY,
        user_id INT NOT NULL,
        name NVARCHAR(20) NOT NULL,
        create_time DATETIME NOT NULL DEFAULT GETDATE(),
        CONSTRAINT fk_user_tags_user
            FOREIGN KEY (user_id) REFERENCES dbo.users(id)
            ON DELETE CASCADE,
        CONSTRAINT uq_user_tags_user_name
            UNIQUE (user_id, name)
    );
END
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = N'idx_user_tags_user_time'
        AND object_id = OBJECT_ID(N'dbo.user_tags')
)
BEGIN
    CREATE INDEX idx_user_tags_user_time
        ON dbo.user_tags(user_id, create_time ASC);
END
GO

IF OBJECT_ID(N'dbo.copywriting_records', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.copywriting_records (
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
            FOREIGN KEY (user_id) REFERENCES dbo.users(id)
            ON DELETE SET NULL
    );
END
GO

DECLARE @keywordsMaxLength SMALLINT;

SELECT @keywordsMaxLength = max_length
FROM sys.columns
WHERE object_id = OBJECT_ID(N'dbo.copywriting_records')
    AND name = N'keywords';

IF @keywordsMaxLength BETWEEN 1 AND 999
BEGIN
    ALTER TABLE dbo.copywriting_records
        ALTER COLUMN keywords NVARCHAR(500) NULL;
END
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = N'idx_copywriting_records_user_time'
        AND object_id = OBJECT_ID(N'dbo.copywriting_records')
)
BEGIN
    CREATE INDEX idx_copywriting_records_user_time
        ON dbo.copywriting_records(user_id, create_time DESC);
END
GO

IF OBJECT_ID(N'dbo.copywriting_record_steps', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.copywriting_record_steps (
        id INT IDENTITY(1,1) PRIMARY KEY,
        record_id INT NOT NULL,
        step_no INT NOT NULL,
        user_message NVARCHAR(500) NULL,
        generated_content NVARCHAR(MAX) NOT NULL,
        create_time DATETIME NOT NULL DEFAULT GETDATE(),
        CONSTRAINT fk_copywriting_record_steps_record
            FOREIGN KEY (record_id) REFERENCES dbo.copywriting_records(id)
            ON DELETE CASCADE,
        CONSTRAINT uq_copywriting_record_steps_no
            UNIQUE (record_id, step_no)
    );
END
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = N'idx_copywriting_record_steps_record_no'
        AND object_id = OBJECT_ID(N'dbo.copywriting_record_steps')
)
BEGIN
    CREATE INDEX idx_copywriting_record_steps_record_no
        ON dbo.copywriting_record_steps(record_id, step_no ASC);
END
GO

IF OBJECT_ID(N'dbo.favorites', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.favorites (
        id INT IDENTITY(1,1) PRIMARY KEY,
        user_id INT NOT NULL,
        record_id INT NOT NULL,
        create_time DATETIME NOT NULL DEFAULT GETDATE(),
        CONSTRAINT fk_favorites_user
            FOREIGN KEY (user_id) REFERENCES dbo.users(id)
            ON DELETE CASCADE,
        CONSTRAINT fk_favorites_record
            FOREIGN KEY (record_id) REFERENCES dbo.copywriting_records(id)
            ON DELETE CASCADE,
        CONSTRAINT uq_favorites_user_record
            UNIQUE (user_id, record_id)
    );
END
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = N'idx_favorites_user_time'
        AND object_id = OBJECT_ID(N'dbo.favorites')
)
BEGIN
    CREATE INDEX idx_favorites_user_time
        ON dbo.favorites(user_id, create_time DESC);
END
GO
