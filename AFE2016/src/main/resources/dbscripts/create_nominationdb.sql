/*CREATE SCHEMA IF NOT EXISTS nomination; 
SET SCHEMA nomination; */

CREATE TABLE IF NOT EXISTS nomination (
  nominationId INTEGER auto_increment NOT NULL,
  submitterId  VARCHAR(100) NOT NULL,
  status  VARCHAR(100) NOT NULL,
  selectedCategory  VARCHAR(100),
  selectedLevel  VARCHAR(100),
  nomination  VARCHAR(100) NOT NULL,
  sponsormailid VARCHAR(100),
  sponsorname VARCHAR(100),
  sponsornumber VARCHAR(100),
  filekey VARCHAR(100),
  filename VARCHAR(100),
  filetype VARCHAR(100),
  filesize VARCHAR(100),
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