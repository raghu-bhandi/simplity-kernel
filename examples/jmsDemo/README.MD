The following instructions will guide in setting up JMS on the glassfish 4.1.1 server. 
The asadmin command to create the QueueConnectionfactory(if default is not available) and the Queue

create-jms-resource --restype javax.jms.ConnectionFactory --description "default factory for subscriptions" --property ClientId=ID01 jms/__defaultConnectionFactory
create-jms-resource --restype javax.jms.Queue --property Name=Queue01 jms/Queue01


The following instructions will guide in setting up JMS on the wildfly 10.0 server. 
Follow the below steps to create Connection Factory and Queue in the wildfly server:
1. Start the wildfly server. 
2. From the browser, go to wildfly console screen --> Configuration --> Subsystems --> Messaging - ActiveMQ --> select default --> click on Queues/Topics.
3. Select Connection Factories --> click on Add --> enter Name, JNDI Name, Connectors. --> click on Save button.
4. Select Queues/Topics --> click on Add --> enter Name, JNDI Names --> click on Save button.

After creating Connection Factory and Queue, update the following files in the jmsDemo application,
1. Update the queueConnectionFactory field in application.xml file.
2. Update the queueName field in jmsProducer.xml and jmsConsumer.xml file.




