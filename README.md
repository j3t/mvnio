[![Build Status](https://travis-ci.org/j3t/mvn-io.svg?branch=master)](https://travis-ci.org/j3t/mvn-io) 
[![Docker Tags](https://img.shields.io/docker/v/jtlabs/mvnio)](https://hub.docker.com/r/jtlabs/mvnio/tags)
 
`mvnio` is a repository for Maven artifacts which uses S3 buckets to store artifacts in scalable fashion. The 
underlying webserver is implemented with [reactive streams](https://www.reactive-streams.org/), or to be more specific 
by using [Spring webflux](https://docs.spring.io/spring-framework/docs/current/spring-framework-reference/web-reactive.html#webflux)
which uses [project-reactor](https://projectreactor.io/) under the hood. For the S3 connectivity, the [Amazon Async S3 client](https://docs.aws.amazon.com/sdk-for-java/v2/developer-guide/basics-async.html)
library is used which is also non-blocking, and supports back pressure. It also works well with other S3 compatible 
storage providers (e.g. `MinIO`).

# How it Works
The diagram below shows how `mvnio` handles maven client requests and how they are mapped to the S3 storage provider.

![Architecture](https://plantuml.j3t.urown.cloud/png/ootBKz2rKyWjoylCLx1IS7SDKSWlKWW83Od9qyzDB4lDqwykIYt8ByuioI-ghDMlJYmgoKnBJ2wfvO9e0UeDDWPfJ2tnJyfAJIu1Qo-5ScBoaagJirDBR94DqL78JgsqHJ89Q03C2GXJWGm0)

In general, a maven client interacts with a maven repository when it tries to `install` an artifact with a `GET` request 
and when it wants to `deploy` and artifact with a `PUT` request. For example, when a client requests 
`GET /maven/releases/foo/bar/1.0.1/bar-1.0.1.pom` from `mvnio` then `mvnio` tries to get an object 
with key `foo/bar/1.0.1/bar-1.0.1.pom` in bucket `releases` from `S3`.

# Configuration
[AppProperties](src/main/java/com/github/j3t/mvnio/AppProperties.java) contains a list of all available configuration 
parameters, and their default values.  

# Security
`mvnio` expects a `Basic Authorization` Header sent along with any client request. Client credentials will not be 
validated by `mvnio` but they will be required to access the corresponding bucket in S3. This means, the client needs 
access to S3, and the corresponding user needs proper permissions which are as follows:

* `s3:GetObject` - to download artifacts
* `s3:HeadObject` and `s3:PutObject` - to upload artifacts

Note: ***Clients can bypass the repository and modify artifacts in S3 directly!***

# Getting started
This example shows how `mvnio` can be setup with [MinIO](https://min.io/) as storage provider. It requires 
`docker-compose` and `mvn` installed properly and, for sake of simplicity, the MinIO admin user will be used to 
connect to S3/MinIO, but any user with proper permissions is sufficient (see [Security](#security)).

Let's get started:
* run `docker-compose up` to start up `mvnio` and `MinIO` as well (ports: 8080 and 9000)
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
            <url>http://localhost:8080/maven/releases</url>
        </repository>
    </distributionManagement>
</project>
```
* run `mvn deploy`
```
...
[INFO] --- maven-deploy-plugin:2.7:deploy (default-deploy) @ bar ---
Uploading to maven-releases: http://localhost:8080/maven/releases/foo/bar/1.0.1/bar-1.0.1.jar
Uploaded to maven-releases: http://localhost:8080/maven/releases/foo/bar/1.0.1/bar-1.0.1.jar (1.4 kB at 1.1 kB/s)
Uploading to maven-releases: http://localhost:8080/maven/releases/foo/bar/1.0.1/bar-1.0.1.pom
Uploaded to maven-releases: http://localhost:8080/maven/releases/foo/bar/1.0.1/bar-1.0.1.pom (601 B at 5.1 kB/s)
Downloading from maven-releases: http://localhost:8080/maven/releases/foo/bar/maven-metadata.xml
Uploading to maven-releases: http://localhost:8080/maven/releases/foo/bar/maven-metadata.xml
Uploaded to maven-releases: http://localhost:8080/maven/releases/foo/bar/maven-metadata.xml (286 B at 2.1 kB/s)
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 2.917 s
[INFO] Finished at: 2020-10-23T13:22:45+02:00
[INFO] Final Memory: 12M/209M
[INFO] ------------------------------------------------------------------------
```
The artifacts should now be available in `MinIO`.
* open http://localhost:9000 in your browser (accessKey: `admin`, secretKey: `long-password`)

# Features
* fully implemented [Standard Repository Layout](https://cwiki.apache.org/confluence/display/MAVENOLD/Repository+Layout+-+Final)
* upload and download Maven artifacts
* multiple repositories
* stateless and thanks to S3 also horizontal scalable
* support for any S3 compatible storage provider 

# Roadmap
* S3 bucket End-To-End test
* mirror for central
* alternative credential provider (e.g. `vault`)
    * storage provider is configurable per repository
    * proxy for external repositories
* repository management via UI

# Pitfalls
* users accessKey/secretKey in S3, are used as username/password in Maven (see `~/.m2/settings.xml`)
* users can bypass the repository and access objects directly in S3

# Further reading
* [How does a Maven Repository work](https://blog.packagecloud.io/eng/2017/03/09/how-does-a-maven-repository-work/)
* [Spring webflux](https://docs.spring.io/spring-framework/docs/current/spring-framework-reference/web-reactive.html#webflux)
* [Amazon Async S3 client](https://docs.aws.amazon.com/sdk-for-java/v2/developer-guide/basics-async.html)