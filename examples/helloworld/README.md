This example provides an overview

1. Basic service - Refer to the /helloworld/src/main/resources/comp/service/tp/helloworld.xml

2. Calls another rest service - Refer to /helloworld/src/main/resources/comp/service/tp/callSqlHw.xml
    
    Host the application, and on index.html click on the "Invoke rest service" to invoke the service which calls another service

3. Implements the Servlet for Rest Maturity model. Impacted files are 
	
	- web.xml
	- services for get request method should be under the folder get, example nest\get\helloworld.xml
	- services for post request method should be under the folder post, example
	nest\post\helloworld.xml
 	
 	Once the application is host, this can be tested with get and post request to
 http://localhost:8080/helloworld/rest/nest/helloworld
 4. File upload and download 
 	- uploads the file and generates a random key for each uploaded file and stores it in temp area
	- File download method takes the generated file key and downloads the file with the name we have provided along with file key

     
