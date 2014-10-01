About Empire-DB
===============

Apache Empire-db is a lightweight data access and persistence component for
relational databases based on JDBC.

For release details see the file CHANGELOG.txt or visit the Empire-db website at
http://empire-db.apache.org


Getting Started
===============

An introduction to Empire-db is provided with the tutorial.pdf document 
that can be found in the root directory of the distribution.  

This component requires a Java 5 JDK (or higher).


Building
========

To build Empire-db you need Apache Maven 2.0.9 or newer, which can be downloaded from 
	http://maven.apache.org/download.html. 
Make sure that your PATH includes the MVN_HOME/bin directory. 

In order to build Empire-db use the following command from the command line:
	> mvn clean install

There are various ways to open the project(s) in your favorite IDE:

=== Eclipse ===

 - From the command line:
   Change to the 'src' sub-directory and run the following command:   
   > mvn clean eclipse:eclipse -DdownloadSources=true 

   In Eclipse choose: Import... Existing projects into workspace

or 
 
 - Install the M2Eclipse plugin from
   http://m2eclipse.codehaus.org/
   
   In Eclipse choose: Import... Maven projects
   
=== Netbeans ===

 - Install the Maven plugin available in the update center (tools - plugins)
   Open the project like you open any other Netbeans project


Getting help
============

The best place for getting help is the empire-db user mailing list.
First subscribe to the list by sending an email to:
	user-subscribe@empire-db.apache.org

Afterwards you may post a message to:
	user@empire-db.apache.org

Please visit our Website for more information.
http://empire-db.apache.org
