package com.github.j3t.mvnio.repo;

import com.github.j3t.mvnio.AppProperties;
import com.github.j3t.mvnio.error.ArtifactAlreadyExistsException;
import com.github.j3t.mvnio.error.ArtifactPathNotValidException;
import com.github.j3t.mvnio.error.NotAuthorizedException;
import com.github.j3t.mvnio.storage.S3Repository;
import lombok.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.nio.ByteBuffer;

import static com.github.j3t.mvnio.repo.AWSContextFilter.AWS_CREDENTIALS_PROVIDER;
import static com.github.j3t.mvnio.repo.RepositoryHelper.getMediaType;
import static com.github.j3t.mvnio.repo.RepositoryHelper.isArtifactPath;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;

@RestController
@RequestMapping("/maven")
public class RepositoryController {

    private final S3Repository s3Repository;
    private final AppProperties appProperties;

    public RepositoryController(S3Repository s3Repository, AppProperties appProperties) {
        this.s3Repository = s3Repository;
        this.appProperties = appProperties;
    }

    @PutMapping(value = "/{repository}/{*artifactPath}")
    public Mono<ResponseEntity<Void>> upload(
            @RequestHeader(value = "content-type", required = false) MediaType contentType,
            @RequestHeader(value = "content-length") long contentLength,
            @PathVariable String repository,
            @PathVariable String artifactPath,
            @RequestBody Flux<ByteBuffer> file) {

        return Mono.subscriberContext()
                .map(ctx -> credentials(ctx, repository))
                .flatMap(credentials -> checkUpload(credentials, repository, artifactPath).thenReturn(credentials))
                .flatMap(credentials -> s3Repository.fileUpload(
                        credentials,
                        repository,
                        key(artifactPath),
                        computeContentType(contentType, artifactPath),
                        contentLength,
                        file))
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).build());
    }

    @GetMapping(value = "/{repository}/{*artifactPath}")
    public Mono<ResponseEntity<Flux<ByteBuffer>>> download(@PathVariable String repository,
                                                           @PathVariable String artifactPath) {

        return Mono.subscriberContext()
                .flatMap(ctx -> s3Repository.fileDownload(
                        credentials(ctx, repository),
                        repository,
                        key(artifactPath)))
                .map(result -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_TYPE, result.sdkResponse.contentType())
                        .header(HttpHeaders.CONTENT_LENGTH, Long.toString(result.sdkResponse.contentLength()))
                        .body(result.flux));
    }

    /**
     * Checks that a given artifact not already exists and can be uploaded. Maven metadata files are ignored.
     *
     * @throws ArtifactAlreadyExistsException if the artifact already exists.
     * @throws ArtifactPathNotValidException if the artifact path is not valid (requires enabled validation).
     */
    private Mono<Boolean> checkUpload(@NonNull AwsCredentialsProvider credentials,
                                      @NonNull String repository,
                                      @NonNull String artifactPath) {

        if (RepositoryHelper.isMetadataPath(artifactPath)) {
            return Mono.just(true);
        }

        if (appProperties.isMavenValidate() && !isArtifactPath(artifactPath)) {
            throw new ArtifactPathNotValidException();
        }

        return s3Repository.fileHead(credentials, repository, key(artifactPath))
                .flatMap(result -> {
                    // artifact exists already -> return error
                    if (result != null)
                        return Mono.error(new ArtifactAlreadyExistsException());
                    // Workaround: otherwise compiler complains about the return type
                    return Mono.just(true);
                })
                // artifact not exists -> return false
                .onErrorReturn(NoSuchKeyException.class, false);
    }

    private String computeContentType(MediaType contentType, String filePath) {
        return contentType != null ? contentType.toString() : getMediaType(filePath, APPLICATION_OCTET_STREAM_VALUE);
    }

    private String key(String coordinates) {
        return coordinates.startsWith("/") ? coordinates.substring(1) : coordinates;
    }

    private AwsCredentialsProvider credentials(Context context, String bucket) {
        if (!context.hasKey(AWS_CREDENTIALS_PROVIDER)) {
            throw new NotAuthorizedException(bucket);
        }
        return context.get(AWS_CREDENTIALS_PROVIDER);
    }

}
