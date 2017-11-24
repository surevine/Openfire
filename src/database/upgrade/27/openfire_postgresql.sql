ALTER TABLE ofPubsubItem ADD COLUMN label TEXT;
UPDATE ofVersion SET version = 27 WHERE name = 'openfire';
