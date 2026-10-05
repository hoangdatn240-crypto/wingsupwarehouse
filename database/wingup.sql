
/* =========================================================
   1. TẠO DATABASE
   ========================================================= */

IF DB_ID('wingsup_warehouse') IS NULL
BEGIN
    CREATE DATABASE wingsup_warehouse;
END
GO

USE wingsup_warehouse;
GO


/* =========================================================
   2. BẢNG USERS
   ========================================================= */

IF OBJECT_ID('dbo.users', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.users (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        username VARCHAR(100) NOT NULL UNIQUE,
        password VARCHAR(255) NOT NULL,
        full_name VARCHAR(255),
        email VARCHAR(255),
        role VARCHAR(20) NOT NULL DEFAULT 'USER',
        active BIT NOT NULL DEFAULT 1
    );
END
GO


/* =========================================================
   3. KIỂM TRA / SỬA ROLE
   ========================================================= */

IF EXISTS (
    SELECT 1
    FROM sys.check_constraints
    WHERE name = 'CK__users__role__412EB0B6'
)
BEGIN
    ALTER TABLE dbo.users
    DROP CONSTRAINT CK__users__role__412EB0B6;
END
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.check_constraints
    WHERE name = 'CK_users_role'
)
BEGIN
    ALTER TABLE dbo.users
    ADD CONSTRAINT CK_users_role
    CHECK (role IN ('USER', 'MANAGER', 'ADMIN'));
END
GO


/* =========================================================
   4. BẢNG CATEGORIES - DANH MỤC
   ========================================================= */

IF OBJECT_ID('dbo.categories', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.categories (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        name VARCHAR(255) NOT NULL,
        description NVARCHAR(1000)
    );
END
GO


/* =========================================================
   5. BẢNG SUPPLIERS - NHÀ CUNG CẤP
   ========================================================= */

IF OBJECT_ID('dbo.suppliers', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.suppliers (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        name VARCHAR(255) NOT NULL,
        phone VARCHAR(50),
        email VARCHAR(255),
        address VARCHAR(500)
    );
END
GO


/* =========================================================
   6. BẢNG PRODUCTS - SẢN PHẨM
   ========================================================= */


DROP TABLE IF EXISTS stock_transactions;
DROP TABLE IF EXISTS products;
GO

CREATE TABLE products (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(255) NOT NULL,
    unit NVARCHAR(50),
    price DECIMAL(18,2) DEFAULT 0,
    quantity INT NOT NULL DEFAULT 0,
    min_quantity INT NOT NULL DEFAULT 0,
    category_id BIGINT NULL,
    supplier_id BIGINT NULL,

    CONSTRAINT FK_products_category
        FOREIGN KEY (category_id)
        REFERENCES categories(id),

    CONSTRAINT FK_products_supplier
        FOREIGN KEY (supplier_id)
        REFERENCES suppliers(id)
);
GO




/* =========================================================
   7. BẢNG STOCK_TRANSACTIONS - PHIẾU NHẬP / XUẤT
   ========================================================= */

IF OBJECT_ID('dbo.stock_transactions', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.stock_transactions (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,

        product_id BIGINT NOT NULL,

        type VARCHAR(10) NOT NULL,

        quantity INT NOT NULL,

        note VARCHAR(1000),

        status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

        created_by BIGINT NULL,

        created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),

        CONSTRAINT FK_stock_transactions_product
            FOREIGN KEY (product_id)
            REFERENCES dbo.products(id),

        CONSTRAINT FK_stock_transactions_user
            FOREIGN KEY (created_by)
            REFERENCES dbo.users(id),

        CONSTRAINT CK_stock_transactions_type
            CHECK (type IN ('IN', 'OUT')),

        CONSTRAINT CK_stock_transactions_quantity
            CHECK (quantity > 0),

        CONSTRAINT CK_stock_transactions_status
            CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
    );
END
GO


/* =========================================================
   8. BẢNG LECTURES - BÀI GIẢNG
   ========================================================= */

IF OBJECT_ID('dbo.lectures', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.lectures (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        name VARCHAR(255) NOT NULL,
        link VARCHAR(1000) NOT NULL
    );
END
GO


/* =========================================================
   9. DỮ LIỆU DANH MỤC MẪU
   ========================================================= */

IF NOT EXISTS (SELECT 1 FROM category)
BEGIN
    INSERT INTO category (name, description)
VALUES
(N'Thùng Công Cụ', N'Wedo, Spike, Prime, Essential, alpha set A, alpha set C, beta set, wisebot'),
(N'Máy Tính Bảng Đen', N'Máy Tính Bảng Màu Đen'),
(N'Máy Tính Bảng Trắng', N'Máy Tính Bảng Màu Trắng'),
(N'Phụ Kiện', N'Não, Động Cơ, Cảm Biến')
END
GO



/* =========================================================
   11. DỮ LIỆU SẢN PHẨM MẪU
   ========================================================= */

IF NOT EXISTS (SELECT 1 FROM products)
BEGIN
    INSERT INTO products
    ( name, unit, price, quantity, min_quantity, category_id, supplier_id)
    VALUES
    (
        'LT001',
        'Laptop Acer Aspire',
        'Cái',
        15000000,
        20,
        5,
        1,
        1
    ),
    (
        'PC001',
        'PC Gaming',
        'Bộ',
        25000000,
        10,
        3,
        2,
        1
    ),
    (
        'KB001',
        'Bàn phím cơ',
        'Cái',
        800000,
        20,
        5,
        3,
        2
    );
END
GO


/* =========================================================
   12. DỮ LIỆU BÀI GIẢNG
   ========================================================= */

IF NOT EXISTS (SELECT 1 FROM lectures)
BEGIN
    INSERT INTO lectures (name, link)
    VALUES
    ('Java cơ bản', 'https://www.youtube.com/'),
    ('Spring Boot cơ bản', 'https://spring.io/'),
    ('SQL Server', 'https://learn.microsoft.com/sql/');
END
GO


/* =========================================================
   13. KIỂM TRA TẤT CẢ BẢNG
   ========================================================= */

SELECT TABLE_SCHEMA, TABLE_NAME
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_TYPE = 'BASE TABLE'
ORDER BY TABLE_NAME;
GO


/* =========================================================
   14. KIỂM TRA DỮ LIỆU
   ========================================================= */

SELECT * FROM users;
SELECT * FROM categories;
SELECT * FROM suppliers;
SELECT * FROM products;
SELECT * FROM stock_transactions;
SELECT * FROM lectures;
GO
