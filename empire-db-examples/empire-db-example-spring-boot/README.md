# Empire-DB Spring Boot Example

This example uses the convenience configuration options provided by Spring Boot.
Instead of `config.xml` all configuration can be found in `application.yml`.

This example builds an executable JAR you can build and execute from this folder like this:

```sh
$ mvn clean install
$ java -jar target/empire-db-example-spring-boot-3.0.0-SNAPSHOT.jar
```

An embedded hsqldb is used by default.