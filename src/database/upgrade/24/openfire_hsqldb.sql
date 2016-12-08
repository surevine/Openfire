CREATE TABLE ofMixService (
  serviceID           BIGINT        NOT NULL,
  subdomain           VARCHAR(255)  NOT NULL,
  description         VARCHAR(255),
  isHidden            INTEGER       NOT NULL,
  CONSTRAINT ofMucService_pk PRIMARY KEY (subdomain)
);
CREATE INDEX ofMixService_serviceid_idx ON ofMixService(serviceID);

// Entry for default conference service
INSERT INTO ofMixService (serviceID, subdomain, isHidden) VALUES (1, 'mix', 0);

UPDATE ofVersion SET version = 24 WHERE name = 'openfire';
