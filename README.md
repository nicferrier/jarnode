# Package Node apps with Java's Maven

Working in an enterprise Java is king. We have rules about storing
things in artifact repositories like Sonatype's Nexus or Artifactory
for Maven.

I have stolen some secret HSBC passwords and they all begin with "secret"

It would be great to store nodejs apps in Maven repositories because
we write a lot of deployment tools around such repositories.

## nodejs uberjars

This is a library to support making uberjars from nodejs projects so
that you can just:

```
java -jar nodejs-uberjar-1.0.16.jar
```

and your node app will run.

## What's the latest version?

```xml
<dependency>
  <groupId>uk.me.ferrier.nic</groupId>
  <artifactId>jarnode</artifactId>
  <version>1.0.26</version>
</dependency>
```


## How does it work?

You need this POM as pom.xml in your nodejs project:

```xml
<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd"
         xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>

  <groupId>uk.me.ferrier.nic</groupId>              <!-- change this!                  -->
  <artifactId>nodetest</artifactId>                 <!-- and this!                     -->
  <packaging>jar</packaging>
  <version>1.0-SNAPSHOT</version>                   <!-- you'll be changing this too!  -->
  <name>nodetest</name>                             <!-- and the project name          -->
  <url>https://github.com/nicferrier/package</url>  <!-- and the url!                  -->

  <!-- Nothing else should be changed!!                                                -->

  <dependencies>
    <dependency>
      <groupId>uk.me.ferrier.nic</groupId>
      <artifactId>jarnode</artifactId>
      <version>1.0.26</version>
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

### What should I change the values to?

The values that need changing are the ones that describe the resulting
artifact, your nodejs app.

```xml
  <groupId>uk.me.ferrier.nic</groupId>              <!-- change this!                  -->
  <artifactId>nodetest</artifactId>                 <!-- and this!                     -->
  <packaging>jar</packaging>
  <version>1.0-SNAPSHOT</version>                   <!-- you'll be changing this too!  -->
  <name>nodetest</name>                             <!-- and the project name          -->
  <url>https://github.com/nicferrier/package</url>  <!-- and the url!                  -->
```

So choose a maven groupid and artifactid that follow Maven
conventions, but describe your node app.

Obviously use the url of your github project, or whatever your
internal VCS is.

## Once I have my POM how do I make the uberjar?

It's super easy. Just go:

```
mvn clean package
```

and you'll get your uberjar.


## Once I have my uberjar how can I run it?

If you built it with just `mvn package` then you can run it by:

```
cd target
java -jar nodetest-1.0-SNAPSHOT.jar
```


## How do I deploy my uberjar?

Usually your Maven setup will have some sort of release plugin that
you can call to deploy to a Nexus repository.

I work with the lovely @danielflower so I use
[this](https://github.com/danielflower/multi-module-maven-release-plugin).

Ask your local Maven maven for help.


## I am missing executable permissions on some of my files!

Yes. Jar files don't carry this data. So the jarnode uberjar bootstrap
program also will switch on the exe permission for any file it finds
in the file `.exethings`.

A typical build.sh would look like this:

```
npm install
find . -executable -type f > .exethings
mvn package
```

The `.exethings` could be constructed lots of ways of course.


## What does the resulting JAR look like?

Here's a snippet from the end of one of mine. Notice that everything
is packed inside, node_modules and all.

That's what you'd expect, right? The node_modules are dependencies.

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

The Java classes in there are the bootstrapping ones that do the
automatic extraction of the app on exec.

## When it unpacks do we leave a mess everywhere?

The jar unpacks to the same directory as the jar is in into a
directory with a leading `.`.

eg:

```
/home/app $ java -jar nodeuber-3.0.11.jar
```

will create a directory called: `/home/app/.nodeuber-3.0.11.jar` and
the node app will exist under there.

If you run the app again it will unpack to the same directory,
deleting the directory first.


## I lost state I stored in the nodeapp when I restarted!

Yes. Because when you restart we delete the directory. 

Make a containing directory and store the state there?

Or work out another way of doing it and tell us?


## How do I get the right version of node onto the target machine?

jarnode will help you install a target version of node, if you package
node in your jarnode artifact.

As jarnode unpacks your app it:

* looks for a `.node-dist` directory
* if it exists and it contains a `.tar.xz` file beginning with `node-`
* if there is a NODE_DISTS environment variable that points to a directory that exists
* then the node distribution is unpacked into that directory
* but if the node distribution is already there the copy is abandoned

Further documentation on this is available in [NODEDISTS](docs/NODEDISTS.md)

### node dist-less artifacts

If a distribution is not delivered in the artifact then jarnode
attempts to use a node from PATH.


## Could this all be easier?

Of course. There are many ways to do most things.

But this way is highly maven, and therefore, enterprise, empathetic.

## See Also

I've put a demo nodejs distribution template [here](https://github.com/nicferrier/jarnode-node-dist-demo).

And I've put a demo JarNode template node app [here](https://github.com/nicferrier/jarnode-demo/).


## Acknowledgements

@rajshahuk had this idea in the first place.

@danielflower helped with the maven and the uploading to central.maven.com

@jameslockwood has helped by being a *real* Javascript programmer that
wants to use it. He's contributed patches that are wonderful.
