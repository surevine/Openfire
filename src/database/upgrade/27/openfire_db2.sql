ALTER TABLE ofPubsubItem ADD COLUMN label clob;
UPDATE ofVersion SET version = 27 WHERE name = 'openfire';
