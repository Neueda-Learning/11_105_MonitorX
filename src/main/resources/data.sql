-- Seed baseline data for MonitorX
INSERT IGNORE INTO customers (id, name, account_number, registered_country) VALUES (1, 'Rahul Sharma', 'ACC1001', 'India');
INSERT IGNORE INTO customers (id, name, account_number, registered_country) VALUES (2, 'Priya Verma', 'ACC1002', 'India');
INSERT IGNORE INTO customers (id, name, account_number, registered_country) VALUES (3, 'John Smith', 'ACC1003', 'USA');
INSERT IGNORE INTO customers (id, name, account_number, registered_country) VALUES (4, 'Amina Yusuf', 'ACC1004', 'UAE');

INSERT IGNORE INTO rules (id, name, type, severity, parameters, is_active) VALUES (1, 'High Transaction Amount', 'AMOUNT_THRESHOLD', 'HIGH', '{"threshold":10000.00}', true);
INSERT IGNORE INTO rules (id, name, type, severity, parameters, is_active) VALUES (2, 'High Velocity Check', 'VELOCITY', 'MEDIUM', '{"timeWindowMinutes":10,"maxCount":5}', true);
INSERT IGNORE INTO rules (id, name, type, severity, parameters, is_active) VALUES (3, 'New Payee Check', 'NEW_PAYEE', 'LOW', '{}', true);
INSERT IGNORE INTO rules (id, name, type, severity, parameters, is_active) VALUES (4, 'Daily Accumulation Limit', 'DAILY_LIMIT', 'HIGH', '{"dailyLimit":50000.00}', true);

-- Seed default operator (username: admin, password: admin123)
INSERT IGNORE INTO operators (id, username, password_hash) VALUES (1, 'admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9');
