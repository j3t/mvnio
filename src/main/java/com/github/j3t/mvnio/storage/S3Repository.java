package com.github.j3t.mvnio.storage;

import com.github.j3t.mvnio.storage.FluxByteBufferResponseTransformer.Result;
import lombok.NonNull;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.nio.ByteBuffer;

public interface S3Repository {
    Mono<Result> download(@NonNull String bucket,
                          @NonNull String key);

    Mono<PutObjectResponse> upload(@NonNull String bucket,
                                   @NonNull String key,
                                   @NonNull String contentType,
                                   @NonNull Long contentLength,
                                   @NonNull Publisher<ByteBuffer> file);

    Mono<HeadObjectResponse> head(@NonNull String bucket,
                                  @NonNull String key);
}
