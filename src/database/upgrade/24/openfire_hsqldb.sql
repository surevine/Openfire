CREATE TABLE ofMixService (
  serviceID           BIGINT        NOT NULL,
  subdomain           VARCHAR(255)  NOT NULL,
  description         VARCHAR(255),
  isHidden            INTEGER       NOT NULL,
  CONSTRAINT ofMixService_pk PRIMARY KEY (subdomain)
);
CREATE INDEX ofMixService_serviceid_idx ON ofMixService(serviceID);

-- Entry for default conference service
INSERT INTO ofMixService (serviceID, subdomain, isHidden) VALUES (1, 'mix', 0);

CREATE TABLE ofMixChannel (
  channelID           INTEGER       NOT NULL,
  creationDate        CHAR(15)      NOT NULL,
  modificationDate    CHAR(15)      NOT NULL,
  name                VARCHAR(50)   NOT NULL,
  owner               VARCHAR(50)   NOT NULL,
  jidVisibility       INTEGER       NOT NULL,
  CONSTRAINT ofMixChannel_pk PRIMARY KEY (channelID)
)

CREATE INDEX ofMixChannel_name_idx ON ofMixChannel(name);

UPDATE ofVersion SET version = 24 WHERE name = 'openfire';
