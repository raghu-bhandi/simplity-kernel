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