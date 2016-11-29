/*CREATE SCHEMA IF NOT EXISTS nomination; 
SET SCHEMA registration; */

CREATE TABLE IF NOT EXISTS registration (
  registrationId INTEGER auto_increment NOT NULL,
  submitterId  VARCHAR(100),
  domain  VARCHAR(100),
  application  VARCHAR(100),
  mailid  VARCHAR(100),
  apikey  VARCHAR(100),
  PRIMARY KEY (registrationId)
);

CREATE TABLE IF NOT EXISTS audittrail (
  auditId INTEGER auto_increment NOT NULL,
  fromId  VARCHAR(100),
  toId  VARCHAR(100),
  subject  VARCHAR(100),
  mail  BLOB,
  mailsentTime  TIMESTAMP,
  PRIMARY KEY (auditId)
);