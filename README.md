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

This pom has shade and a copy resources task which will copy the
current directory to the *right* maven resources directory so that
it's included correctly in the resulting uberjar:

```xml
<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd"
         xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <groupId>uk.me.ferrier.nic</groupId>
  <artifactId>nodetest</artifactId>
  <packaging>jar</packaging>
  <version>1.0-SNAPSHOT</version>
  <name>jarnode</name>
  <url>http://maven.apache.org</url>
  <dependencies>
    <dependency>
      <groupId>junit</groupId>
      <artifactId>junit</artifactId>
      <version>3.8.1</version>
      <scope>test</scope>
    </dependency>
  </dependencies>
  <build>
    <resources>
      <resource>
        <directory>src/main/resources</directory>
        <excludes>
          <exclude>target</exclude>
          <exclude>nodeapp/src/**</exclude>
        </excludes>
      </resource>
    </resources>
    <plugins>
      <plugin>
        <artifactId>maven-resources-plugin</artifactId>
        <version>3.1.0</version>
        <executions>
          <execution>
            <id>copy-resources</id>
            <phase>validate</phase>
            <goals>
              <goal>copy-resources</goal>
            </goals>
            <configuration>
              <outputDirectory>${basedir}/src/main/resources/nodeapp</outputDirectory>
              <resources>          
                <resource>
                  <directory>./</directory>
                  <filtering>true</filtering>
                </resource>
              </resources>              
            </configuration>            
          </execution>
        </executions>
      </plugin>
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
  drwxrwxrwx         0   9-May-2018  23:34:42  nodeapp/node_modules/xtend/
  -rw-rw-rw-       545   9-May-2018  23:34:36  nodeapp/node_modules/xtend/.jshintrc
  -rw-rw-rw-        13   9-May-2018  23:34:36  nodeapp/node_modules/xtend/.npmignore
  -rw-rw-rw-      1056   9-May-2018  23:34:36  nodeapp/node_modules/xtend/LICENCE
  -rw-rw-rw-        49   9-May-2018  23:34:36  nodeapp/node_modules/xtend/Makefile
  -rw-rw-rw-       725   9-May-2018  23:34:36  nodeapp/node_modules/xtend/README.md
  -rw-rw-rw-       384   9-May-2018  23:34:36  nodeapp/node_modules/xtend/immutable.js
  -rw-rw-rw-       369   9-May-2018  23:34:36  nodeapp/node_modules/xtend/mutable.js
  -rw-rw-rw-      1857   9-May-2018  23:34:36  nodeapp/node_modules/xtend/package.json
  -rw-rw-rw-      1760   9-May-2018  23:34:36  nodeapp/node_modules/xtend/test.js
  -rw-rw-rw-     54344   9-May-2018  23:34:36  nodeapp/package-lock.json
  -rw-rw-rw-       791   9-May-2018  23:34:36  nodeapp/package.json
  -rw-rw-rw-      2751   9-May-2018  23:34:36  nodeapp/pom.xml
  -rw-rw-rw-        22   9-May-2018  23:34:36  nodeapp/server.js
  drwxrwxrwx         0   9-May-2018  23:34:42  META-INF/maven/
  drwxrwxrwx         0   9-May-2018  23:34:42  META-INF/maven/uk.me.ferrier.nic/
  drwxrwxrwx         0   9-May-2018  23:34:42  META-INF/maven/uk.me.ferrier.nic/nodetest/
  -rw-rw-rw-      2727   9-May-2018  23:31:06  META-INF/maven/uk.me.ferrier.nic/nodetest/pom.xml
  -rw-rw-rw-       117   9-May-2018  23:34:36  META-INF/maven/uk.me.ferrier.nic/nodetest/pom.properties
- ----------  --------  -----------  --------  ------------------------------------------------------------------------------------------------------
               7791496                         2226 files
```

which is near perfect.


## Can the template be simpler?

I'm sure a simpler maven plugin (nodejs?) could be made by combining
the code for copy and shade.

