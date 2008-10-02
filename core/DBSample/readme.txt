Apache Empire-db
Copyright 2008 The Apache Software Foundation

In order to run / debug the Empire-db SampleApp from the Eclipse IDE please do the following:

1. In Eclipse IDE use File -> Import -> Existing Projects into Workspace to import the Sample Project

2. Open the file org.apache.empire.samples.db.SampleApp.java in the src folder and set a breakpoint in first code line of the main() function.
 
3. Then right-click on the DBSample Project Node and select Debug-As -> 3 Java Application and select the "SampleApp" class

The Sample uses a HSQLDB database. If you want to use another database system you will have to add the driver jar file to the the classpath
and change the config.xml file accordingly.
