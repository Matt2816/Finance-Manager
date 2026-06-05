ALTER TABLE spending_categories DROP CONSTRAINT IF EXISTS spending_categories_slug_key;
ALTER TABLE normalized_transactions DROP CONSTRAINT IF EXISTS normalized_transactions_transaction_id_key;
ALTER TABLE merchant_loyalty_metrics DROP CONSTRAINT IF EXISTS merchant_loyalty_metrics_merchant_key_key;

CREATE UNIQUE INDEX IF NOT EXISTS uk_spending_categories_user_slug ON spending_categories (user_id, slug);
CREATE UNIQUE INDEX IF NOT EXISTS uk_normalized_transactions_user_tx ON normalized_transactions (user_id, transaction_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_merchant_loyalty_metrics_user_key ON merchant_loyalty_metrics (user_id, merchant_key);
CREATE UNIQUE INDEX IF NOT EXISTS uk_transactions_user_hash ON transactions (user_id, hash) WHERE hash IS NOT NULL;
