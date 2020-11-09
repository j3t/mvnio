package testcontainers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

import java.util.UUID;

public class MinioContainer extends GenericContainer<MinioContainer> {

    public MinioContainer() {
        this("minio/minio");
    }

    public MinioContainer(String dockerImageName) {
        super(dockerImageName);

        setWaitStrategy(Wait.forHttp("/minio/health/ready").forStatusCode(200));

        withEnv("MINIO_ACCESS_KEY", UUID.randomUUID().toString());
        withEnv("MINIO_SECRET_KEY", UUID.randomUUID().toString());
        withCommand("server /data");
        withExposedPorts(9000);
        withNetwork(Network.newNetwork());  // we need a dedicated network otherwise mc cannot participate
    }

    public String accessKey() {
        return getEnvMap().get("MINIO_ACCESS_KEY");
    }

    public String secretKey() {
        return getEnvMap().get("MINIO_SECRET_KEY");
    }

    public String getExternalAddress() {
        return "http://localhost:" + getMappedPort(9000);
    }
}
