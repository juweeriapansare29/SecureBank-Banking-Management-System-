CREATE DATABASE IF NOT EXISTS securebank
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE securebank;

CREATE TABLE IF NOT EXISTS bank_accounts (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  account_number VARCHAR(24) NOT NULL UNIQUE,
  owner_name VARCHAR(100) NOT NULL,
  account_type VARCHAR(30) NOT NULL,
  balance DECIMAL(15,2) NOT NULL DEFAULT 0.00,
  overdraft_limit DECIMAL(15,2) NOT NULL DEFAULT 0.00
);

CREATE TABLE IF NOT EXISTS bank_transactions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  reference_code VARCHAR(40) NOT NULL UNIQUE,
  transaction_type VARCHAR(30) NOT NULL,
  amount DECIMAL(15,2) NOT NULL,
  description VARCHAR(255),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  account_id BIGINT NOT NULL,
  CONSTRAINT fk_transaction_account FOREIGN KEY (account_id)
    REFERENCES bank_accounts(id)
);

-- Customer-specific tables for the all-in-one academic demo.
CREATE TABLE IF NOT EXISTS customers (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  full_name VARCHAR(100) NOT NULL,
  email VARCHAR(190) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  role ENUM('CUSTOMER','ADMIN') NOT NULL DEFAULT 'CUSTOMER',
  status ENUM('ACTIVE','LOCKED') NOT NULL DEFAULT 'ACTIVE',
  kyc_status ENUM('NOT_SUBMITTED','PENDING','VERIFIED','REJECTED') NOT NULL DEFAULT 'NOT_SUBMITTED',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS customer_beneficiaries (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  customer_id BIGINT NOT NULL,
  display_name VARCHAR(100) NOT NULL,
  demo_account_ref VARCHAR(100) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_beneficiary_customer FOREIGN KEY(customer_id) REFERENCES customers(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS savings_goals (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  customer_id BIGINT NOT NULL,
  goal_name VARCHAR(100) NOT NULL,
  target_amount DECIMAL(15,2) NOT NULL,
  saved_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
  target_months INT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_goal_customer FOREIGN KEY(customer_id) REFERENCES customers(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS monthly_budgets (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  customer_id BIGINT NOT NULL,
  month_start DATE NOT NULL,
  budget_amount DECIMAL(15,2) NOT NULL,
  UNIQUE KEY uq_customer_month(customer_id, month_start),
  CONSTRAINT fk_budget_customer FOREIGN KEY(customer_id) REFERENCES customers(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS kyc_requests (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  customer_id BIGINT NOT NULL,
  document_type VARCHAR(40) NOT NULL,
  demo_reference VARCHAR(40) NOT NULL,
  status ENUM('PENDING','REVIEWED','VERIFIED','REJECTED') NOT NULL DEFAULT 'PENDING',
  submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_kyc_customer FOREIGN KEY(customer_id) REFERENCES customers(id) ON DELETE CASCADE
);

-- Link accounts to their owner. Existing demo rows can leave customer_id NULL.
ALTER TABLE bank_accounts ADD COLUMN IF NOT EXISTS customer_id BIGINT NULL;
ALTER TABLE bank_accounts ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE bank_accounts ADD CONSTRAINT fk_account_customer FOREIGN KEY(customer_id) REFERENCES customers(id) ON DELETE CASCADE;
ALTER TABLE bank_transactions ADD COLUMN IF NOT EXISTS category VARCHAR(60) NOT NULL DEFAULT 'Other';
ALTER TABLE bank_transactions ADD COLUMN IF NOT EXISTS status VARCHAR(30) NOT NULL DEFAULT 'COMPLETED_DEMO';
