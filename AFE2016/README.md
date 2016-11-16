# Points to take while building and hosting the application:
	* Please copy the login.conf and krb5.conf from the resources/spnega into the Server directory
	* To alter the uploadfilespath, alter the corresponding property in the pom file
	* The connecion string details for the test and production environments can be altered in the corresponding application.xml and context.xml under the resources/env folder
	* The categories and levels information can be modified in the webapp/public/js/data.json
	* To build jar for production use the command - "mvn -Pproduction install"
	* To build jar for test use the command - "mvn -Ptest install"