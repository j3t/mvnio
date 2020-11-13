package com.github.j3t.mvnio.storage;

import lombok.NonNull;
import org.reactivestreams.Publisher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.nio.ByteBuffer;

public interface S3Repository {
    Mono<Download> download(@NonNull String bucket,
                          @NonNull String key);

    Mono<PutObjectResponse> upload(@NonNull String bucket,
                                   @NonNull String key,
                                   @NonNull String contentType,
                                   @NonNull Long contentLength,
                                   @NonNull Publisher<ByteBuffer> file);

    Mono<HeadObjectResponse> head(@NonNull String bucket,
                                  @NonNull String key);

    Flux<String> metadata(@NonNull String bucket, String startAfter, int limit);

    Flux<String> list(@NonNull String bucket, @NonNull String path);
}
