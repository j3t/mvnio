package testcontainers;

import static java.lang.String.format;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.testcontainers.containers.GenericContainer;

import java.io.IOException;
import java.util.UUID;

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
            execSecure("mc config host add test-minio http://%s:9000 %s %s",
                    minio.getNetworkAliases().get(0),
                    minio.accessKey(),
                    minio.secretKey());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public void execSecure(String command, Object... args) {
        ExecResult result = exec(command, args);

        if (result.getExitCode() != 0) {
            throw new AssertionError(result.getStderr());
        }
    }

    public ExecResult exec(String command, Object... args) {
        try {
            return execInContainer("/bin/sh", "-c",  format(command, args));
        } catch (IOException|InterruptedException e) {
            throw new AssertionError("exec has been failed!", e);
        }
    }

    public void deleteBucket(String bucket) {
        exec("mc rb test-minio/%s --force", bucket);
    }

    public void createBucket(String bucket) {
        execSecure("mc mb test-minio/%s", bucket);
    }

    public void createObject(String bucket, String key, String content) {
        execSecure("echo -n \"%s\" | mc pipe test-minio/%s/%s", content, bucket, key);
    }

    public void createObject(String bucket, String key) {
        createObject(bucket, key, UUID.randomUUID().toString());
    }

    public void createUser(String username, String password) {
        execSecure("mc admin user add test-minio %s %s", username, password);
    }

    public void applyReadonlyPolicy(String username) {
        applyPolicy("readonly", username);
    }

    public void applyReadwritePolicy(String username) {
        applyPolicy("readwrite", username);
    }

    public void applyPolicy(String policy, String username) {
        execSecure("mc admin policy attach test-minio %s --user=%s", policy, username);
    }

    public void removeUser(String username) {
        ExecResult result = exec("mc admin user remove test-minio %s", username);

        if (result.getExitCode() == 0) {
            return;
        }

        if (result.getExitCode() == 1) {
            if (result.getStderr().contains("The specified user does not exist.")) {
                return;
            }
        }

        throw new AssertionError(result.getStderr());
    }
}
