
To Reverse-Engineer an existing database use either of the following two methods:

1. Use the code generator Maven plugin: 

> mvn empire-db:codegen

For further information see comments in pom.xml or use

> mvn help:describe -DgroupId=org.apache.empire-db -DartifactId=empire-db-maven-plugin -Ddetail=true


2. Run org.apache.empire.db.codegen.CodeGenerator from the command line: 

> generate generate-config.xml C:\Maven\Repo
		