package com.github.j3t.mvnio.storage;

import com.github.j3t.mvnio.storage.FluxByteBufferResponseTransformer.Result;
import lombok.NonNull;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;

import java.nio.ByteBuffer;

public class S3RepositoryS3AsyncClientImpl implements S3Repository {

    private final S3AsyncClient s3AsyncClient;

    public S3RepositoryS3AsyncClientImpl(S3AsyncClient s3AsyncClient) {
        this.s3AsyncClient = s3AsyncClient;
    }

    @Override
    public Mono<Result> fileDownload(@NonNull AwsCredentialsProvider credentialsProvider,
                                     @NonNull String bucket,
                                     @NonNull String key) {

        return Mono.fromFuture(s3AsyncClient.getObject(GetObjectRequest.builder()
                .bucket(bucket)
                .overrideConfiguration(AwsRequestOverrideConfiguration.builder()
                        .credentialsProvider(credentialsProvider)
                        .build())
                .key(key)
                .build(), new FluxByteBufferResponseTransformer()));
    }

    @Override
    public Mono<PutObjectResponse> fileUpload(@NonNull AwsCredentialsProvider credentialsProvider,
                                              @NonNull String bucket,
                                              @NonNull String key,
                                              @NonNull String contentType,
                                              @NonNull Long contentLength,
                                              @NonNull Publisher<ByteBuffer> file) {

        return Mono.fromFuture(s3AsyncClient.putObject(PutObjectRequest.builder()
                .bucket(bucket)
                .overrideConfiguration(AwsRequestOverrideConfiguration.builder()
                        .credentialsProvider(credentialsProvider)
                        .build())
                .key(key)
                .contentType(contentType)
                .contentLength(contentLength)
                .build(), AsyncRequestBody.fromPublisher(file)));
    }

    @Override
    public Mono<HeadObjectResponse> fileHead(@NonNull AwsCredentialsProvider credentialsProvider,
                                             @NonNull String bucket,
                                             @NonNull String key) {

        return Mono.fromFuture(s3AsyncClient.headObject(HeadObjectRequest.builder()
                .bucket(bucket)
                .overrideConfiguration(AwsRequestOverrideConfiguration.builder()
                        .credentialsProvider(credentialsProvider)
                        .build())
                .key(key)
                .build()));
    }
}
