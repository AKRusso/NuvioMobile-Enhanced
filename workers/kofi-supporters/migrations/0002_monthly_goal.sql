CREATE TABLE donation_receipts (
    id TEXT PRIMARY KEY NOT NULL CHECK (length(id) BETWEEN 1 AND 128),
    received_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
) STRICT;

CREATE TABLE donation_monthly_totals (
    month_key TEXT NOT NULL CHECK (length(month_key) = 7),
    currency TEXT NOT NULL CHECK (length(currency) BETWEEN 3 AND 8),
    amount_cents INTEGER NOT NULL CHECK (amount_cents >= 0),
    PRIMARY KEY (month_key, currency)
) STRICT;
