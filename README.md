![Travis](https://travis-ci.org/j3t/mvn-io.svg?branch=master) ![Docker](https://img.shields.io/docker/v/jtlabs/mvnio)

`mvnio` is a repository where Maven artifacts can be uploaded and downloaded. The webserver is implemented with 
reactive streams, or more specific with [Spring webflux](https://docs.spring.io/spring-framework/docs/current/spring-framework-reference/web-reactive.html#webflux)
and it uses S3 buckets as storage provider, but any `S3 compatible storage` is sufficient (e.g. `MinIO`).

# How it Works
Maven clients upload and download artifacts via the webserver, and the webserver takes care of that they are persisted 
in S3 buckets. The diagram below shows how maven client requests to `mvn-io` are mapped to the S3 storage.

![Basic](https://plantuml.j3t.urown.cloud/png/ootBKz2rKr3ABSlJpSnNKh1IS7SDKSWlKWW83Od9qyzDB4lDqwykIYt8ByuioI-ghDMlJYmgoKnBJ2wfvOBh0faGRAplcvddwGys8xN4FoahDRa4R58fb1EJbrIQd9rQOejidev2TcgbBRAX28EG78PY5v090000)

For example, if a client requests `GET /maven/releases/com/github/j3t/mvnio/1.0.1/mvnio-1.0.1.pom` from `mvnio` 
then, the object with key `com/github/j3t/mvnio/1.0.1/mvnio-1.0.1.pom` in bucket `releases` gets request from `S3`.

# Security
`mvnio` expects a `Basic Authorization` Header sent along with any client request. Client credentials will not be 
validated by `mvnio` but they will be required to access the corresponding bucket in S3. This means, the client needs 
access to S3 and the corresponding user needs proper permissions which are as follows:

* `s3:GetObject` - to download artifacts
* `s3:HeadObject` and `s3:PutObject` - to upload artifacts

Note: ***Clients can bypass the repository and modify artifacts in S3 directly!***

# Getting started
This example shows how `mvnio` can be setup with [MinIO](https://min.io/) as storage provider. It requires 
`docker-compose` and `mvn` installed properly and, for sake of simplicity, the MinIO admin user will be used to 
connect to S3/MinIO, but any user with proper permissions is sufficient (see [Security](#security)).

Let's get started:
* run `docker-compose up` start up `mvn-io` and `MinIO` as well (ports: 8181 and 9191)
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
The artifacts should now be visible in `MinIO` http://localhost:9191 

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