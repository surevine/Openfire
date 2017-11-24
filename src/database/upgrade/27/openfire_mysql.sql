ALTER TABLE ofPubsubItem ADD COLUMN label MEDIUMTEXT NULL;
UPDATE ofVersion SET version = 27 WHERE name = 'openfire';
