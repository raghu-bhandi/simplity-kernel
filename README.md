# Simplity

* Github  - [![Build Status](https://travis-ci.org/simplity/simplity.svg?branch=master)](https://travis-ci.org/simplity/simplity)
* InfyGit - [![Build Status](http://infygit/encore/simplity/badges/master/build.svg)](http://infygit/encore/simplity/pipelines)

``` Ridiculously simple set of APIs/framework for typical transaction processing systems.```

##Components of simplity

###- Service  
  > Component which helps in Transaction Processing,here server is viewed as a logical entity that can respond to all the published services.A service is uniquely identified by clients with its published name. We recommend a qualified name of the form module.serviceName. For large projects you may go in for module.subModule.serviceName etc  
  
   **- Actions**  
     ``` Actions implement the logic/design of your service.Each action does a part of the over-all work. As far as possible, actions are to be designed independent of other actions in the service, just as a service is designed independent of other services in an application.Simplity has been extensively used to deliver services that are part of transaction processing system. We have designed several actions that are well suited to carry out part of such transactions.```
      
* **AddColumn :** *Helps in adding column to a data sheet.*
* **AddMessage :** *Add a message to the context.*
* **Complexlogic :** *Logic that is implemented in a java code.*
* **CopyRows :** *Copy rows from a compatible sheet.*
* **CopyUserId :** *userId id saved in session on login, and is copied into the service context. You can copy this value into any other field you want to deal with.*
* **CreateSheet :** *Set rows and columns to a table.*
* **Download :** *Download a file from permanent storage to temp storage.*
* **ExecuteSP :** *If your db operation is more complex, you may write a stored procedure and execute it using this action.*
* **ExecuteSql :** *you may design your own sql.Such sql is executed as a prepared statement using this action.*
* **Filter :** *Read rows from the underlying table/view based on the filtering criteria. Rows from one or more child-tables can also be read as part of this action.*
* **JumpTo :** *Service has actions that are executed in a sequence. JumpTo allows you to change this sequence.*
* **Log :** *Log values of fields and sheets into trace.*
* **Logic :** *Logic that is implemented in a java code.*
* **Loop :** *Loop through a set of actions for each row in a data sheet.*
* **Read :** *Read a row from the underlying table/view based on the primary key value. Columns from this row are set as field values in the service context. Rows from one r more child-tables can also be read as part of this action.*
* **ReadWithSql :** *Read a row/s from as output of a prepared statement/sql.Rows from one or more child-tables can also be read as part of this action.*
* **RenameSheet :** *Change the name of a data sheet using this action.*
* **RowExists :** *Check if a row exists in this record for the primary key.*
* **Save :** *Add, update or delete rows of data based on specification in a record. While adding we handle primary key generation, as well as values for fields like createdBy, created at etc.*
* **SetValue :** *Set a field value using an expression that is evaluated at run time.*
* **SubService :** *Service that is to be executed as a step/action in side another service.*
* **Suggest :** *Speciffically design to provide suggested value for a goolgle-suggest-like control on the client. Columns from matching rows are populated into the output sheet, based on a record definition.*
* **Upload :** *Upload a file from temp storage to permanent storage and change the file key/token.*

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