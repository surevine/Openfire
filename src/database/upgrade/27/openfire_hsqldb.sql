ALTER TABLE ofPubsubItem ADD COLUMN label VARCHAR(4000) NULL;
UPDATE ofVersion SET version = 27 WHERE name = 'openfire';
