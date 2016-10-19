/*CREATE SCHEMA IF NOT EXISTS nomination; 
SET SCHEMA nomination; */

CREATE TABLE IF NOT EXISTS nomination (
  nominationId INTEGER auto_increment NOT NULL,
  submitterId  VARCHAR(100) NOT NULL,
  status  VARCHAR(100) NOT NULL,
  selectedCategory  VARCHAR(100) NOT NULL,
  selectedLevel  VARCHAR(100) NOT NULL,
  nomination  VARCHAR(100) NOT NULL,
  sponsormailid VARCHAR(100) NOT NULL,
  sponsorname VARCHAR(100) NOT NULL,
  sponsornumber VARCHAR(100) NOT NULL,
  filekey VARCHAR(100) NOT NULL,
  filename VARCHAR(100) NOT NULL,
  filetype VARCHAR(100) NOT NULL,
  filesize VARCHAR(100) NOT NULL,
  PRIMARY KEY (nominationId)
);

CREATE TABLE IF NOT EXISTS members (
	membersId INTEGER auto_increment NOT NULL,
	nominationId INTEGER NOT NULL, 
	employeeEmailID VARCHAR(100) NOT NULL, 
    eNo VARCHAR(100) NOT NULL,
	Name VARCHAR(100) NOT NULL,
	Unit VARCHAR(100) NOT NULL,
	contribution VARCHAR(1000) NOT NULL,  	
	PRIMARY KEY (membersId),
	FOREIGN KEY (nominationId)
  	REFERENCES nomination(nominationId)
);