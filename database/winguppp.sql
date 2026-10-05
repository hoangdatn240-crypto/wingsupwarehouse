USE wingsup_warehouse;
GO

/* =========================================================
   1. BẢNG YÊU CẦU ĐẶC BIỆT
   ========================================================= */

IF OBJECT_ID('dbo.special_requests', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.special_requests
    (
        id BIGINT IDENTITY(1,1) NOT NULL,
        user_id BIGINT NOT NULL,
        subject NVARCHAR(255) NOT NULL,
        status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
        created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        updated_at DATETIME2 NOT NULL DEFAULT GETDATE(),

        CONSTRAINT PK_special_requests
            PRIMARY KEY (id),

        CONSTRAINT FK_special_requests_user
            FOREIGN KEY (user_id)
            REFERENCES dbo.users(id)
    );
END
GO


/* =========================================================
   2. BẢNG TIN NHẮN YÊU CẦU ĐẶC BIỆT
   ========================================================= */

IF OBJECT_ID('dbo.special_request_messages', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.special_request_messages
    (
        id BIGINT IDENTITY(1,1) NOT NULL,
        request_id BIGINT NOT NULL,
        sender_id BIGINT NOT NULL,
        message NVARCHAR(2000) NOT NULL,
        created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        is_read BIT NOT NULL DEFAULT 0,

        CONSTRAINT PK_special_request_messages
            PRIMARY KEY (id),

        CONSTRAINT FK_special_request_messages_request
            FOREIGN KEY (request_id)
            REFERENCES dbo.special_requests(id)
            ON DELETE CASCADE,

        CONSTRAINT FK_special_request_messages_sender
            FOREIGN KEY (sender_id)
            REFERENCES dbo.users(id)
    );
END
GO


/* =========================================================
   3. INDEX
   ========================================================= */

IF NOT EXISTS
(
    SELECT 1
    FROM sys.indexes
    WHERE name = 'IX_special_requests_user_id'
      AND object_id = OBJECT_ID('dbo.special_requests')
)
BEGIN
    CREATE INDEX IX_special_requests_user_id
    ON dbo.special_requests(user_id);
END
GO


IF NOT EXISTS
(
    SELECT 1
    FROM sys.indexes
    WHERE name = 'IX_special_requests_updated_at'
      AND object_id = OBJECT_ID('dbo.special_requests')
)
BEGIN
    CREATE INDEX IX_special_requests_updated_at
    ON dbo.special_requests(updated_at DESC);
END
GO


IF NOT EXISTS
(
    SELECT 1
    FROM sys.indexes
    WHERE name = 'IX_special_request_messages_request_id'
      AND object_id = OBJECT_ID('dbo.special_request_messages')
)
BEGIN
    CREATE INDEX IX_special_request_messages_request_id
    ON dbo.special_request_messages(request_id);
END
GO


IF NOT EXISTS
(
    SELECT 1
    FROM sys.indexes
    WHERE name = 'IX_special_request_messages_sender_id'
      AND object_id = OBJECT_ID('dbo.special_request_messages')
)
BEGIN
    CREATE INDEX IX_special_request_messages_sender_id
    ON dbo.special_request_messages(sender_id);
END
GO

SELECT *
FROM dbo.special_requests;

SELECT *
FROM dbo.special_request_messages;