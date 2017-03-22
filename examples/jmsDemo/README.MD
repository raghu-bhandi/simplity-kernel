The following instructions will guide in setting up JMS on the glassfish 4.1.1 server. 
The asadmin command to create the QueueConnectionfactory(if default is not available) and the Queue

create-jms-resource --restype javax.jms.ConnectionFactory --description "default factory for subscriptions" --property ClientId=ID01 jms/__defaultConnectionFactory
create-jms-resource --restype javax.jms.Queue --property Name=Queue01 jms/Queue01

