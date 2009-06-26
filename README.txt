Disclaimer
==========
Apache Empire-db is an effort undergoing incubation at The Apache Software Foundation (ASF),
sponsored by the Apache Incubator PMC. Incubation is required of all newly accepted
projects until a further review indicates that the infrastructure, communications,
and decision making process have stabilized in a manner consistent with other
successful ASF projects. While incubation status is not necessarily a reflection of
the completeness or stability of the code, it does indicate that the project has yet
to be fully endorsed by the ASF.


New Features in Release 2.0.5:
==============================

Empire-db Release 2.0.5 has the following major new features
- Maven project management
- New database handlers for Apache Derby, H2, PostgreSQL
- New Examples showing WebService integration using Apache CXF

For more details see the file CHANGELOG.txt or visit the Empire-db website at
http://incubator.apache.org/empire-db


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
	empire-db-user-subscribe@incubator.apache.org

Afterwards you may post a message to:
	empire-db-user@incubator.apache.org

Please visit our Website for more information.
http://incubator.apache.org/empire-db
