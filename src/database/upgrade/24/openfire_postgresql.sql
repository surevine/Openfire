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
  channelID           BIGINT        NOT NULL,
  creationDate        CHAR(15)      NOT NULL,
  modificationDate    CHAR(15)      NOT NULL,
  name                VARCHAR(50)   NOT NULL,
  owner               VARCHAR(1024)  NOT NULL,
  jidVisibility       INTEGER       NOT NULL,
  CONSTRAINT ofMixChannel_pk PRIMARY KEY (channelID)
);

CREATE INDEX ofMixChannel_name_idx ON ofMixChannel(name);

CREATE TABLE ofMixChannelParticipant (
  mcpID               				BIGINT        	NOT NULL,
  realJid             				VARCHAR(1024)   NOT NULL,
  proxyJid            				VARCHAR(1024)   NOT NULL,
  nickName                          VARCHAR(1024),
  channelJidVisibilityPreference 	INTEGER  		NOT NULL,
  channelID_fk       				BIGINT          NOT NULL,
  CONSTRAINT ofMixChannelParticipant_pk PRIMARY KEY (mcpID),
  FOREIGN KEY (channelID_fk) REFERENCES ofMixChannel(channelID)
);

CREATE TABLE ofMixChannelParticipantSubscription (
  mcpSubsID 	        BIGINT        	NOT NULL,
  nodeName              VARCHAR(255)    NOT NULL,
  participantID_fk      BIGINT          NOT NULL,
  CONSTRAINT mcpSubsID_pk PRIMARY KEY (mcpSubsID),
  FOREIGN KEY (participantID_fk) REFERENCES ofMixChannelParticipant(mcpID)
);

UPDATE ofVersion SET version = 24 WHERE name = 'openfire';
