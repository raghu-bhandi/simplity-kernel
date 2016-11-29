/*CREATE SCHEMA IF NOT EXISTS nomination; 
SET SCHEMA nomination; */

CREATE TABLE IF NOT EXISTS nomination (
  nominationId INTEGER auto_increment NOT NULL,
  submitterMailNickname  VARCHAR(100),
  submitterMail  VARCHAR(200),
  status  VARCHAR(100),
  selectedCategory  VARCHAR(100),
<<<<<<< HEAD
  summary  VARCHAR(1000),
=======
  summary  VARCHAR(100),
>>>>>>> refs/remotes/origin/master
  nomination  VARCHAR(100) NOT NULL,
  sponsorMailNickname VARCHAR(100),
  sponsorMail VARCHAR(200),
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
	employeeMailNickname VARCHAR(100) NOT NULL, 
	employeeMail VARCHAR(200) NOT NULL,
    eNo VARCHAR(100) NOT NULL,
	Name VARCHAR(100) NOT NULL,
	Unit VARCHAR(100) NOT NULL,
	contribution VARCHAR(1000) NOT NULL,  	
	PRIMARY KEY (membersId),
	FOREIGN KEY (nominationId)
  	REFERENCES nomination(nominationId)
);