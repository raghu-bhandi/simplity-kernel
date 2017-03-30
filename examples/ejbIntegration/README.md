Following points give an overview on working of this example.

GlassFish 4.1.1 is used as server for deployment.

1. "Invoke helloworld" directly invokes and executes the helloworld service.
2. "Invoke Logic Service" invokes logicService and re-directs the flow to HelloLogic class where it has access to all EJBs.
3. Invoke servlet hello with below link http://localhost:8080/SimplityWithEJB/hello which makes a call to helloworld service and also access the EJBs from the servlet.

