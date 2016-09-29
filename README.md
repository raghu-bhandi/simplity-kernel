# simplity

[![Build Status](https://travis-ci.org/simplity/simplity.svg?branch=master)](https://travis-ci.org/simplity/simplity)

``` Ridiculously simple set of APIs/framework for typical transaction processing systems.```

##Components of simplity

###- Service  
  > Component which helps in Transaction Processing,here server is viewed as a logical entity that can respond to all the published services.A service is uniquely identified by clients with its published name. We recommend a qualified name of the form module.serviceName. For large projects you may go in for module.subModule.serviceName etc  
  
###- Application  
   > Helps in configuring the application.
   
###- DataTypes  
 > A data type component defines the restrictions on the values a field can have. Its primary purpose is to validate values that are received from the client.  Data types are organized into one or more files inside /dt/ folder. Data type names are to be unique across all files for a project
 
###- Record  
> Table, entity and data-structure are the other possible names for this component. Essentially we define a group of fields into a record and use it for different purposes. A record helps us in implementing one of the golden rules of design : DRY (Do not Repeat Yourself) Once a record is defined, we can use to represent a database table/view, or a data structure for a parameter for a stored procedure, or set of fields that are expected as input from client etc. 

###- SQL  
> SQL is the language that is used to direct the rdbms to manipulate retrieve data, manipulate data, or change database structure itself. This component allows you to design a dynamic sql to e executed at run time, based on run-time values


Simplity is based on the concepts used in **[Exility](https://github.com/ExilantTechnologies/ExilityCore-5.0.0)**.
It is re-designed based on the current best practices in the industry, and a complete re-write.

