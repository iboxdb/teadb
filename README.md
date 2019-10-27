# TeaDB

```
mvn deploy:deploy-file -Dfile=iBoxDB-2.15.jar -DgroupId=iBoxDB -DartifactId=iBoxDB -Dversion=2.15 -Dpackaging=jar -Durl=file:./repository/ -DrepositoryId=repository -DupdateReleaseInfo=true
```
  
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
	<version>2.15</version>
  </dependency>
</dependencies>
```


