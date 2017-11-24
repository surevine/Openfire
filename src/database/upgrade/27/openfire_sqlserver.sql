ALTER TABLE ofPubsubItem ADD COLUMN label NTEXT;
UPDATE ofVersion SET version = 27 WHERE name = 'openfire';
