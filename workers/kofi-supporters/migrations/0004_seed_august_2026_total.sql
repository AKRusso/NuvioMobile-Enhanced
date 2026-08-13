INSERT INTO donation_monthly_totals (month_key, currency, amount_cents)
VALUES ('2026-08', 'EUR', 500)
ON CONFLICT(month_key, currency) DO NOTHING;
