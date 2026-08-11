CREATE TABLE donations (
    id TEXT PRIMARY KEY NOT NULL CHECK (length(id) BETWEEN 1 AND 128),
    name TEXT NOT NULL CHECK (length(name) BETWEEN 1 AND 200),
    donated_at TEXT NOT NULL,
    message TEXT CHECK (message IS NULL OR length(message) <= 1000),
    avatar TEXT CHECK (avatar IS NULL OR length(avatar) <= 2048),
    profile TEXT CHECK (profile IS NULL OR length(profile) <= 2048),
    received_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
) STRICT;

CREATE INDEX donations_recent_idx ON donations(donated_at DESC, id DESC);
