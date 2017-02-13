/******************************************************************************
 * Copyright (c) 2005 Actuate Corporation.
 * All rights reserved. This file and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial implementation
 *
 * Classic Models Inc. sample database developed as part of the
 * Eclipse BIRT Project. For more information, see http:\\www.eclipse.org\birt
 *
 *******************************************************************************/

/* Recommended DATABASE name is classicmodels. */

/* CREATE DATABASE classicmodels; */
/* USE classicmodels; */

/* DROP the existing tables. Comment this out if it is not needed.*/


DROP TABLE IF EXISTS STUDENTS;

/* Create the full set of Classic Models Tables */
SET @DBPATH = DATABASE_PATH();


Create schema IF NOT EXISTS  TESTSCHEMA;

CREATE TABLE IF NOT EXISTS  TESTSCHEMA.STUDENTS (
  rollNumber INTEGER NOT NULL  PRIMARY KEY ,
  name VARCHAR(50) NOT NULL,
  schoolName VARCHAR(50) NOT NULL
  
) AS SELECT * FROM CSVREAD(SELECT CONCAT(@DBPATH,'/students.txt'));
