### Maven Deploy & Benchmark with MySQL



#### Run
```
//close all IDE first
//Java 11 Version
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk

mvn clean
mvn install
mvn exec:java
```


##### Results

**VM 2Cores + 8G**

```sql
threadCount= 100,000 batchCount= 10 

iBoxDB
Database Transaction Test: Succeeded
iBoxDB Insert: 1,000,000 AVG: 47,016 objects/s 
iBoxDB Update: 1,000,000 AVG: 25,558 objects/s 
iBoxDB Delete: 1,000,000 AVG: 42,714 objects/s 

MySQL
Database Transaction Test: Succeeded
MySQL  Insert: 1,000,000 AVG: 5,514 objects/s 
MySQL  Update: 1,000,000 AVG: 5,109 objects/s 
MySQL  Delete: 1,000,000 AVG: 6,044 objects/s 
```


Welcome post Results to Issues


#### Maven pom.xml

```xml
<project>
    <repositories>
        <repository>
            <id>repository</id>
            <url>https://github.com/iboxdb/teadb/raw/repository</url>
        </repository>
    </repositories>

    <dependencies>
        <dependency>
            <groupId>iBoxDB</groupId>
            <artifactId>iBoxDB</artifactId>
            <version>2.27</version>
        </dependency>
    </dependencies>
</project>
```



#### Local Deploy Jar

```
mvn deploy:deploy-file -Dfile=iBoxDB-2.27.jar -DgroupId=iBoxDB -DartifactId=iBoxDB -Dversion=2.27 -Dpackaging=jar -Durl=file:./repository/ -DrepositoryId=repository -DupdateReleaseInfo=true
```


#### Remote Deploy Jar

```
mvn deploy:deploy-file -Dfile=iBoxDB-2.27.jar -DgroupId=iBoxDB -DartifactId=iBoxDB -Dversion=2.27 -Dpackaging=jar -Durl=https://maven.pkg.github.com/iboxdb/teadb  -DupdateReleaseInfo=true -DrepositoryId=github
```


#### Setup Remote Maven for Remote Deploy

```sh
[user@localhost ~]$ vi .m2/settings.xml 
```

```xml
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                      http://maven.apache.org/xsd/settings-1.0.0.xsd">

  <activeProfiles>
    <activeProfile>github</activeProfile>
  </activeProfiles>

  <profiles>
    <profile>
      <id>github</id>
      <repositories>
        <repository>
          <id>central</id>
          <url>https://repo1.maven.org/maven2</url>
          <releases><enabled>true</enabled></releases>
          <snapshots><enabled>true</enabled></snapshots>
        </repository>
        <repository>
          <id>github</id>
          <name>GitHub OWNER Apache Maven Packages</name>
          <url>https://maven.pkg.github.com/iboxdb/teadb</url>
        </repository>
      </repositories>
    </profile>
  </profiles>

  <servers>
    <server>
      <id>github</id>
      <username>USERNAME</username>
      <password>TOKEN</password>
    </server>
  </servers>
</settings>

```

[You need an access token to install packages in GitHub Packages.](https://help.github.com/en/github/authenticating-to-github/creating-a-personal-access-token-for-the-command-line)

[Installing a package](https://help.github.com/en/packages/using-github-packages-with-your-projects-ecosystem/configuring-apache-maven-for-use-with-github-packages#installing-a-package)



