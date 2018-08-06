# Distributing Node

It's useful, sometimes, to distribute a nodejs with your artifact.

You might do this if you do not have the correct version of nodejs
installed on your target environment because it is not present at all,
or out of date.

## Defining the distributable node

Let's say you want to depend on nodejs v10.8.0 for Linux on x64.

You need to make a repository that can publish such a thing.

The repository might look like this:

```
+-- nodedist
|   +-- pom.xml
|   +-- src
|       +-- main
|           +-- assembly
|               +-- resources.xml
|           +-- resources
|               +-- node-v10.8.0-linux-x64.tar.xz
```

So that's only 3 files in total:

* `pom.xml`
* `resources.xml` in `src/main/assembly`
* `node-v10.8.0-linux-x64.tar.xz`

The node tar.xz file is the source node distribution file. I
downloaded mine from the nodejs site.

We have two other files, the `pom.xml` and the `assembly.xml`.

This stuff is maven magic and I think it's pretty easy to just copy
them and change a few things... but I'll try and explain.

## The POM

We're aiming for a build where we can say:

```
mvn install
```

or perhaps if we have an enterprise maven repository then:

```
mvn deploy
```

or some such. Personally I use [Daniel Flower's release
plugin](https://github.com/danielflower/multi-module-maven-release-plugin)
for maven because I know him and I can moan about it when it breaks.

So here's the `pom.xml` for a `mvn install`: 

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
 http://maven.apache.org/maven-v4_0_0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <groupId>uk.me.ferrier.nic.node-dist</groupId>  <!-- change this! -->
  <artifactId>node-linux-x64</artifactId>         <!-- and this!    -->
  <version>10.8.0</version>                       <!-- and this!    -->
  <packaging>pom</packaging>                      <!-- not this!    -->
  <name>nodejs for linux x64</name>               <!-- but this!    -->

  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-assembly-plugin</artifactId>
        <executions>
          <execution>
            <id>make shared resources</id>
            <goals>
              <goal>single</goal>
            </goals>
            <phase>package</phase>
            <configuration>
              <descriptors>
                <descriptor>src/main/assembly/resources.xml</descriptor>
              </descriptors>
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>

</project>
```

Note: it DOES NOT have a deploy or release plugin, you'd need to add
that if you wanted to publish to an enterprise maven repository.

That is pretty portable. The maven meta data is what you need to
uniquely identify the artifact.

In this case we're trying to identify a node build. It would be nice
if maven had architectural properties that we could embed here. I'm
not sure it does. Perhaps someone will enlighten me.

## Assembly.xml required

The other file you need is the assembly `resources.xml`. This needs to
be placed in a directory, I guess not the one where the resources to
be packaged (ie: the node distribution) actually are. That's why we've
put it in `src/main/assembly`. Note that the `pom.xml` refers to this
location.

The `resources.xml` file looks like this:

```xml
<assembly
    xmlns="http://maven.apache.org/plugins/maven-assembly-plugin/assembly/1.1.2" 
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://maven.apache.org/plugins/maven-assembly-plugin/assembly/1.1.2 
                        http://maven.apache.org/xsd/assembly-1.1.2.xsd">
  <id>resources</id>
  <formats>
    <format>zip</format>
  </formats>
  <includeBaseDirectory>false</includeBaseDirectory>
  <fileSets>
    <fileSet>
      <directory>src/main/resources</directory>
      <outputDirectory>/.node-dists</outputDirectory>
    </fileSet>
  </fileSets>
</assembly>

```

So with it's `fileset` xml element it points to the directory
containing the resource to be copied, relative to the `pom.xml`.

It also, in the `outputDirectory` element, specifies where the
artifact will be copied to, in the resulting zip file which will be
stored in the maven repository.

## Using the artifact

How do you depend and unpack such an artifact?

Here's the updated `pom.xml` from the [README](../README.md).

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
      <groupId>uk.me.ferrier.nic</groupId>
      <artifactId>jarnode</artifactId>
      <version>1.0-SNAPSHOT</version>
    </dependency>

    <!-- this bit is new -->
    <dependency>
      <groupId>uk.me.ferrier.nic.node-dist</groupId>
      <artifactId>node-linux-x64</artifactId>
      <version>10.8.0</version>
      <classifier>resources</classifier>
      <type>zip</type>
      <scope>provided</scope>
    </dependency>
    <!-- down to here-->
  </dependencies>
  
  <build>
    <resources>
      <resource>
        <directory>./</directory>
        <excludes>
          <exclude>target</exclude>
        </excludes>
      </resource>
      
      <!-- this bit is new - pulls the resources -->
      <resource>
        <directory>${project.build.directory}/generated-resources</directory>
        <filtering>true</filtering>
      </resource>
      <!-- end of that bit -->
    </resources>

    <plugins>
      <!-- this bit is new - the dependency plugin to get the depends -->
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-dependency-plugin</artifactId>
        <executions>
          <execution>
            <id>unpack-shared-resources</id>
            <goals>
              <goal>unpack-dependencies</goal>
            </goals>
            <phase>generate-resources</phase>
            <configuration>
             <outputDirectory>${project.build.directory}/generated-resources</outputDirectory>
             <includeArtifacIds>node-linux-x64</includeArtifacIds>
             <includeGroupIds>uk.me.ferrier.nic.node-dist</includeGroupIds>
             <excludeTransitive>true</excludeTransitive>
             <!--use as much as needed to be specific...also scope,type,classifier etc-->
            </configuration>
          </execution>
        </executions>
      </plugin>
      <!-- and back to what we had before -->
      
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

So with that `pom.xml` doing `mvn package` will make your node app,
compiled into a JAR with a .node-dist that will be automatically
unpacked and used on your target machine, providing you have a
NODE_DISTS environment variable pointing at a writeable directory.

## If everything goes right...

Your JAR'd package would look something like this:

```
M Filemode      Length  Date         Time      File
- ----------  --------  -----------  --------  -------------------------------------------------------------------------------------------------------------
  -rw-rw-rw-       179   6-Aug-2018  12:25:38  meta-inf/manifest.mf
  drwxrwxrwx         0   6-Aug-2018  12:25:38  meta-inf/
  -rw-rw-rw-      1053   6-Aug-2018  12:24:54  .exethings
  -rw-rw-rw-        45   6-Aug-2018  12:24:54  .gitignore
  drwxrwxrwx         0   6-Aug-2018  12:25:38  .node-dists/
  -rw-rw-rw-  22225907   6-Aug-2018  12:25:16  .node-dists/node-v10.8.0-linux-x64.tar.xz
  -rw-rw-rw-      3106   6-Aug-2018  12:24:54  dependency-reduced-pom.xml
  drwxrwxrwx         0   6-Aug-2018  12:25:42  node_modules/
  drwxrwxrwx         0   6-Aug-2018  12:25:42  node_modules/.bin/
  -rw-rw-rw-       149   6-Aug-2018  12:24:54  node_modules/.bin/mime
  -rw-rw-rw-       731   6-Aug-2018  12:24:54  node_modules/.bin/mkdirp
  -rw-rw-rw-      4092   6-Aug-2018  12:24:54  node_modules/.bin/semver
```

Obviously, that's truncated.

## Artifact size

This *is* a problem. With a nodejs packaged inside this simple
demonstration artifact is 25M big.

That will be pretty much the same with docker or any other artifact
layering system... but of course those systems have more size
efficiencies built in.


## Tips for putting this together in an Enterprise

You might want to not store node in git repo but make a build that
builds an artifact like this artifact.

So it would:

* download node from a known location
* maven build
* publish the resulting artifact to an internal artifact store

No need to store the version of nodejs internally at all, except in
the store.

### Parent POMs

One last trick that might make sense is to build a `pom.xml` that can
be inherited by other apps doing builds. This could express all the
dependency logic against a particular version and make life much
easier for your javascript developers.
