-- Скрипт для синхронізації sequences після завантаження даних з явно вказаними ID
-- Виконайте цей скрипт в psql або через DBeaver

-- Оновлення sequence для transactions
SELECT setval('transactions_id_seq', (SELECT COALESCE(MAX(id), 1) FROM transactions), true);

-- Оновлення sequence для інших таблиць (на всяк випадок)
SELECT setval('accounts_id_seq', (SELECT COALESCE(MAX(id), 1) FROM accounts), true);
SELECT setval('customers_id_seq', (SELECT COALESCE(MAX(id), 1) FROM customers), true);
SELECT setval('users_id_seq', (SELECT COALESCE(MAX(id), 1) FROM users), true);
SELECT setval('employers_id_seq', (SELECT COALESCE(MAX(id), 1) FROM employers), true);
SELECT setval('roles_id_seq', (SELECT COALESCE(MAX(id), 1) FROM roles), true);

-- Перевірка поточних значень sequences
SELECT 'transactions_id_seq', last_value FROM transactions_id_seq
UNION ALL
SELECT 'accounts_id_seq', last_value FROM accounts_id_seq
UNION ALL
SELECT 'customers_id_seq', last_value FROM customers_id_seq
UNION ALL
SELECT 'users_id_seq', last_value FROM users_id_seq
UNION ALL
SELECT 'employers_id_seq', last_value FROM employers_id_seq
UNION ALL
SELECT 'roles_id_seq', last_value FROM roles_id_seq;

