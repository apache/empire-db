
To Reverse-Engineer an existing database use either of the following two methods:

1. Use the code generator Maven plugin: 

> mvn clean generate-sources eclipse:clean eclipse:eclipse

Please see comments in pom.xml for further details

2. Run org.apache.empire.db.codegen.CodeGenerator from the command line: 

> generate generate-config.xml C:\Maven\Repo
	
