package testcontainers;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.testcontainers.containers.GenericContainer;

import java.io.IOException;

public class MinioMcContainer extends GenericContainer<MinioMcContainer> {

    private final MinioContainer minio;

    public MinioMcContainer(MinioContainer minio) {
        super("minio/mc");
        this.minio = minio;
        dependsOn(minio);
        withNetwork(minio.getNetwork());
        withCreateContainerCmdModifier(c -> c.withTty(true).withEntrypoint("/bin/sh"));
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        try {
            execMcCmd("mc config host add test-minio http://%s:9000 %s %s",
                    minio.getNetworkAliases().get(0),
                    minio.accessKey(),
                    minio.secretKey());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public ExecResult execMcCmd(String cmd, Object... args) throws IOException, InterruptedException {
        ExecResult result = execMcCmdUnsafe(cmd, args);
        if (result.getExitCode() != 0) {
            throw new AssertionError(result.getStderr());
        }
        return result;
    }

    public ExecResult execMcCmdUnsafe(String cmd, Object... args) throws IOException, InterruptedException {
        return execInContainer(String.format(cmd, args).split("\\s"));
    }

    public void deleteBucket(String bucket) throws IOException, InterruptedException {
        execMcCmdUnsafe("mc rb test-minio/%s --force", bucket);
    }

    public void createBucket(String bucket) throws IOException, InterruptedException {
        execMcCmd("mc mb test-minio/%s", bucket);
    }
}
