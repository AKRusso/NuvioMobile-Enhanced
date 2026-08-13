ALTER TABLE donation_receipts ADD COLUMN month_key TEXT;
ALTER TABLE donation_receipts ADD COLUMN currency TEXT;
ALTER TABLE donation_receipts ADD COLUMN amount_cents INTEGER;

CREATE TRIGGER donation_receipts_update_monthly_total
AFTER INSERT ON donation_receipts
WHEN NEW.month_key IS NOT NULL AND NEW.currency IS NOT NULL AND NEW.amount_cents IS NOT NULL
BEGIN
    INSERT INTO donation_monthly_totals (month_key, currency, amount_cents)
    VALUES (NEW.month_key, NEW.currency, NEW.amount_cents)
    ON CONFLICT(month_key, currency) DO UPDATE SET amount_cents = amount_cents + excluded.amount_cents;
END;
