When one works in an enterprise it would be super useful if it was
possible to package a node app inside a maven wrapper.

This should be very easy. Java has the concept of an uberjar, a single
jar where everything is packaged up (incuding non-class files) and is
executable.

So this is an attempt to make a library that can then be used in maven
to package a node app.

The executable program in this library will be used to extract and
execute the node app.

## How to make a pom for a node project

This pom has shade and a resources definitions which make the node app
packagable and runnable by jarnode:

```xml
<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd"
         xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <groupId>uk.me.ferrier.nic</groupId>
  <artifactId>nodetest</artifactId>
  <packaging>jar</packaging>
  <version>1.0-SNAPSHOT</version>
  <name>nodetest</name>
  <url>https://github.com/nicferrier/jarnode</url>
  <dependencies>
    <dependency>
      <groupId>uk.me.ferrier.nic</groupId>
      <artifactId>jarnode</artifactId>
      <version>1.0-SNAPSHOT</version>
    </dependency>
  </dependencies>
  
  <build>
    <resources>
      <resource>
        <directory>./</directory>
        <excludes>
          <exclude>target</exclude>
        </excludes>
      </resource>
    </resources>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-shade-plugin</artifactId>
        <version>3.1.1</version>
        <executions>
          <execution>
            <phase>package</phase>
            <goals>
              <goal>shade</goal>
            </goals>
            <configuration>
              <artifactSet>
                <excludes>
                  <exclude>junit:junit</exclude>
                  <exclude>jmock:*</exclude>
                  <exclude>*:xml-apis</exclude>
                  <exclude>org.apache.maven:lib:tests</exclude>
                  <exclude>log4j:log4j:jar:</exclude>
                </excludes>
              </artifactSet>
              <filters>
                <filter>
                  <artifact>*:*</artifact>
                  <includes>
                    <include>**</include>
                  </includes>
                </filter>
              </filters>
              <transformers>
                <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                  <mainClass>uk.me.ferrier.nic.jarnode.App</mainClass>
                </transformer>
              </transformers>
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
</project>
```

One needs to be able to exclude certain directories from the top level
copy (for example the target directory, and the resulting src
directory should not get copied).

An example nodeapp built like this with the above pom gets a jar file that looks like this:

```
  -rw-rw-rw-      1056  10-May-2018  10:10:56  node_modules/xtend/LICENCE
  -rw-rw-rw-        49  10-May-2018  10:10:56  node_modules/xtend/Makefile
  -rw-rw-rw-       725  10-May-2018  10:10:56  node_modules/xtend/README.md
  -rw-rw-rw-       384  10-May-2018  10:10:56  node_modules/xtend/immutable.js
  -rw-rw-rw-       369  10-May-2018  10:10:56  node_modules/xtend/mutable.js
  -rw-rw-rw-      1857  10-May-2018  10:10:56  node_modules/xtend/package.json
  -rw-rw-rw-      1760  10-May-2018  10:10:56  node_modules/xtend/test.js
  -rw-rw-rw-     54344  10-May-2018  10:10:56  package-lock.json
  -rw-rw-rw-       791  10-May-2018  10:10:56  package.json
  -rw-rw-rw-      2958  10-May-2018  10:10:56  pom.xml
  -rw-rw-rw-        22  10-May-2018  10:10:56  server.js
  drwxrwxrwx         0  10-May-2018  10:11:00  META-INF/maven/
  drwxrwxrwx         0  10-May-2018  10:11:00  META-INF/maven/uk.me.ferrier.nic/
  drwxrwxrwx         0  10-May-2018  10:11:00  META-INF/maven/uk.me.ferrier.nic/nodetest/
  -rw-rw-rw-      2958  10-May-2018  10:10:32  META-INF/maven/uk.me.ferrier.nic/nodetest/pom.xml
  -rw-rw-rw-       117  10-May-2018  10:10:56  META-INF/maven/uk.me.ferrier.nic/nodetest/pom.properties
  -rw-rw-rw-        37   9-May-2018  22:31:58  testfile.js
  drwxrwxrwx         0  10-May-2018  10:11:00  uk/
  drwxrwxrwx         0  10-May-2018  10:11:00  uk/me/
  drwxrwxrwx         0  10-May-2018  10:11:00  uk/me/ferrier/
  drwxrwxrwx         0  10-May-2018  10:11:00  uk/me/ferrier/nic/
  -rw-rw-rw-      1733  10-May-2018  10:11:00  uk/me/ferrier/nic/App.class
  drwxrwxrwx         0  10-May-2018  10:11:00  META-INF/maven/uk.me.ferrier.nic/jarnode/
  -rw-rw-rw-      4326  10-May-2018  00:17:54  META-INF/maven/uk.me.ferrier.nic/jarnode/pom.xml
  -rw-rw-rw-       116   9-May-2018  22:17:42  META-INF/maven/uk.me.ferrier.nic/jarnode/pom.properties
- ----------  --------  -----------  --------  ----------------------------------------------------------------------------------------------
               7756202                         2235 files
```

so the jar contains the correct files including the jarnode bootstrap class "App".


## Can the template be simpler?

I'm sure a simpler maven plugin (nodejs?) could be made by combining
the code for copy and shade.

