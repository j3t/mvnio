package com.github.j3t.mvnio.storage;

import com.github.j3t.mvnio.error.NotAuthorizedException;

import lombok.NonNull;
import org.reactivestreams.Publisher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Publisher;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

import static com.github.j3t.mvnio.storage.S3CredentialsWebFilter.S3_CREDENTIALS_PROVIDER;

public class S3RepositoryS3AsyncClientImpl implements S3Repository {

    private final S3AsyncClient s3AsyncClient;

    public S3RepositoryS3AsyncClientImpl(S3AsyncClient s3AsyncClient) {
        this.s3AsyncClient = s3AsyncClient;
    }

    @Override
    public Mono<Download> download(@NonNull String bucket, @NonNull String key) {

        return Mono.deferContextual(ctx -> Mono.fromFuture(s3AsyncClient.getObject(GetObjectRequest.builder()
                .bucket(bucket)
                .overrideConfiguration(overrideConfiguration(ctx))
                .key(key)
                .build(), new FluxByteBufferResponseTransformer())));
    }

    @Override
    public Mono<PutObjectResponse> upload(@NonNull String bucket,
                                          @NonNull String key,
                                          @NonNull String contentType,
                                          @NonNull Long contentLength,
                                          @NonNull Publisher<ByteBuffer> file) {

        return Mono.deferContextual(ctx -> Mono.fromFuture(s3AsyncClient.putObject(PutObjectRequest.builder()
                .bucket(bucket)
                .overrideConfiguration(overrideConfiguration(ctx))
                .key(key)
                .contentType(contentType)
                .contentLength(contentLength)
                .build(), AsyncRequestBody.fromPublisher(file))));
    }

    @Override
    public Mono<HeadObjectResponse> head(@NonNull String bucket, @NonNull String key) {
        return Mono.deferContextual(ctx -> Mono.fromFuture(s3AsyncClient.headObject(HeadObjectRequest.builder()
                .bucket(bucket)
                .overrideConfiguration(overrideConfiguration(ctx))
                .key(key)
                .build())));
    }

    @Override
    public Flux<String> metadata(@NonNull String bucket, String startAfter, int limit) {
        return Flux.deferContextual(ctx -> s3AsyncClient.listObjectsV2Paginator(ListObjectsV2Request.builder()
                        .bucket(bucket)
                        .maxKeys(50)
                        .startAfter(startAfter)
                        .overrideConfiguration(overrideConfiguration(ctx))
                        .build())
                .contents()
                .filter(this::isMetadata)
                .limit(limit)
                .map(s3Object -> "/"+s3Object.key()));
    }

    private boolean isMetadata(S3Object s3Object) {
        return s3Object.key().endsWith("/maven-metadata.xml") && !s3Object.key().endsWith("-SNAPSHOT/maven-metadata.xml");
    }

    @Override
    public Flux<String> list(@NonNull String bucket, @NonNull String path) {
        return Flux.deferContextual(ctx -> {
            ListObjectsV2Publisher p = s3AsyncClient.listObjectsV2Paginator(ListObjectsV2Request.builder()
                    .bucket(bucket)
                    .prefix(toPrefix(path))
                    .delimiter("/")
                    .overrideConfiguration(overrideConfiguration(ctx))
                    .build());

            return Flux.concat(p.commonPrefixes().map(this::toDirectory), p.contents().map(this::toFile));
        });
    }

    private String toFile(S3Object s3Object) {
        String[] parts = s3Object.key().split("/");
        return parts[parts.length - 1];
    }

    private String toDirectory(CommonPrefix commonPrefix) {
        String[] parts = commonPrefix.prefix().split("/");
        return parts[parts.length - 1] + "/";
    }

    private static String toPrefix(String path) {
        String prefix = path;

        if (prefix.startsWith("/")) {
            prefix = prefix.substring(1);
        }

        if (!prefix.endsWith("/")) {
            prefix = prefix + "/";
        }

        return prefix.equals("/") ? "" : prefix;
    }

    private static Consumer<AwsRequestOverrideConfiguration.Builder> overrideConfiguration(ContextView ctx) {
        return builder -> {
            if (ctx.hasKey(S3_CREDENTIALS_PROVIDER)) {
                builder.credentialsProvider(ctx.get(S3_CREDENTIALS_PROVIDER));
            } else {
                throw new NotAuthorizedException();
            }
        };
    }

}
