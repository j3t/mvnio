![Travis](https://travis-ci.org/j3t/mvn-io.svg?branch=master)

This project provides a webserver that implements the [Standard Repository Layout](https://cwiki.apache.org/confluence/display/MAVENOLD/Repository+Layout+-+Final),  
which basically means that it can be used to upload/download Maven-Artifacts with Maven 2 and 3.

# How it Works
The Maven-Artifacts are stored in S3 buckets, but any `S3 compatible storage` is sufficient (e.g. `MinIO`). `mvn-io` is 
a webserver which acts like a proxy so that they are accessible by Maven. The diagram below shows how maven client 
requests to `mvn-io` are mapped to the S3 storage.

![Basic](https://plantuml.j3t.urown.cloud/png/ootBKz2rKr3ABSlJpSnNKh1IS7SDKSWlKWW83Od9qyzDB4lDqwykIYt8ByuioI-ghDMlJYmgoKnBJ2wfvOBh0faGRAplcvddwGys8xN4FoahDRa4R58fb1EJbrIQd9rQOejidev2TcgbBRAX28EG78PY5v090000)

For example a `GET /maven/releases/com/github/j3t/mvnio/1.0.1/mvnio-1.0.1.pom` request to `mvn-io` results in a 
`GetObject(bucket:releases, key:com/github/j3t/mvnio/1.0.1/mvnio-1.0.1.pom)` request to `S3`.

# Security
`mvn-io` expects a `Basic Authorization` Header sent along with any client request. The credentials are not validated 
by `mvn-io` but used to access the corresponding bucket. Meaning, the client needs access to a valid S3 user, and the 
permissions per bucket are as follows:

* `s3:GetObject` - to download artifacts
* `s3:HeadObject` and `s3:PutObject` - to upload artifacts

Note: ***Clients can bypass the repository and modify artifacts in S3 directly!***

# Getting started
This example shows a `mvn-io` setup with `MinIO` as storage provider. For sake of simplicity, the example uses the MinIO 
admin user but any user with enougth permissions is sufficient (see [Security](#security)). You will also need installed 
`docker-compose` and `mvn` to execute the commands on your local machine.

* run `docker-compose up` start up `mvn-io` and `MinIO` as well
* run `docker-compose run --rm mc config host add minio http://minio:9000 admin long-password` once to register MinIO
* run `docker-compose run --rm mc mb minio/releases` to create a bucket named `releases`
* adjust your `~/.m2/settings.xml`:
```
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd">
  <servers>
    <server>
      <id>maven-releases</id>
      <username>admin</username>
      <password>long-password</password>
    </server>
  </servers>
</settings>
```
* create a file named `pom.xml` in an empty directory with the following content:
```
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>foo</groupId>
    <artifactId>bar</artifactId>
    <version>1.0.1</version>

    <distributionManagement>
        <repository>
            <id>maven-releases</id>
            <url>http://localhost:8181/maven/releases</url>
        </repository>
    </distributionManagement>
</project>
```
* run `mvn deploy`
```
...
[INFO] --- maven-deploy-plugin:2.7:deploy (default-deploy) @ bar ---
Uploading to maven-releases: http://localhost:8181/maven/releases/foo/bar/1.0.1/bar-1.0.1.jar
Uploaded to maven-releases: http://localhost:8181/maven/releases/foo/bar/1.0.1/bar-1.0.1.jar (1.4 kB at 1.1 kB/s)
Uploading to maven-releases: http://localhost:8181/maven/releases/foo/bar/1.0.1/bar-1.0.1.pom
Uploaded to maven-releases: http://localhost:8181/maven/releases/foo/bar/1.0.1/bar-1.0.1.pom (601 B at 5.1 kB/s)
Downloading from maven-releases: http://localhost:8181/maven/releases/foo/bar/maven-metadata.xml
Uploading to maven-releases: http://localhost:8181/maven/releases/foo/bar/maven-metadata.xml
Uploaded to maven-releases: http://localhost:8181/maven/releases/foo/bar/maven-metadata.xml (286 B at 2.1 kB/s)
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 2.917 s
[INFO] Finished at: 2020-10-23T13:22:45+02:00
[INFO] Final Memory: 12M/209M
[INFO] ------------------------------------------------------------------------
```

# Features
* fully implemented [Standard Repository Layout](https://cwiki.apache.org/confluence/display/MAVENOLD/Repository+Layout+-+Final)
* upload and download Maven artifacts
* multiple repositories
* stateless and thanks to S3 also horizontal scalable
* support for any S3 compatible storage provider 

# Roadmap
* the central repository can be mirrored
* alternative credential provider (e.g. `vault`)
    * storage provider is configurable per repository
    * external repositories can be proxied
* repository management via UI

# Pitfalls
* users accessKey/secretKey in S3/MinIO, are used as username/password in Maven (see `~/.m2/settings.xml`)
* users can bypass the repository and access objects directly in S3/MinIO

# Further reading
* [How does a Maven Repository work](https://blog.packagecloud.io/eng/2017/03/09/how-does-a-maven-repository-work/)
* [Spring webflux](https://docs.spring.io/spring-framework/docs/current/spring-framework-reference/web-reactive.html#webflux)
* [Amazon Async S3 client](https://docs.aws.amazon.com/sdk-for-java/v2/developer-guide/basics-async.html)