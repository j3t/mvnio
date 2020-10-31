package com.github.j3t.mvnio.s3;

import reactor.core.publisher.Flux;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

public class FluxByteBufferResponseTransformer implements AsyncResponseTransformer<GetObjectResponse, FluxByteBufferResponseTransformer.Result> {
    private CompletableFuture<Result> future = new CompletableFuture<>();
    private Result result = new Result();

    @Override
    public CompletableFuture<Result> prepare() {
        return future;
    }

    @Override
    public void onResponse(GetObjectResponse sdkResponse) {
        this.result.sdkResponse = sdkResponse;
    }

    @Override
    public void onStream(SdkPublisher<ByteBuffer> publisher) {
        result.flux = Flux.from(publisher);
        future.complete(result);
    }

    @Override
    public void exceptionOccurred(Throwable error) {
        future.completeExceptionally(error);
    }

    public class Result {

        public Flux<ByteBuffer> flux;
        public GetObjectResponse sdkResponse;
    }
}