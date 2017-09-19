ALTER TABLE ofPubsubItem ADD COLUMN label VARCHAR(4000);
UPDATE ofVersion SET version = 27 WHERE name = 'openfire';
