CREATE DATABASE IF NOT EXISTS ibank
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE ibank;

CREATE TABLE IF NOT EXISTS t_user (
  user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  customer_id BIGINT NULL,
  username VARCHAR(50) NOT NULL,
  phone VARCHAR(20) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  pay_password_hash VARCHAR(255) NULL,
  role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',
  status VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
  failed_login_count INT NOT NULL DEFAULT 0,
  locked_until DATETIME NULL,
  last_login_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_username (username),
  UNIQUE KEY uk_user_phone (phone),
  KEY idx_user_customer_id (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_customer (
  customer_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  full_name VARCHAR(80) NOT NULL,
  id_card_no VARCHAR(32) NULL,
  phone VARCHAR(20) NOT NULL,
  email VARCHAR(120) NULL,
  address VARCHAR(255) NULL,
  risk_level VARCHAR(10) NOT NULL DEFAULT 'C2',
  risk_level_source VARCHAR(30) NOT NULL DEFAULT 'SYSTEM',
  risk_level_updated_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_customer_user_id (user_id),
  KEY idx_customer_phone (phone),
  CONSTRAINT fk_customer_user
    FOREIGN KEY (user_id) REFERENCES t_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 't_customer' AND column_name = 'risk_level_source') = 0,
  'ALTER TABLE t_customer ADD COLUMN risk_level_source VARCHAR(30) NOT NULL DEFAULT ''SYSTEM''',
  'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 't_customer' AND column_name = 'risk_level_updated_at') = 0,
  'ALTER TABLE t_customer ADD COLUMN risk_level_updated_at DATETIME NULL',
  'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE t_customer SET risk_level_source = 'SYSTEM' WHERE risk_level_source IS NULL;

CREATE TABLE IF NOT EXISTS t_risk_assessment (
  assessment_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  customer_id BIGINT NOT NULL,
  total_score INT NOT NULL,
  risk_level VARCHAR(10) NOT NULL,
  answers_json TEXT NOT NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'VALID',
  effective_from DATETIME NOT NULL,
  effective_until DATETIME NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_risk_assessment_customer_id (customer_id),
  KEY idx_risk_assessment_effective_until (effective_until),
  CONSTRAINT fk_risk_assessment_customer
    FOREIGN KEY (customer_id) REFERENCES t_customer (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_account (
  account_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  customer_id BIGINT NOT NULL,
  account_no VARCHAR(32) NOT NULL,
  account_type VARCHAR(20) NOT NULL DEFAULT 'SAVING',
  branch_name VARCHAR(120) NOT NULL DEFAULT 'iBank Online Branch',
  available_balance DECIMAL(18,2) NOT NULL DEFAULT 0.00,
  frozen_balance DECIMAL(18,2) NOT NULL DEFAULT 0.00,
  status VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
  default_flag TINYINT(1) NOT NULL DEFAULT 0,
  opened_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_account_no (account_no),
  KEY idx_account_customer_id (customer_id),
  CONSTRAINT fk_account_customer
    FOREIGN KEY (customer_id) REFERENCES t_customer (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_login_log (
  log_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NULL,
  login_identity VARCHAR(80) NOT NULL,
  ip_address VARCHAR(64) NULL,
  user_agent VARCHAR(255) NULL,
  success TINYINT(1) NOT NULL,
  failure_reason VARCHAR(255) NULL,
  risk_score INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_login_log_user_id (user_id),
  KEY idx_login_log_created_at (created_at),
  CONSTRAINT fk_login_log_user
    FOREIGN KEY (user_id) REFERENCES t_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_operation_log (
  log_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NULL,
  operation_type VARCHAR(50) NOT NULL,
  business_id VARCHAR(64) NULL,
  detail VARCHAR(500) NULL,
  ip_address VARCHAR(64) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_operation_log_user_id (user_id),
  KEY idx_operation_log_created_at (created_at),
  CONSTRAINT fk_operation_log_user
    FOREIGN KEY (user_id) REFERENCES t_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_transaction (
  transaction_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  transaction_no VARCHAR(40) NOT NULL,
  customer_id BIGINT NOT NULL,
  from_account_id BIGINT NULL,
  to_account_id BIGINT NULL,
  txn_type VARCHAR(30) NOT NULL,
  amount DECIMAL(18,2) NOT NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
  risk_score INT NOT NULL DEFAULT 0,
  remark VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_transaction_no (transaction_no),
  KEY idx_transaction_customer_id (customer_id),
  KEY idx_transaction_created_at (created_at),
  CONSTRAINT fk_transaction_customer
    FOREIGN KEY (customer_id) REFERENCES t_customer (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_ledger_entry (
  ledger_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  transaction_id BIGINT NOT NULL,
  account_id BIGINT NOT NULL,
  direction VARCHAR(10) NOT NULL,
  amount DECIMAL(18,2) NOT NULL,
  balance_after DECIMAL(18,2) NOT NULL,
  summary VARCHAR(255) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_ledger_account_id (account_id),
  KEY idx_ledger_created_at (created_at),
  CONSTRAINT fk_ledger_transaction
    FOREIGN KEY (transaction_id) REFERENCES t_transaction (transaction_id),
  CONSTRAINT fk_ledger_account
    FOREIGN KEY (account_id) REFERENCES t_account (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_bill_payment (
  payment_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  transaction_id BIGINT NOT NULL,
  customer_id BIGINT NOT NULL,
  account_id BIGINT NOT NULL,
  payment_type VARCHAR(30) NOT NULL,
  institution_name VARCHAR(120) NOT NULL,
  payer_no VARCHAR(40) NOT NULL,
  billing_month VARCHAR(7) NOT NULL,
  amount DECIMAL(18,2) NOT NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'SUCCESS',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_bill_payment_customer_id (customer_id),
  KEY idx_bill_payment_account_id (account_id),
  KEY idx_bill_payment_created_at (created_at),
  CONSTRAINT fk_bill_payment_transaction
    FOREIGN KEY (transaction_id) REFERENCES t_transaction (transaction_id),
  CONSTRAINT fk_bill_payment_customer
    FOREIGN KEY (customer_id) REFERENCES t_customer (customer_id),
  CONSTRAINT fk_bill_payment_account
    FOREIGN KEY (account_id) REFERENCES t_account (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_wealth_product (
  product_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  product_code VARCHAR(40) NOT NULL,
  product_name VARCHAR(120) NOT NULL,
  risk_level VARCHAR(10) NOT NULL,
  product_type VARCHAR(30) NOT NULL DEFAULT 'FIXED_TERM',
  expected_rate DECIMAL(8,4) NOT NULL,
  period_days INT NOT NULL,
  confirm_days INT NOT NULL DEFAULT 0,
  arrival_days INT NOT NULL DEFAULT 1,
  allow_early_redeem TINYINT(1) NOT NULL DEFAULT 0,
  min_amount DECIMAL(18,2) NOT NULL,
  max_amount DECIMAL(18,2) NOT NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'ON_SALE',
  description VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_wealth_product_code (product_code),
  KEY idx_wealth_product_status (status),
  KEY idx_wealth_product_risk_level (risk_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 't_wealth_product' AND column_name = 'product_type') = 0,
  'ALTER TABLE t_wealth_product ADD COLUMN product_type VARCHAR(30) NOT NULL DEFAULT ''FIXED_TERM'' AFTER risk_level',
  'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 't_wealth_product' AND column_name = 'confirm_days') = 0,
  'ALTER TABLE t_wealth_product ADD COLUMN confirm_days INT NOT NULL DEFAULT 0 AFTER period_days',
  'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 't_wealth_product' AND column_name = 'arrival_days') = 0,
  'ALTER TABLE t_wealth_product ADD COLUMN arrival_days INT NOT NULL DEFAULT 1 AFTER confirm_days',
  'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 't_wealth_product' AND column_name = 'allow_early_redeem') = 0,
  'ALTER TABLE t_wealth_product ADD COLUMN allow_early_redeem TINYINT(1) NOT NULL DEFAULT 0 AFTER arrival_days',
  'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS t_wealth_holding (
  holding_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  customer_id BIGINT NOT NULL,
  account_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  buy_transaction_id BIGINT NOT NULL,
  redeem_transaction_id BIGINT NULL,
  buy_order_id BIGINT NULL,
  redeem_order_id BIGINT NULL,
  principal DECIMAL(18,2) NOT NULL,
  expected_rate DECIMAL(8,4) NOT NULL,
  buy_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  value_date DATE NULL,
  maturity_date DATE NULL,
  redeem_time DATETIME NULL,
  current_income DECIMAL(18,2) NOT NULL DEFAULT 0.00,
  estimated_value DECIMAL(18,2) NOT NULL DEFAULT 0.00,
  status VARCHAR(30) NOT NULL DEFAULT 'HOLDING',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_wealth_holding_customer_id (customer_id),
  KEY idx_wealth_holding_account_id (account_id),
  KEY idx_wealth_holding_status (status),
  CONSTRAINT fk_wealth_holding_customer
    FOREIGN KEY (customer_id) REFERENCES t_customer (customer_id),
  CONSTRAINT fk_wealth_holding_account
    FOREIGN KEY (account_id) REFERENCES t_account (account_id),
  CONSTRAINT fk_wealth_holding_product
    FOREIGN KEY (product_id) REFERENCES t_wealth_product (product_id),
  CONSTRAINT fk_wealth_holding_buy_transaction
    FOREIGN KEY (buy_transaction_id) REFERENCES t_transaction (transaction_id),
  CONSTRAINT fk_wealth_holding_redeem_transaction
    FOREIGN KEY (redeem_transaction_id) REFERENCES t_transaction (transaction_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 't_wealth_holding' AND column_name = 'buy_order_id') = 0,
  'ALTER TABLE t_wealth_holding ADD COLUMN buy_order_id BIGINT NULL AFTER redeem_transaction_id',
  'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 't_wealth_holding' AND column_name = 'redeem_order_id') = 0,
  'ALTER TABLE t_wealth_holding ADD COLUMN redeem_order_id BIGINT NULL AFTER buy_order_id',
  'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 't_wealth_holding' AND column_name = 'value_date') = 0,
  'ALTER TABLE t_wealth_holding ADD COLUMN value_date DATE NULL AFTER buy_time',
  'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 't_wealth_holding' AND column_name = 'maturity_date') = 0,
  'ALTER TABLE t_wealth_holding ADD COLUMN maturity_date DATE NULL AFTER value_date',
  'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 't_wealth_holding' AND column_name = 'estimated_value') = 0,
  'ALTER TABLE t_wealth_holding ADD COLUMN estimated_value DECIMAL(18,2) NOT NULL DEFAULT 0.00 AFTER current_income',
  'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS t_wealth_order (
  order_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_no VARCHAR(40) NOT NULL,
  customer_id BIGINT NOT NULL,
  account_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  holding_id BIGINT NULL,
  transaction_id BIGINT NULL,
  order_type VARCHAR(20) NOT NULL,
  amount DECIMAL(18,2) NOT NULL,
  confirmed_amount DECIMAL(18,2) NULL,
  income_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
  status VARCHAR(40) NOT NULL,
  submit_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  confirm_time DATETIME NULL,
  value_date DATE NULL,
  maturity_date DATE NULL,
  expected_arrival_date DATE NULL,
  completed_time DATETIME NULL,
  fail_reason VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_wealth_order_no (order_no),
  KEY idx_wealth_order_customer_id (customer_id),
  KEY idx_wealth_order_status (status),
  KEY idx_wealth_order_type_status (order_type, status),
  KEY idx_wealth_order_holding_id (holding_id),
  CONSTRAINT fk_wealth_order_customer
    FOREIGN KEY (customer_id) REFERENCES t_customer (customer_id),
  CONSTRAINT fk_wealth_order_account
    FOREIGN KEY (account_id) REFERENCES t_account (account_id),
  CONSTRAINT fk_wealth_order_product
    FOREIGN KEY (product_id) REFERENCES t_wealth_product (product_id),
  CONSTRAINT fk_wealth_order_holding
    FOREIGN KEY (holding_id) REFERENCES t_wealth_holding (holding_id),
  CONSTRAINT fk_wealth_order_transaction
    FOREIGN KEY (transaction_id) REFERENCES t_transaction (transaction_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_wealth_order_confirm (
  confirm_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  transaction_id BIGINT NOT NULL,
  customer_id BIGINT NOT NULL,
  account_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  amount DECIMAL(18,2) NOT NULL,
  customer_risk_level VARCHAR(10) NOT NULL,
  product_risk_level VARCHAR(10) NOT NULL,
  match_result VARCHAR(30) NOT NULL,
  disclosure_version VARCHAR(30) NOT NULL,
  product_disclosure_checked TINYINT(1) NOT NULL DEFAULT 0,
  non_deposit_checked TINYINT(1) NOT NULL DEFAULT 0,
  yield_not_guaranteed_checked TINYINT(1) NOT NULL DEFAULT 0,
  account_confirmed TINYINT(1) NOT NULL DEFAULT 0,
  ip_address VARCHAR(64) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_wealth_confirm_customer_id (customer_id),
  KEY idx_wealth_confirm_transaction_id (transaction_id),
  KEY idx_wealth_confirm_product_id (product_id),
  CONSTRAINT fk_wealth_confirm_transaction
    FOREIGN KEY (transaction_id) REFERENCES t_transaction (transaction_id),
  CONSTRAINT fk_wealth_confirm_customer
    FOREIGN KEY (customer_id) REFERENCES t_customer (customer_id),
  CONSTRAINT fk_wealth_confirm_account
    FOREIGN KEY (account_id) REFERENCES t_account (account_id),
  CONSTRAINT fk_wealth_confirm_product
    FOREIGN KEY (product_id) REFERENCES t_wealth_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO t_wealth_product
  (product_code, product_name, risk_level, expected_rate, period_days, min_amount, max_amount, status, description)
VALUES
  ('WP-R1-030', '安心现金增强 30 天', 'R1', 0.0180, 30, 1000.00, 500000.00, 'ON_SALE', '低风险短期限现金管理产品，适合保守型客户。'),
  ('WP-R2-090', '稳健月月享 90 天', 'R2', 0.0260, 90, 1000.00, 800000.00, 'ON_SALE', '稳健收益型产品，适合中短期资金配置。'),
  ('WP-R3-180', '平衡增利 180 天', 'R3', 0.0360, 180, 5000.00, 1000000.00, 'ON_SALE', '平衡型理财产品，收益和波动适中。'),
  ('WP-R4-365', '成长精选 365 天', 'R4', 0.0520, 365, 10000.00, 1500000.00, 'ON_SALE', '成长型资产配置产品，适合较高风险承受能力客户。'),
  ('WP-R5-730', '进取优选 730 天', 'R5', 0.0680, 730, 20000.00, 2000000.00, 'ON_SALE', '进取型长期产品，收益弹性较高，风险也较高。')
ON DUPLICATE KEY UPDATE
  product_code = product_code;

CREATE TABLE IF NOT EXISTS t_risk_limit_rule (
  rule_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  rule_code VARCHAR(60) NOT NULL,
  txn_type VARCHAR(30) NOT NULL,
  customer_risk_level VARCHAR(10) NOT NULL,
  single_limit DECIMAL(18,2) NOT NULL,
  daily_amount_limit DECIMAL(18,2) NOT NULL,
  daily_count_limit INT NOT NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_risk_limit_rule_code (rule_code),
  UNIQUE KEY uk_risk_limit_rule_scope (txn_type, customer_risk_level),
  KEY idx_risk_limit_rule_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_customer_limit (
  customer_limit_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  customer_id BIGINT NOT NULL,
  txn_type VARCHAR(30) NOT NULL,
  single_limit DECIMAL(18,2) NOT NULL,
  daily_amount_limit DECIMAL(18,2) NOT NULL,
  daily_count_limit INT NOT NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_customer_limit_scope (customer_id, txn_type),
  CONSTRAINT fk_customer_limit_customer
    FOREIGN KEY (customer_id) REFERENCES t_customer (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_risk_limit_usage (
  usage_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  customer_id BIGINT NOT NULL,
  txn_date DATE NOT NULL,
  txn_type VARCHAR(30) NOT NULL,
  used_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
  used_count INT NOT NULL DEFAULT 0,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_risk_limit_usage_scope (customer_id, txn_date, txn_type),
  CONSTRAINT fk_risk_limit_usage_customer
    FOREIGN KEY (customer_id) REFERENCES t_customer (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_risk_event (
  event_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  customer_id BIGINT NOT NULL,
  account_id BIGINT NULL,
  transaction_no VARCHAR(40) NOT NULL,
  txn_type VARCHAR(30) NOT NULL,
  amount DECIMAL(18,2) NOT NULL,
  risk_score INT NOT NULL DEFAULT 0,
  risk_level VARCHAR(20) NOT NULL,
  decision VARCHAR(20) NOT NULL,
  hit_rules VARCHAR(500) NULL,
  reason VARCHAR(500) NULL,
  ip_address VARCHAR(64) NULL,
  handle_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
  handle_result VARCHAR(30) NULL,
  handler_admin_user_id BIGINT NULL,
  handle_note VARCHAR(500) NULL,
  handled_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_risk_event_customer_id (customer_id),
  KEY idx_risk_event_created_at (created_at),
  KEY idx_risk_event_decision (decision),
  KEY idx_risk_event_handle_status (handle_status),
  CONSTRAINT fk_risk_event_customer
    FOREIGN KEY (customer_id) REFERENCES t_customer (customer_id),
  CONSTRAINT fk_risk_event_account
    FOREIGN KEY (account_id) REFERENCES t_account (account_id),
  CONSTRAINT fk_risk_event_handler
    FOREIGN KEY (handler_admin_user_id) REFERENCES t_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 't_risk_event' AND column_name = 'handle_status') = 0,
  'ALTER TABLE t_risk_event ADD COLUMN handle_status VARCHAR(30) NOT NULL DEFAULT ''PENDING''',
  'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 't_risk_event' AND column_name = 'handle_result') = 0,
  'ALTER TABLE t_risk_event ADD COLUMN handle_result VARCHAR(30) NULL',
  'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 't_risk_event' AND column_name = 'handler_admin_user_id') = 0,
  'ALTER TABLE t_risk_event ADD COLUMN handler_admin_user_id BIGINT NULL',
  'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 't_risk_event' AND column_name = 'handle_note') = 0,
  'ALTER TABLE t_risk_event ADD COLUMN handle_note VARCHAR(500) NULL',
  'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 't_risk_event' AND column_name = 'handled_at') = 0,
  'ALTER TABLE t_risk_event ADD COLUMN handled_at DATETIME NULL',
  'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS t_risk_action_log (
  action_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  event_id BIGINT NOT NULL,
  admin_user_id BIGINT NOT NULL,
  action_type VARCHAR(40) NOT NULL,
  before_status VARCHAR(40) NULL,
  after_status VARCHAR(40) NULL,
  note VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_risk_action_event_id (event_id),
  KEY idx_risk_action_admin_user_id (admin_user_id),
  KEY idx_risk_action_created_at (created_at),
  CONSTRAINT fk_risk_action_event
    FOREIGN KEY (event_id) REFERENCES t_risk_event (event_id),
  CONSTRAINT fk_risk_action_admin_user
    FOREIGN KEY (admin_user_id) REFERENCES t_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_risk_dataset_batch (
  batch_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  batch_code VARCHAR(80) NOT NULL,
  dataset_name VARCHAR(60) NOT NULL,
  source_file VARCHAR(500) NOT NULL,
  import_mode VARCHAR(40) NOT NULL DEFAULT 'BUSINESS_AND_TRAINING',
  row_limit INT NULL,
  imported_rows INT NOT NULL DEFAULT 0,
  skipped_rows INT NOT NULL DEFAULT 0,
  fraud_rows INT NOT NULL DEFAULT 0,
  flagged_rows INT NOT NULL DEFAULT 0,
  status VARCHAR(30) NOT NULL DEFAULT 'RUNNING',
  error_message VARCHAR(1000) NULL,
  metadata_json TEXT NULL,
  started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  completed_at DATETIME NULL,
  UNIQUE KEY uk_risk_dataset_batch_code (batch_code),
  KEY idx_risk_dataset_batch_name (dataset_name),
  KEY idx_risk_dataset_batch_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_risk_external_entity_map (
  map_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  dataset_name VARCHAR(60) NOT NULL,
  external_entity_id VARCHAR(80) NOT NULL,
  entity_type VARCHAR(30) NOT NULL,
  user_id BIGINT NOT NULL,
  customer_id BIGINT NOT NULL,
  account_id BIGINT NOT NULL,
  account_no VARCHAR(32) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_risk_external_entity (dataset_name, external_entity_id),
  KEY idx_risk_external_customer (customer_id),
  KEY idx_risk_external_account (account_id),
  CONSTRAINT fk_risk_external_user
    FOREIGN KEY (user_id) REFERENCES t_user (user_id),
  CONSTRAINT fk_risk_external_customer
    FOREIGN KEY (customer_id) REFERENCES t_customer (customer_id),
  CONSTRAINT fk_risk_external_account
    FOREIGN KEY (account_id) REFERENCES t_account (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_risk_training_sample (
  sample_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  batch_id BIGINT NOT NULL,
  source_dataset VARCHAR(60) NOT NULL,
  source_row_no INT NOT NULL,
  source_step INT NOT NULL,
  source_type VARCHAR(30) NOT NULL,
  external_origin_id VARCHAR(80) NOT NULL,
  external_dest_id VARCHAR(80) NOT NULL,
  transaction_id BIGINT NULL,
  transaction_no VARCHAR(40) NULL,
  customer_id BIGINT NULL,
  from_account_id BIGINT NULL,
  to_account_id BIGINT NULL,
  txn_type VARCHAR(30) NOT NULL,
  amount DECIMAL(18,2) NOT NULL,
  event_time DATETIME NOT NULL,
  old_balance_origin DECIMAL(18,2) NULL,
  new_balance_origin DECIMAL(18,2) NULL,
  old_balance_dest DECIMAL(18,2) NULL,
  new_balance_dest DECIMAL(18,2) NULL,
  label_fraud TINYINT(1) NOT NULL DEFAULT 0,
  label_flagged_rule TINYINT(1) NOT NULL DEFAULT 0,
  label_source VARCHAR(30) NOT NULL DEFAULT 'PAYSIM',
  feature_json TEXT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_risk_training_batch_row (batch_id, source_row_no),
  KEY idx_risk_training_transaction (transaction_id),
  KEY idx_risk_training_customer (customer_id),
  KEY idx_risk_training_label (label_fraud, label_flagged_rule),
  KEY idx_risk_training_time (event_time),
  CONSTRAINT fk_risk_training_batch
    FOREIGN KEY (batch_id) REFERENCES t_risk_dataset_batch (batch_id),
  CONSTRAINT fk_risk_training_transaction
    FOREIGN KEY (transaction_id) REFERENCES t_transaction (transaction_id),
  CONSTRAINT fk_risk_training_customer
    FOREIGN KEY (customer_id) REFERENCES t_customer (customer_id),
  CONSTRAINT fk_risk_training_from_account
    FOREIGN KEY (from_account_id) REFERENCES t_account (account_id),
  CONSTRAINT fk_risk_training_to_account
    FOREIGN KEY (to_account_id) REFERENCES t_account (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_risk_model_score (
  score_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  sample_id BIGINT NULL,
  transaction_id BIGINT NULL,
  transaction_no VARCHAR(40) NULL,
  model_version VARCHAR(60) NOT NULL,
  feature_version VARCHAR(60) NOT NULL,
  risk_score INT NOT NULL,
  risk_probability DECIMAL(10,8) NULL,
  decision VARCHAR(30) NOT NULL,
  reason_json TEXT NULL,
  scored_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_risk_model_sample (sample_id),
  KEY idx_risk_model_transaction (transaction_id),
  KEY idx_risk_model_version (model_version),
  CONSTRAINT fk_risk_model_sample
    FOREIGN KEY (sample_id) REFERENCES t_risk_training_sample (sample_id),
  CONSTRAINT fk_risk_model_transaction
    FOREIGN KEY (transaction_id) REFERENCES t_transaction (transaction_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_risk_graph_dataset_batch (
  graph_batch_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  batch_code VARCHAR(80) NOT NULL,
  dataset_name VARCHAR(60) NOT NULL,
  source_file VARCHAR(500) NOT NULL,
  source_format VARCHAR(30) NOT NULL,
  row_limit INT NULL,
  node_rows INT NOT NULL DEFAULT 0,
  edge_rows INT NOT NULL DEFAULT 0,
  skipped_rows INT NOT NULL DEFAULT 0,
  normal_edges INT NOT NULL DEFAULT 0,
  fraud_edges INT NOT NULL DEFAULT 0,
  flagged_edges INT NOT NULL DEFAULT 0,
  status VARCHAR(30) NOT NULL DEFAULT 'RUNNING',
  error_message VARCHAR(1000) NULL,
  metadata_json TEXT NULL,
  started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  completed_at DATETIME NULL,
  UNIQUE KEY uk_risk_graph_batch_code (batch_code),
  KEY idx_risk_graph_batch_dataset (dataset_name),
  KEY idx_risk_graph_batch_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_risk_graph_node (
  graph_node_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  dataset_name VARCHAR(60) NOT NULL,
  external_node_id VARCHAR(180) NOT NULL,
  node_type VARCHAR(30) NOT NULL,
  display_name VARCHAR(180) NULL,
  first_batch_id BIGINT NULL,
  in_degree INT NOT NULL DEFAULT 0,
  out_degree INT NOT NULL DEFAULT 0,
  fraud_in_degree INT NOT NULL DEFAULT 0,
  fraud_out_degree INT NOT NULL DEFAULT 0,
  total_in_amount DECIMAL(20,2) NOT NULL DEFAULT 0.00,
  total_out_amount DECIMAL(20,2) NOT NULL DEFAULT 0.00,
  feature_json TEXT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_risk_graph_node_dataset_external (dataset_name, external_node_id),
  KEY idx_risk_graph_node_dataset_type (dataset_name, node_type),
  KEY idx_risk_graph_node_in_degree (in_degree),
  KEY idx_risk_graph_node_out_degree (out_degree),
  CONSTRAINT fk_risk_graph_node_first_batch
    FOREIGN KEY (first_batch_id) REFERENCES t_risk_graph_dataset_batch (graph_batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_risk_graph_edge (
  graph_edge_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  batch_id BIGINT NOT NULL,
  dataset_name VARCHAR(60) NOT NULL,
  source_row_no INT NOT NULL,
  source_edge_id VARCHAR(120) NOT NULL,
  from_node_id BIGINT NOT NULL,
  to_node_id BIGINT NOT NULL,
  from_external_id VARCHAR(180) NOT NULL,
  to_external_id VARCHAR(180) NOT NULL,
  edge_type VARCHAR(60) NOT NULL,
  amount DECIMAL(20,2) NOT NULL,
  currency VARCHAR(12) NULL,
  paid_amount DECIMAL(20,2) NULL,
  paid_currency VARCHAR(12) NULL,
  received_amount DECIMAL(20,2) NULL,
  received_currency VARCHAR(12) NULL,
  event_time DATETIME NULL,
  source_step INT NULL,
  label_fraud TINYINT(1) NOT NULL DEFAULT 0,
  label_rule TINYINT(1) NOT NULL DEFAULT 0,
  label_source VARCHAR(40) NOT NULL,
  typology VARCHAR(80) NULL,
  feature_json TEXT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_risk_graph_edge_batch_row (batch_id, source_row_no),
  KEY idx_risk_graph_edge_dataset_label (dataset_name, label_fraud, label_rule),
  KEY idx_risk_graph_edge_from_node (from_node_id),
  KEY idx_risk_graph_edge_to_node (to_node_id),
  KEY idx_risk_graph_edge_time (event_time),
  KEY idx_risk_graph_edge_type (edge_type),
  KEY idx_risk_graph_edge_amount (amount),
  CONSTRAINT fk_risk_graph_edge_batch
    FOREIGN KEY (batch_id) REFERENCES t_risk_graph_dataset_batch (graph_batch_id),
  CONSTRAINT fk_risk_graph_edge_from_node
    FOREIGN KEY (from_node_id) REFERENCES t_risk_graph_node (graph_node_id),
  CONSTRAINT fk_risk_graph_edge_to_node
    FOREIGN KEY (to_node_id) REFERENCES t_risk_graph_node (graph_node_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_risk_graph_model_score (
  score_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  graph_edge_id BIGINT NOT NULL,
  model_version VARCHAR(80) NOT NULL,
  feature_version VARCHAR(80) NOT NULL,
  risk_score INT NOT NULL,
  risk_probability DECIMAL(10,8) NOT NULL,
  decision VARCHAR(30) NOT NULL,
  review_threshold DECIMAL(10,8) NULL,
  block_threshold DECIMAL(10,8) NULL,
  reason_json TEXT NULL,
  scored_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_risk_graph_model_edge_version (graph_edge_id, model_version),
  KEY idx_risk_graph_model_version_decision (model_version, decision),
  KEY idx_risk_graph_model_decision_score (model_version, decision, risk_score),
  KEY idx_risk_graph_model_score (model_version, risk_score),
  CONSTRAINT fk_risk_graph_model_edge
    FOREIGN KEY (graph_edge_id) REFERENCES t_risk_graph_edge (graph_edge_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_risk_graph_model_governance (
  model_version VARCHAR(80) PRIMARY KEY,
  model_role VARCHAR(30) NOT NULL DEFAULT 'EXPERIMENT',
  lifecycle_status VARCHAR(30) NOT NULL DEFAULT 'EVALUATING',
  online_mode VARCHAR(30) NOT NULL DEFAULT 'OFFLINE_REVIEW',
  is_operational TINYINT(1) NOT NULL DEFAULT 0,
  governance_note VARCHAR(500) NULL,
  promoted_by_admin_user_id BIGINT NULL,
  promoted_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_risk_graph_model_governance_role (model_role),
  KEY idx_risk_graph_model_governance_operational (is_operational),
  CONSTRAINT fk_risk_graph_model_governance_admin
    FOREIGN KEY (promoted_by_admin_user_id) REFERENCES t_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_risk_graph_review_case (
  case_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  graph_edge_id BIGINT NOT NULL,
  model_version VARCHAR(80) NOT NULL,
  case_type VARCHAR(40) NOT NULL,
  case_status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
  model_decision VARCHAR(30) NOT NULL,
  business_decision VARCHAR(40) NOT NULL,
  risk_score INT NOT NULL,
  risk_probability DECIMAL(10,8) NOT NULL,
  label_fraud TINYINT(1) NOT NULL DEFAULT 0,
  priority INT NOT NULL DEFAULT 50,
  reason VARCHAR(1000) NULL,
  review_result VARCHAR(40) NULL,
  review_note VARCHAR(1000) NULL,
  reviewed_by_admin_id BIGINT NULL,
  reviewed_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_risk_graph_case_edge_model (graph_edge_id, model_version),
  KEY idx_risk_graph_case_model_status (model_version, case_status),
  KEY idx_risk_graph_case_type_status (case_type, case_status),
  KEY idx_risk_graph_case_priority (priority, risk_score),
  CONSTRAINT fk_risk_graph_case_edge
    FOREIGN KEY (graph_edge_id) REFERENCES t_risk_graph_edge (graph_edge_id),
  CONSTRAINT fk_risk_graph_case_reviewer
    FOREIGN KEY (reviewed_by_admin_id) REFERENCES t_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_admin_role (
  role_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_code VARCHAR(50) NOT NULL,
  role_name VARCHAR(80) NOT NULL,
  description VARCHAR(255) NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_admin_role_code (role_code),
  KEY idx_admin_role_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_admin_permission (
  permission_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  permission_code VARCHAR(80) NOT NULL,
  permission_name VARCHAR(120) NOT NULL,
  permission_group VARCHAR(50) NOT NULL,
  description VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_admin_permission_code (permission_code),
  KEY idx_admin_permission_group (permission_group)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_admin_role_permission (
  role_id BIGINT NOT NULL,
  permission_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (role_id, permission_id),
  CONSTRAINT fk_admin_role_permission_role
    FOREIGN KEY (role_id) REFERENCES t_admin_role (role_id),
  CONSTRAINT fk_admin_role_permission_permission
    FOREIGN KEY (permission_id) REFERENCES t_admin_permission (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_admin_user_role (
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id, role_id),
  CONSTRAINT fk_admin_user_role_user
    FOREIGN KEY (user_id) REFERENCES t_user (user_id),
  CONSTRAINT fk_admin_user_role_role
    FOREIGN KEY (role_id) REFERENCES t_admin_role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO t_admin_permission
  (permission_code, permission_name, permission_group, description)
VALUES
  ('ADMIN_DASHBOARD_VIEW', '查看后台首页', 'DASHBOARD', '访问后台首页和基础指标'),
  ('CUSTOMER_VIEW', '查看客户信息', 'CUSTOMER', '查看客户列表和客户详情'),
  ('CUSTOMER_RISK_ADJUST', '调整客户风险等级', 'CUSTOMER', '调整客户风险承受能力等级'),
  ('RISK_EVENT_VIEW', '查看风控事件', 'RISK', '查看风控事件和事件详情'),
  ('RISK_EVENT_HANDLE', '处置风控事件', 'RISK', '提交风控事件处置、冻结或解冻账户'),
  ('RISK_GRAPH_SCORE_VIEW', '查看GNN风险评分', 'RISK', '查看图神经网络模型评分、图边证据和风险分层'),
  ('RISK_GRAPH_CASE_VIEW', '查看GNN复核队列', 'RISK', '查看模型高风险、标签冲突和重点调查样本'),
  ('RISK_GRAPH_CASE_HANDLE', '处理GNN复核样本', 'RISK', '记录人工复核结论并沉淀模型反馈样本'),
  ('RISK_RULE_VIEW', '查看风控规则', 'RISK', '查看交易限额风控规则'),
  ('RISK_RULE_UPDATE', '修改风控规则', 'RISK', '修改交易限额风控规则'),
  ('WEALTH_PRODUCT_VIEW', '查看理财产品后台', 'WEALTH', '查看后台理财产品列表'),
  ('WEALTH_PRODUCT_UPDATE', '修改理财产品', 'WEALTH', '上下架或修改理财产品参数'),
  ('WEALTH_SETTLEMENT_VIEW', '查看理财清算', 'WEALTH', '查看理财订单确认、赎回到账和清算队列'),
  ('WEALTH_SETTLEMENT_RUN', '执行理财清算', 'WEALTH', '执行理财申购确认和赎回到账批处理'),
  ('ADMIN_ALERT_VIEW', '查看后台消息', 'MESSAGE', '查看后台消息告警和待办事项'),
  ('ADMIN_ALERT_HANDLE', '处理后台消息', 'MESSAGE', '确认、解决或关闭后台消息告警'),
  ('TICKET_VIEW', '查看服务工单', 'SERVICE', '查看客户服务工单和交易争议'),
  ('TICKET_HANDLE', '处理服务工单', 'SERVICE', '受理、回复、解决或关闭服务工单'),
  ('TICKET_ASSIGN', '转派服务工单', 'SERVICE', '将服务工单转派给其他责任角色'),
  ('TICKET_ALL_VIEW', '查看全部服务工单', 'SERVICE', '跨角色查看全部客户服务工单'),
  ('RECONCILIATION_VIEW', '查看账务对账', 'ACCOUNTING', '查看对账批次和对账详情'),
  ('RECONCILIATION_RUN', '发起账务对账', 'ACCOUNTING', '发起指定日期对账批次'),
  ('RECONCILIATION_ITEM_VIEW', '查看对账异常', 'ACCOUNTING', '查看对账异常处理中心和异常详情'),
  ('RECONCILIATION_ITEM_HANDLE', '处理对账异常', 'ACCOUNTING', '接手、确认、关闭或修复对账异常'),
  ('ADJUSTMENT_VIEW', '查看调账申请', 'ACCOUNTING', '查看调账申请列表和详情'),
  ('ADJUSTMENT_CREATE', '创建调账申请', 'ACCOUNTING', '从确认异常发起调账申请'),
  ('ADJUSTMENT_REVIEW', '复核调账申请', 'ACCOUNTING', '复核通过或驳回调账申请'),
  ('ADJUSTMENT_EXECUTE', '执行调账', 'ACCOUNTING', '执行已复核通过的调账申请'),
  ('ADMIN_AUDIT_VIEW', '查看审计日志', 'SECURITY', '查看后台审计日志'),
  ('ADMIN_USER_VIEW', '查看管理员账号', 'SECURITY', '查看后台管理员账号和角色分配'),
  ('ADMIN_USER_MANAGE', '管理管理员账号', 'SECURITY', '管理后台管理员账号'),
  ('ROLE_PERMISSION_VIEW', '查看角色权限', 'SECURITY', '查看后台角色与权限清单'),
  ('ROLE_PERMISSION_MANAGE', '管理角色权限', 'SECURITY', '管理后台角色与权限')
ON DUPLICATE KEY UPDATE
  permission_name = VALUES(permission_name),
  permission_group = VALUES(permission_group),
  description = VALUES(description);

INSERT INTO t_admin_role
  (role_code, role_name, description, status)
VALUES
  ('SUPER_ADMIN', '超级管理员', '系统最高权限，管理后台基础配置和全部业务模块。', 'ACTIVE'),
  ('CUSTOMER_OPERATOR', '客户运营', '查看客户信息和基础运营数据。', 'ACTIVE'),
  ('RISK_OPERATOR', '风控运营', '查看并处置风控事件，调整客户风险等级。', 'ACTIVE'),
  ('RISK_MANAGER', '风控规则管理员', '管理交易限额和风控规则。', 'ACTIVE'),
  ('PRODUCT_MANAGER', '理财产品管理员', '管理理财产品参数和上下架。', 'ACTIVE'),
  ('ACCOUNTING_OPERATOR', '账务运营', '发起对账、处理对账异常并创建调账申请。', 'ACTIVE'),
  ('ACCOUNTING_REVIEWER', '账务复核员', '复核并执行调账申请。', 'ACTIVE'),
  ('AUDITOR', '审计员', '只读查看后台关键业务数据。', 'ACTIVE')
ON DUPLICATE KEY UPDATE
  role_name = VALUES(role_name),
  description = VALUES(description),
  status = VALUES(status);

INSERT INTO t_admin_role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM t_admin_role r
JOIN t_admin_permission p
WHERE r.role_code = 'SUPER_ADMIN'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

INSERT INTO t_admin_role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM t_admin_role r
JOIN t_admin_permission p ON p.permission_code IN ('ADMIN_DASHBOARD_VIEW', 'CUSTOMER_VIEW',
  'TICKET_VIEW', 'TICKET_HANDLE', 'TICKET_ASSIGN')
WHERE r.role_code = 'CUSTOMER_OPERATOR'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

INSERT INTO t_admin_role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM t_admin_role r
JOIN t_admin_permission p ON p.permission_code IN ('ADMIN_DASHBOARD_VIEW', 'CUSTOMER_VIEW',
  'CUSTOMER_RISK_ADJUST', 'RISK_EVENT_VIEW', 'RISK_EVENT_HANDLE', 'RISK_GRAPH_SCORE_VIEW',
  'RISK_GRAPH_CASE_VIEW', 'RISK_GRAPH_CASE_HANDLE', 'ADMIN_ALERT_VIEW', 'ADMIN_ALERT_HANDLE',
  'TICKET_VIEW', 'TICKET_HANDLE')
WHERE r.role_code = 'RISK_OPERATOR'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

INSERT INTO t_admin_role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM t_admin_role r
JOIN t_admin_permission p ON p.permission_code IN ('ADMIN_DASHBOARD_VIEW', 'RISK_EVENT_VIEW',
  'RISK_GRAPH_SCORE_VIEW', 'RISK_GRAPH_CASE_VIEW', 'RISK_GRAPH_CASE_HANDLE', 'RISK_RULE_VIEW',
  'RISK_RULE_UPDATE', 'ADMIN_ALERT_VIEW', 'ADMIN_ALERT_HANDLE', 'TICKET_VIEW', 'TICKET_HANDLE')
WHERE r.role_code = 'RISK_MANAGER'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

INSERT INTO t_admin_role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM t_admin_role r
JOIN t_admin_permission p ON p.permission_code IN ('ADMIN_DASHBOARD_VIEW',
  'WEALTH_PRODUCT_VIEW', 'WEALTH_PRODUCT_UPDATE', 'WEALTH_SETTLEMENT_VIEW', 'WEALTH_SETTLEMENT_RUN',
  'ADMIN_ALERT_VIEW', 'ADMIN_ALERT_HANDLE',
  'TICKET_VIEW', 'TICKET_HANDLE')
WHERE r.role_code = 'PRODUCT_MANAGER'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

INSERT INTO t_admin_role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM t_admin_role r
JOIN t_admin_permission p ON p.permission_code IN ('ADMIN_DASHBOARD_VIEW', 'CUSTOMER_VIEW',
  'RECONCILIATION_VIEW', 'RECONCILIATION_RUN', 'RECONCILIATION_ITEM_VIEW',
  'RECONCILIATION_ITEM_HANDLE', 'ADJUSTMENT_VIEW', 'ADJUSTMENT_CREATE',
  'ADMIN_ALERT_VIEW', 'ADMIN_ALERT_HANDLE', 'TICKET_VIEW', 'TICKET_HANDLE', 'TICKET_ASSIGN')
WHERE r.role_code = 'ACCOUNTING_OPERATOR'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

INSERT INTO t_admin_role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM t_admin_role r
JOIN t_admin_permission p ON p.permission_code IN ('ADMIN_DASHBOARD_VIEW',
  'RECONCILIATION_VIEW', 'RECONCILIATION_ITEM_VIEW', 'ADJUSTMENT_VIEW',
  'ADJUSTMENT_REVIEW', 'ADJUSTMENT_EXECUTE', 'ADMIN_ALERT_VIEW', 'ADMIN_ALERT_HANDLE',
  'TICKET_VIEW', 'TICKET_HANDLE')
WHERE r.role_code = 'ACCOUNTING_REVIEWER'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

INSERT INTO t_admin_role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM t_admin_role r
JOIN t_admin_permission p ON p.permission_code IN ('ADMIN_DASHBOARD_VIEW', 'CUSTOMER_VIEW',
  'RISK_EVENT_VIEW', 'RISK_GRAPH_SCORE_VIEW', 'RISK_GRAPH_CASE_VIEW', 'RISK_RULE_VIEW',
  'WEALTH_PRODUCT_VIEW', 'WEALTH_SETTLEMENT_VIEW', 'RECONCILIATION_VIEW', 'RECONCILIATION_ITEM_VIEW', 'ADJUSTMENT_VIEW',
  'ADMIN_AUDIT_VIEW', 'ADMIN_USER_VIEW', 'ROLE_PERMISSION_VIEW', 'ADMIN_ALERT_VIEW',
  'TICKET_VIEW', 'TICKET_ALL_VIEW')
WHERE r.role_code = 'AUDITOR'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

INSERT INTO t_admin_user_role (user_id, role_id)
SELECT u.user_id, r.role_id
FROM t_user u
JOIN t_admin_role r ON r.role_code = 'SUPER_ADMIN'
WHERE u.username = 'admin'
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id);

INSERT INTO t_admin_user_role (user_id, role_id)
SELECT u.user_id, r.role_id
FROM t_user u
JOIN t_admin_role r ON r.role_code = 'ACCOUNTING_REVIEWER'
WHERE u.username = 'admin_reviewer'
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id);

CREATE TABLE IF NOT EXISTS t_reconciliation_batch (
  batch_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  recon_date DATE NOT NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'RUNNING',
  total_checks INT NOT NULL DEFAULT 0,
  exception_count INT NOT NULL DEFAULT 0,
  created_by_admin_user_id BIGINT NOT NULL,
  started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  finished_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_recon_batch_recon_date (recon_date),
  KEY idx_recon_batch_status (status),
  KEY idx_recon_batch_started_at (started_at),
  CONSTRAINT fk_recon_batch_admin_user
    FOREIGN KEY (created_by_admin_user_id) REFERENCES t_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_reconciliation_item (
  item_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  batch_id BIGINT NOT NULL,
  check_type VARCHAR(50) NOT NULL,
  severity VARCHAR(20) NOT NULL,
  business_type VARCHAR(50) NULL,
  business_id VARCHAR(64) NULL,
  expected_value VARCHAR(255) NULL,
  actual_value VARCHAR(255) NULL,
  description VARCHAR(500) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
  handler_admin_user_id BIGINT NULL,
  handle_result VARCHAR(40) NULL,
  handle_note VARCHAR(500) NULL,
  handled_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_recon_item_batch_id (batch_id),
  KEY idx_recon_item_check_type (check_type),
  KEY idx_recon_item_severity (severity),
  KEY idx_recon_item_status (status),
  KEY idx_recon_item_handler (handler_admin_user_id),
  CONSTRAINT fk_recon_item_batch
    FOREIGN KEY (batch_id) REFERENCES t_reconciliation_batch (batch_id),
  CONSTRAINT fk_recon_item_handler
    FOREIGN KEY (handler_admin_user_id) REFERENCES t_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 't_reconciliation_item' AND column_name = 'handler_admin_user_id') = 0,
  'ALTER TABLE t_reconciliation_item ADD COLUMN handler_admin_user_id BIGINT NULL',
  'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 't_reconciliation_item' AND column_name = 'handle_result') = 0,
  'ALTER TABLE t_reconciliation_item ADD COLUMN handle_result VARCHAR(40) NULL',
  'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 't_reconciliation_item' AND column_name = 'handle_note') = 0,
  'ALTER TABLE t_reconciliation_item ADD COLUMN handle_note VARCHAR(500) NULL',
  'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 't_reconciliation_item' AND column_name = 'handled_at') = 0,
  'ALTER TABLE t_reconciliation_item ADD COLUMN handled_at DATETIME NULL',
  'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 't_reconciliation_item' AND column_name = 'updated_at') = 0,
  'ALTER TABLE t_reconciliation_item ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP',
  'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS t_reconciliation_action_log (
  action_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  item_id BIGINT NOT NULL,
  admin_user_id BIGINT NOT NULL,
  action_type VARCHAR(40) NOT NULL,
  before_status VARCHAR(40) NULL,
  after_status VARCHAR(40) NULL,
  note VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_recon_action_item_id (item_id),
  KEY idx_recon_action_admin_user_id (admin_user_id),
  KEY idx_recon_action_created_at (created_at),
  CONSTRAINT fk_recon_action_item
    FOREIGN KEY (item_id) REFERENCES t_reconciliation_item (item_id),
  CONSTRAINT fk_recon_action_admin_user
    FOREIGN KEY (admin_user_id) REFERENCES t_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_adjustment_request (
  adjustment_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  adjustment_no VARCHAR(40) NOT NULL,
  reconciliation_item_id BIGINT NULL,
  source_type VARCHAR(40) NOT NULL DEFAULT 'RECONCILIATION_ITEM',
  source_ticket_id BIGINT NULL,
  account_id BIGINT NOT NULL,
  customer_id BIGINT NOT NULL,
  direction VARCHAR(20) NOT NULL,
  amount DECIMAL(18,2) NOT NULL,
  reason VARCHAR(500) NOT NULL,
  evidence VARCHAR(500) NOT NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'PENDING_REVIEW',
  applicant_admin_user_id BIGINT NOT NULL,
  reviewer_admin_user_id BIGINT NULL,
  review_note VARCHAR(500) NULL,
  reviewed_at DATETIME NULL,
  executed_transaction_id BIGINT NULL,
  executed_ledger_id BIGINT NULL,
  executed_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_adjustment_no (adjustment_no),
  KEY idx_adjustment_item_id (reconciliation_item_id),
  KEY idx_adjustment_source_ticket (source_type, source_ticket_id),
  KEY idx_adjustment_account_id (account_id),
  KEY idx_adjustment_customer_id (customer_id),
  KEY idx_adjustment_status (status),
  KEY idx_adjustment_created_at (created_at),
  CONSTRAINT fk_adjustment_reconciliation_item
    FOREIGN KEY (reconciliation_item_id) REFERENCES t_reconciliation_item (item_id),
  CONSTRAINT fk_adjustment_account
    FOREIGN KEY (account_id) REFERENCES t_account (account_id),
  CONSTRAINT fk_adjustment_customer
    FOREIGN KEY (customer_id) REFERENCES t_customer (customer_id),
  CONSTRAINT fk_adjustment_applicant_admin
    FOREIGN KEY (applicant_admin_user_id) REFERENCES t_user (user_id),
  CONSTRAINT fk_adjustment_reviewer_admin
    FOREIGN KEY (reviewer_admin_user_id) REFERENCES t_user (user_id),
  CONSTRAINT fk_adjustment_executed_transaction
    FOREIGN KEY (executed_transaction_id) REFERENCES t_transaction (transaction_id),
  CONSTRAINT fk_adjustment_executed_ledger
    FOREIGN KEY (executed_ledger_id) REFERENCES t_ledger_entry (ledger_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @sql := IF((
  SELECT IS_NULLABLE FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 't_adjustment_request'
     AND column_name = 'reconciliation_item_id') = 'NO',
  'ALTER TABLE t_adjustment_request MODIFY reconciliation_item_id BIGINT NULL',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF((
  SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 't_adjustment_request'
     AND column_name = 'source_type') = 0,
  'ALTER TABLE t_adjustment_request ADD COLUMN source_type VARCHAR(40) NOT NULL DEFAULT ''RECONCILIATION_ITEM'' AFTER reconciliation_item_id',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF((
  SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 't_adjustment_request'
     AND column_name = 'source_ticket_id') = 0,
  'ALTER TABLE t_adjustment_request ADD COLUMN source_ticket_id BIGINT NULL AFTER source_type',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE t_adjustment_request
SET source_type = 'RECONCILIATION_ITEM'
WHERE source_type IS NULL OR source_type = '';

SET @sql := IF(
  (SELECT COUNT(*)
   FROM information_schema.statistics
   WHERE table_schema = DATABASE() AND table_name = 't_adjustment_request'
     AND index_name = 'idx_adjustment_source_ticket') = 0,
  'CREATE INDEX idx_adjustment_source_ticket ON t_adjustment_request (source_type, source_ticket_id)',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS t_adjustment_action_log (
  action_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  adjustment_id BIGINT NOT NULL,
  admin_user_id BIGINT NOT NULL,
  action_type VARCHAR(40) NOT NULL,
  before_status VARCHAR(40) NULL,
  after_status VARCHAR(40) NULL,
  note VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_adjustment_action_adjustment_id (adjustment_id),
  KEY idx_adjustment_action_admin_user_id (admin_user_id),
  KEY idx_adjustment_action_created_at (created_at),
  CONSTRAINT fk_adjustment_action_request
    FOREIGN KEY (adjustment_id) REFERENCES t_adjustment_request (adjustment_id),
  CONSTRAINT fk_adjustment_action_admin_user
    FOREIGN KEY (admin_user_id) REFERENCES t_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO t_risk_limit_rule
  (rule_code, txn_type, customer_risk_level, single_limit, daily_amount_limit, daily_count_limit, status)
VALUES
  ('LIMIT_WITHDRAW_C1', 'WITHDRAW', 'C1', 15000.00, 30000.00, 5, 'ACTIVE'),
  ('LIMIT_WITHDRAW_C2', 'WITHDRAW', 'C2', 25000.00, 50000.00, 8, 'ACTIVE'),
  ('LIMIT_WITHDRAW_C3', 'WITHDRAW', 'C3', 50000.00, 100000.00, 10, 'ACTIVE'),
  ('LIMIT_WITHDRAW_C4', 'WITHDRAW', 'C4', 75000.00, 150000.00, 15, 'ACTIVE'),
  ('LIMIT_WITHDRAW_C5', 'WITHDRAW', 'C5', 100000.00, 200000.00, 20, 'ACTIVE'),
  ('LIMIT_TRANSFER_C1', 'TRANSFER_INNER', 'C1', 60000.00, 90000.00, 8, 'ACTIVE'),
  ('LIMIT_TRANSFER_C2', 'TRANSFER_INNER', 'C2', 100000.00, 150000.00, 12, 'ACTIVE'),
  ('LIMIT_TRANSFER_C3', 'TRANSFER_INNER', 'C3', 200000.00, 300000.00, 20, 'ACTIVE'),
  ('LIMIT_TRANSFER_C4', 'TRANSFER_INNER', 'C4', 300000.00, 450000.00, 25, 'ACTIVE'),
  ('LIMIT_TRANSFER_C5', 'TRANSFER_INNER', 'C5', 400000.00, 600000.00, 30, 'ACTIVE'),
  ('LIMIT_PAYMENT_C1', 'PAYMENT', 'C1', 6000.00, 15000.00, 12, 'ACTIVE'),
  ('LIMIT_PAYMENT_C2', 'PAYMENT', 'C2', 10000.00, 25000.00, 20, 'ACTIVE'),
  ('LIMIT_PAYMENT_C3', 'PAYMENT', 'C3', 20000.00, 50000.00, 30, 'ACTIVE'),
  ('LIMIT_PAYMENT_C4', 'PAYMENT', 'C4', 30000.00, 75000.00, 40, 'ACTIVE'),
  ('LIMIT_PAYMENT_C5', 'PAYMENT', 'C5', 40000.00, 100000.00, 50, 'ACTIVE'),
  ('LIMIT_BUY_WEALTH_C1', 'BUY_WEALTH', 'C1', 50000.00, 100000.00, 4, 'ACTIVE'),
  ('LIMIT_BUY_WEALTH_C2', 'BUY_WEALTH', 'C2', 150000.00, 300000.00, 6, 'ACTIVE'),
  ('LIMIT_BUY_WEALTH_C3', 'BUY_WEALTH', 'C3', 500000.00, 800000.00, 10, 'ACTIVE'),
  ('LIMIT_BUY_WEALTH_C4', 'BUY_WEALTH', 'C4', 750000.00, 1200000.00, 15, 'ACTIVE'),
  ('LIMIT_BUY_WEALTH_C5', 'BUY_WEALTH', 'C5', 1000000.00, 1600000.00, 20, 'ACTIVE')
ON DUPLICATE KEY UPDATE
  rule_code = rule_code;

CREATE TABLE IF NOT EXISTS t_notification (
  notification_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  customer_id BIGINT NOT NULL,
  user_id BIGINT NULL,
  notification_type VARCHAR(30) NOT NULL,
  title VARCHAR(120) NOT NULL,
  content VARCHAR(500) NOT NULL,
  business_type VARCHAR(40) NULL,
  business_id VARCHAR(64) NULL,
  read_flag TINYINT(1) NOT NULL DEFAULT 0,
  read_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_notification_customer_id (customer_id),
  KEY idx_notification_read_flag (read_flag),
  KEY idx_notification_created_at (created_at),
  CONSTRAINT fk_notification_customer
    FOREIGN KEY (customer_id) REFERENCES t_customer (customer_id),
  CONSTRAINT fk_notification_user
    FOREIGN KEY (user_id) REFERENCES t_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_admin_alert (
  alert_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  alert_type VARCHAR(40) NOT NULL,
  severity VARCHAR(20) NOT NULL DEFAULT 'INFO',
  title VARCHAR(120) NOT NULL,
  content VARCHAR(500) NOT NULL,
  target_type VARCHAR(50) NULL,
  target_id VARCHAR(64) NULL,
  responsible_role_code VARCHAR(50) NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'NEW',
  assigned_admin_user_id BIGINT NULL,
  handled_by_admin_user_id BIGINT NULL,
  handle_note VARCHAR(500) NULL,
  handled_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_admin_alert_type (alert_type),
  KEY idx_admin_alert_severity (severity),
  KEY idx_admin_alert_status (status),
  KEY idx_admin_alert_role (responsible_role_code),
  KEY idx_admin_alert_business (alert_type, target_type, target_id, status),
  KEY idx_admin_alert_created_at (created_at),
  CONSTRAINT fk_admin_alert_assigned_user
    FOREIGN KEY (assigned_admin_user_id) REFERENCES t_user (user_id),
  CONSTRAINT fk_admin_alert_handled_user
    FOREIGN KEY (handled_by_admin_user_id) REFERENCES t_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = DATABASE() AND table_name = 't_admin_alert'
     AND index_name = 'idx_admin_alert_business') = 0,
  'ALTER TABLE t_admin_alert ADD INDEX idx_admin_alert_business (alert_type, target_type, target_id, status)',
  'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS t_service_ticket (
  ticket_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  ticket_no VARCHAR(40) NOT NULL,
  customer_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  ticket_type VARCHAR(40) NOT NULL,
  priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
  status VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED',
  title VARCHAR(120) NOT NULL,
  description VARCHAR(1000) NOT NULL,
  related_business_type VARCHAR(50) NULL,
  related_business_id VARCHAR(64) NULL,
  assigned_role_code VARCHAR(50) NULL,
  assigned_admin_user_id BIGINT NULL,
  accepted_at DATETIME NULL,
  resolved_at DATETIME NULL,
  closed_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_service_ticket_no (ticket_no),
  KEY idx_service_ticket_customer (customer_id),
  KEY idx_service_ticket_type (ticket_type),
  KEY idx_service_ticket_priority (priority),
  KEY idx_service_ticket_status (status),
  KEY idx_service_ticket_assigned_role (assigned_role_code),
  KEY idx_service_ticket_assigned_admin (assigned_admin_user_id),
  KEY idx_service_ticket_related (related_business_type, related_business_id),
  KEY idx_service_ticket_created_at (created_at),
  CONSTRAINT fk_service_ticket_customer
    FOREIGN KEY (customer_id) REFERENCES t_customer (customer_id),
  CONSTRAINT fk_service_ticket_user
    FOREIGN KEY (user_id) REFERENCES t_user (user_id),
  CONSTRAINT fk_service_ticket_assigned_admin
    FOREIGN KEY (assigned_admin_user_id) REFERENCES t_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_ticket_reply (
  reply_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  ticket_id BIGINT NOT NULL,
  sender_type VARCHAR(20) NOT NULL,
  sender_user_id BIGINT NULL,
  content VARCHAR(1000) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_ticket_reply_ticket (ticket_id),
  KEY idx_ticket_reply_sender (sender_user_id),
  KEY idx_ticket_reply_created_at (created_at),
  CONSTRAINT fk_ticket_reply_ticket
    FOREIGN KEY (ticket_id) REFERENCES t_service_ticket (ticket_id),
  CONSTRAINT fk_ticket_reply_sender
    FOREIGN KEY (sender_user_id) REFERENCES t_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_ticket_action_log (
  log_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  ticket_id BIGINT NOT NULL,
  admin_user_id BIGINT NULL,
  action_type VARCHAR(40) NOT NULL,
  before_status VARCHAR(30) NULL,
  after_status VARCHAR(30) NULL,
  note VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_ticket_action_ticket (ticket_id),
  KEY idx_ticket_action_admin (admin_user_id),
  KEY idx_ticket_action_created_at (created_at),
  CONSTRAINT fk_ticket_action_ticket
    FOREIGN KEY (ticket_id) REFERENCES t_service_ticket (ticket_id),
  CONSTRAINT fk_ticket_action_admin
    FOREIGN KEY (admin_user_id) REFERENCES t_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_admin_audit_log (
  log_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  admin_user_id BIGINT NOT NULL,
  operation_type VARCHAR(50) NOT NULL,
  target_type VARCHAR(50) NULL,
  target_id VARCHAR(64) NULL,
  detail VARCHAR(500) NULL,
  ip_address VARCHAR(64) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_admin_audit_admin_user_id (admin_user_id),
  KEY idx_admin_audit_created_at (created_at),
  CONSTRAINT fk_admin_audit_user
    FOREIGN KEY (admin_user_id) REFERENCES t_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
