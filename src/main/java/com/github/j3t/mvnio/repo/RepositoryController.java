package com.github.j3t.mvnio.repo;

import com.github.j3t.mvnio.AppProperties;
import com.github.j3t.mvnio.error.ArtifactAlreadyExistsException;
import com.github.j3t.mvnio.error.ArtifactPathNotValidException;
import com.github.j3t.mvnio.repo.validation.ArtifactPathValidator;
import com.github.j3t.mvnio.repo.validation.MetadataPathValidator;
import com.github.j3t.mvnio.storage.S3Repository;
import lombok.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.nio.ByteBuffer;

@RestController
@RequestMapping("/maven")
public class RepositoryController {

    private final S3Repository s3;
    private final AppProperties appProperties;

    public RepositoryController(S3Repository s3, AppProperties appProperties) {
        this.s3 = s3;
        this.appProperties = appProperties;
    }

    @PutMapping(value = "/{repository}/{*artifactPath}")
    public Mono<ResponseEntity<Void>> upload(
            @RequestHeader(value = "content-type", required = false) MediaType contentType,
            @RequestHeader(value = "content-length") long contentLength,
            @PathVariable String repository,
            @PathVariable String artifactPath,
            @RequestBody Flux<ByteBuffer> file) {

        // check: artifact path is valid?
        return validate(repository, artifactPath)
                // yes -> compute content type
                .then(computeContentType(contentType, artifactPath))
                // and upload file
                .flatMap(type -> s3.upload(repository, key(artifactPath), type, contentLength, file))
                // and then return 201
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).build());
    }

    @GetMapping(value = "/{repository}/{*artifactPath}")
    public Mono<ResponseEntity<Flux<ByteBuffer>>> download(@PathVariable String repository,
                                                           @PathVariable String artifactPath) {

        // download file
        return s3.download(repository, key(artifactPath))
                // and return 200
                .map(result -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_TYPE, result.sdkResponse.contentType())
                        .header(HttpHeaders.CONTENT_LENGTH, Long.toString(result.sdkResponse.contentLength()))
                        .body(result.flux));
    }

    /**
     * Checks that a given artifact not already exists and can be uploaded. Maven metadata files are ignored.
     *
     * @throws ArtifactAlreadyExistsException if the artifact already exists.
     * @throws ArtifactPathNotValidException  if the artifact path is not valid (requires enabled validation).
     */
    private Mono<Void> validate(@NonNull String repository,
                                @NonNull String artifactPath) {


        // check: is a valid metadata path?
        return new MetadataPathValidator(artifactPath).validate()
                // no -> check: is artifact validation enabled?
                .filter(vem -> appProperties.isMavenValidate())
                // yes -> check: is a valid artifact path?
                .flatMap(validate -> new ArtifactPathValidator(artifactPath).validate()
                        // no -> throw an error
                        .flatMap(vea -> Mono.error(new ArtifactPathNotValidException()))
                        // yes -> check: file exists?
                        .switchIfEmpty(s3.head(repository, key(artifactPath))
                                // yes -> throw an error
                                .flatMap(headResponse -> Mono.error(new ArtifactAlreadyExistsException()))
                                // no -> handle the exception and return something
                                .onErrorReturn(NoSuchKeyException.class, false)))
                // no -> return empty
                .then();
    }

    private Mono<String> computeContentType(MediaType contentType, String artifactPath) {
        return Mono.justOrEmpty(contentType)
                .switchIfEmpty(ContentTypeResolver.findByPath(artifactPath))
                .switchIfEmpty(Mono.just(MediaType.APPLICATION_OCTET_STREAM))
                .map(MediaType::toString);
    }

    private String key(String coordinates) {
        return coordinates.startsWith("/") ? coordinates.substring(1) : coordinates;
    }

}
