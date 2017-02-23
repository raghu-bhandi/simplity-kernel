DROP TABLE hw IF EXISTS;


CREATE TABLE hw (
  id         INTEGER IDENTITY PRIMARY KEY,
  greeting VARCHAR(30),
);

INSERT into hw (id,greeting) values (1,'Good morning');