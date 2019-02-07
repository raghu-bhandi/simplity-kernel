# simplity - moved to https://github.com/simplity/
ridiculously simple development platform for micro services 

simplity is not like anything else you have seen in the frameworks world. So, if you try to understand it in comparison to say, Spring framework, you will be lost completely. But, let us say you start from your project specific context:
- system of records to be maintained
- set of business rules that govern
     - the way these records are accessed
     - logic to be used to calculate data items 
     - privileges and access control
     - work-flow for creation and modification of documents/records
- needs of clients reaching out to you from the web and other channels
- changing needs of web-technologies on the client side
     - you thought you have implemented the bleeding-edge last year, but people are already referring to it as legacy
- deployment architecture that improves regularly, but demands behavioral changes from your application

Then you will love the way simplity guides you to develop your micro services with hardly any new concepts/syntax to learn. 
You will see that:
 - you quickly dive into your data-model and micro services without even knowing anything about deployment scenario
 - each micro service can be built and tested independent of the other
 - your micro service is assembled using nano components
 - nano components have such simple touch-points that they are
     - easily re-used across services
     - tested on their own
     - service are tested for functionality as well as performance

You use your favorite IDE like eclipse to develop all these as a simple dynamic-web-project, and host it on any web-container.
Meanwhile, as your colleagues work on the deployment architecture you will be able to add simple connectors/drivers/agents that transform your application to the deployment needs,  without touching your services
- implement multi level caching
- internationalization and localization
- integrate with 
     - centralized document storage infrastructure
     - SSO like LDAP
- Production Support Systems to handle system errors and crashes
- deploy in a server-farm behind a load-balancer, or deploy it on cloud.

And, one more thing  :-) :-)
You write far less Java code than you would imagine.


simplity provides a set of nano components that do most of your errands so that you focus primarily on project-specific logic.


simplity is based on the concepts used in Exility(https://github.com/ExilantTechnologies/ExilityCore-5.0.0).
It is a complete re-write of the same with a view to improve extensibility using plugins.
