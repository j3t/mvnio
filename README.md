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
* start `mvn-io` and connect it to S3
    * property: `s3.endpoint` (default: `http://localhost:9000`)
* create a S3 bucket (name: `releases`)
* create a S3 user (accessKey: `release-user`, secretKey: `wJa4rXUtnF8MI`)
* make sure the user has access to the bucket and can read and write objects (the policy below is sufficient):
```
{
 "Version": "2012-10-17",
 "Statement": [
  {
   "Effect": "Allow",
   "Action": [
    "s3:HeadObject",
    "s3:GetObject",
    "s3:PutObject"
   ],
   "Resource": [
    "arn:aws:s3:::releases/*"
   ]
  }
 ]
}
```
* prepare your `~/.m2/settings.xml` like:
```
<settings>
  <servers>
    <server>
      <id>release-repo</id>
      <username>release-user</username>
      <password>wJa4rXUtnF8MI</password>
    </server>
  </servers>
</settings>
```
* connect your project to the repository:
```
<project>
    <groupId>foo</groupId>
    <artifactId>bar</artifactId>
    <version>1.0.1</version>

    <distributionManagement>
        <repository>
            <id>release-repo</id>
            <url>https://<your-domain>/maven/releases</url>
        </repository>
    </distributionManagement>
    ...
```
* run `mvn deploy`
```
...
[INFO] --- maven-deploy-plugin:2.8.2:deploy (default-deploy) @ bar ---
Uploading to release-repo: https://mvn-io.cloud/maven/releases/foo/bar/1.0-1/bar-1.0-1.jar
Uploaded to release-repo: https://mvn-io.cloud/maven/releases/foo/bar/1.0-1/bar-1.0-1.jar (3.3 kB at 42 kB/s)
Uploading to release-repo: https://mvn-io.cloud/maven/releases/foo/bar/1.0-1/bar-1.0-1.pom
Uploaded to release-repo: https://mvn-io.cloud/maven/releases/foo/bar/1.0-1/bar-1.0-1.pom (3.1 kB at 48 kB/s)
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 4.633 s
[INFO] Finished at: 2020-10-18T10:57:00+02:00
[INFO] Final Memory: 25M/339M
[INFO] ------------------------------------------------------------------------
```

# Features
* fully implemented [Standard Repository Layout](https://cwiki.apache.org/confluence/display/MAVENOLD/Repository+Layout+-+Final)
* upload and download Maven artifacts
* multiple repositories
* stateless and thanks to S3 also horizontal scalable
* support for any S3 compatible storage provider 

# Roadmap
* Travis executes tests continuously 
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