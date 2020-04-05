### Maven Deploy & Benchmark with MySQL



#### Run
```
//close all IDE first
//Java 11 Version
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk

mvn clean
mvn install -e
mvn exec:java
```


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



#### Maven
```xml
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
	<version>2.17</version>
  </dependency>
</dependencies>
```


#### Install

```
mvn deploy:deploy-file -Dfile=iBoxDB-2.17.jar -DgroupId=iBoxDB -DartifactId=iBoxDB -Dversion=2.17 -Dpackaging=jar -Durl=file:./repository/ -DrepositoryId=repository -DupdateReleaseInfo=true
```

