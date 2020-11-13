#### copy project dependencies to a local folder
`mvn dependency:copy-dependencies -Dmdep.useRepositoryLayout=true -DoutputDirectory=local-repo`

#### upload artifact (primary artifact)
`mvn deploy:deploy-file -DpomFile=netty-transport-native-epoll-4.1.53.Final.pom -Dfile=netty-transport-native-epoll-4.1.53.Final-linux-x86_64.jar -Dclassifier=linux-x86_64 -DrepositoryId=maven-releases -Durl=http://localhost:8080/maven/releases`

#### upload artifact (secondary artifact)
`mvn deploy:deploy-file -Dfile=netty-transport-native-epoll-4.1.53.Final-linux-x86_64.jar -DgroupId=io.netty -DartifactId=netty-transport-native-epoll -Dversion=4.1.53.Final -Dclassifier=linux-x86_64 -DgeneratePom=false -DrepositoryId=maven-releases -Durl=http://localhost:8080/maven/releases`

#### upload project dependencies (primary artifacts)
`find local-repo -iname '*.jar' -type f | while read NAME ; do mvn deploy:deploy-file -DpomFile=${NAME%.jar}.pom -Dfile=${NAME} -DrepositoryId=maven-releases -Durl=http://localhost:8080/maven/releases ; done`
