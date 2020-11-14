package com.github.j3t.mvnio.maven;

import java.nio.ByteBuffer;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.github.j3t.mvnio.AppProperties;
import com.github.j3t.mvnio.error.ClientError;
import com.github.j3t.mvnio.maven.validation.ArtifactPathValidator;
import com.github.j3t.mvnio.maven.validation.MetadataPathValidator;
import com.github.j3t.mvnio.storage.S3Repository;

import lombok.NonNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

@RestController
@RequestMapping("/")
public class RepositoryController {

    private final S3Repository s3;
    private final AppProperties appProperties;

    public RepositoryController(S3Repository s3, AppProperties appProperties) {
        this.s3 = s3;
        this.appProperties = appProperties;
    }

    @PutMapping(value = "/maven/{repository}/{*artifactPath}")
    public Mono<ResponseEntity<Void>> upload(@RequestHeader(value = "content-type", required = false) MediaType contentType,
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
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED).build());
    }

    @GetMapping(value = "/maven/{repository}/{*artifactPath}")
    public Mono<ResponseEntity<Flux<ByteBuffer>>> download(@PathVariable String repository,
                                                           @PathVariable String artifactPath) {

        // download file
        return s3.download(repository, key(artifactPath))
                // and return 200
                .map(result -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_TYPE, result.getContentType())
                        .header(HttpHeaders.CONTENT_LENGTH, Long.toString(result.getContentLength()))
                        .body(result.getContent()));
    }

    @GetMapping(value = "/metadata/{repository}")
    public Mono<List<String>> metadata(@PathVariable String repository,
                                       @RequestParam(required = false) String startAfter,
                                       @RequestParam(required = false, defaultValue = "10") int limit) {
        return s3.metadata(repository, startAfter, limit).collectList();
    }

    @GetMapping(value = "/list/{repository}/{*path}")
    public Mono<List<String>> list(@PathVariable String repository, @PathVariable String path) {
        return s3.list(repository, path).collectList();
    }

    /**
     * Checks that a given artifact not already exists and can be uploaded. Maven metadata files are ignored.
     *
     * @throws ClientError if the artifact already exists if path is not valid
     */
    private Mono<Void> validate(@NonNull String repository,
                                @NonNull String artifactPath) {


        // check: is a valid metadata path?
        return new MetadataPathValidator(artifactPath).validate()
                // no -> check: is artifact validation enabled?
                .filter(errorMPV -> appProperties.isMavenValidate())
                // yes -> check: is artifact path valid?
                .flatMap(errorMPV -> new ArtifactPathValidator(artifactPath).validate()
                        // no -> throw an error
                        .flatMap(errorAPV -> Mono.error(new ClientError(400, "Path validation failed")))
                        // yes -> check: file exists?
                        .switchIfEmpty(s3.head(repository, key(artifactPath))
                                // yes -> throw an error
                                .flatMap(headResponse -> Mono.error(new ClientError(403, "Artifact already exists")))
                                // no -> handle the exception and return something
                                .onErrorReturn(NoSuchKeyException.class, false)))
                // no -> return empty (upload artifact approved)
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
