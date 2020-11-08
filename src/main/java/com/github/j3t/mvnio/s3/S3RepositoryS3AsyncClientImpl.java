package com.github.j3t.mvnio.s3;

import com.github.j3t.mvnio.error.NotAuthorizedException;
import com.github.j3t.mvnio.s3.FluxByteBufferResponseTransformer.Result;
import lombok.NonNull;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

import static com.github.j3t.mvnio.s3.S3CredentialsWebFilter.S3_CREDENTIALS_PROVIDER;

public class S3RepositoryS3AsyncClientImpl implements S3Repository {

    private final S3AsyncClient s3AsyncClient;

    public S3RepositoryS3AsyncClientImpl(S3AsyncClient s3AsyncClient) {
        this.s3AsyncClient = s3AsyncClient;
    }

    @Override
    public Mono<Result> download(@NonNull String bucket, @NonNull String key) {

        return Mono.subscriberContext()
                .flatMap(ctx -> Mono.fromFuture(s3AsyncClient.getObject(GetObjectRequest.builder()
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

        return Mono.subscriberContext()
                .flatMap(ctx -> Mono.fromFuture(s3AsyncClient.putObject(PutObjectRequest.builder()
                        .bucket(bucket)
                        .overrideConfiguration(overrideConfiguration(ctx))
                        .key(key)
                        .contentType(contentType)
                        .contentLength(contentLength)
                        .build(), AsyncRequestBody.fromPublisher(file))));
    }

    @Override
    public Mono<HeadObjectResponse> head(@NonNull String bucket, @NonNull String key) {

        return Mono.subscriberContext()
                .flatMap(ctx -> Mono.fromFuture(s3AsyncClient.headObject(HeadObjectRequest.builder()
                        .bucket(bucket)
                        .overrideConfiguration(overrideConfiguration(ctx))
                        .key(key)
                        .build())));
    }

    private static Consumer<AwsRequestOverrideConfiguration.Builder> overrideConfiguration(Context ctx) {
        return builder -> {
            if (ctx.hasKey(S3_CREDENTIALS_PROVIDER)) {
                builder.credentialsProvider(ctx.get(S3_CREDENTIALS_PROVIDER));
            } else {
                throw new NotAuthorizedException();
            }
        };
    }

}
