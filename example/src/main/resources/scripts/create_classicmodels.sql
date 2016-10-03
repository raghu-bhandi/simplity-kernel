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

/* DROP the existing tables. Comment this out if it is not needed. */

DROP TABLE IF EXISTS Customers ;
DROP TABLE IF EXISTS Employees;
DROP TABLE IF EXISTS Offices;
DROP TABLE IF EXISTS OrderDetails;
DROP TABLE IF EXISTS Orders;
DROP TABLE IF EXISTS Payments;
DROP TABLE IF EXISTS Products;
DROP TABLE IF EXISTS StoreFiles;
DROP TABLE IF EXISTS BLOB_TEST_TABLE;

/* Create the full set of Classic Models Tables */

CREATE TABLE Customers (
  customerNumber INTEGER NOT NULL,
  customerName VARCHAR(50) NOT NULL,
  contactLastName VARCHAR(50) NOT NULL,
  contactFirstName VARCHAR(50) NOT NULL,
  phone VARCHAR(50) NOT NULL,
  addressLine1 VARCHAR(50) NOT NULL,
  addressLine2 VARCHAR(50) NULL,
  city VARCHAR(50) NOT NULL,
  state VARCHAR(50) NULL,
  postalCode VARCHAR(15) NULL,
  country VARCHAR(50) NOT NULL,
  salesRepEmployeeNumber INTEGER NULL,
  creditLimit DOUBLE NULL,
  PRIMARY KEY (customerNumber)
)  AS SELECT * FROM CSVREAD('${resourcespath}/datafiles/customers.txt');

CREATE TABLE Employees (
  employeeNumber INTEGER NOT NULL,
  lastName VARCHAR(50) NOT NULL,
  firstName VARCHAR(50) NOT NULL,
  extension VARCHAR(10) NOT NULL,
  email VARCHAR(100) NOT NULL,
  officeCode VARCHAR(20) NOT NULL,
  reportsTo INTEGER NULL,
  jobTitle VARCHAR(50) NOT NULL,
  PRIMARY KEY (employeeNumber)
)  AS SELECT * FROM CSVREAD('${resourcespath}/datafiles/employees.txt');

CREATE TABLE Offices (
  officeCode VARCHAR(50) NOT NULL,
  city VARCHAR(50) NOT NULL,
  phone VARCHAR(50) NOT NULL,
  addressLine1 VARCHAR(50) NOT NULL,
  addressLine2 VARCHAR(50) NULL,
  state VARCHAR(50) NULL,
  country VARCHAR(50) NOT NULL,
  postalCode VARCHAR(10) NOT NULL,
  territory VARCHAR(10) NOT NULL,
  PRIMARY KEY (officeCode)
)  AS SELECT * FROM CSVREAD('${resourcespath}/datafiles/offices.txt');

CREATE TABLE OrderDetails (
  orderNumber INTEGER NOT NULL,
  productCode VARCHAR(50) NOT NULL,
  quantityOrdered INTEGER NOT NULL,
  priceEach DOUBLE NOT NULL,
  orderLineNumber SMALLINT NOT NULL,
  PRIMARY KEY (orderNumber, productCode)
)  AS SELECT * FROM CSVREAD('${resourcespath}/datafiles/orderdetails.txt');

CREATE TABLE Orders (
  orderNumber INTEGER NOT NULL,
  orderDate DATETIME NOT NULL,
  requiredDate DATETIME NOT NULL,
  shippedDate DATETIME NULL,
  status VARCHAR(15) NOT NULL,
  comments TEXT NULL,
  customerNumber INTEGER NOT NULL,
  PRIMARY KEY (orderNumber)
)  AS SELECT * FROM CSVREAD('${resourcespath}/datafiles/orders.txt');

CREATE TABLE Payments (
  customerNumber INTEGER NOT NULL,
  checkNumber VARCHAR(50) NOT NULL,
  paymentDate DATETIME NOT NULL,
  amount DOUBLE NOT NULL,
  PRIMARY KEY (customerNumber, checkNumber)
)  AS SELECT * FROM CSVREAD('${resourcespath}/datafiles/payments.txt');

CREATE TABLE Products (
  productCode VARCHAR(50) NOT NULL,
  productName VARCHAR(70) NOT NULL,
  productLine VARCHAR(50) NOT NULL,
  productScale VARCHAR(10) NOT NULL,
  productVendor VARCHAR(50) NOT NULL,
  productDescription TEXT NOT NULL,
  quantityInStock SMALLINT NOT NULL,
  buyPrice DOUBLE NOT NULL,
  MSRP DOUBLE NOT NULL,
  PRIMARY KEY (productCode)
)  AS SELECT * FROM CSVREAD('${resourcespath}/datafiles/products.txt');

--CREATE TABLE StoreFiles (
--  fileCode INTEGER NOT NULL,
--  file BLOB NOT NULL
--  PRIMARY KEY (fileCode)
--);

CREATE TABLE  BLOB_TEST_TABLE 
   (	BLOB_TEST_NAME VARCHAR NOT NULL , 
	CLOB_IN_ROW CLOB, 
	CLOB_TEXT  VARCHAR2(1000)  NOT NULL , 
	BLOB_EXTERNAL  VARCHAR  NOT NULL  
   ) ;

--INSERT INTO A StoreFiles values(1, FILE_READ('${resourcespath}/downloads/getstarted/Peter_Pan.jpg'));