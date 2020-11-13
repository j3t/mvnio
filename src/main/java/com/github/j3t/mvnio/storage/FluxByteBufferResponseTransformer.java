package com.github.j3t.mvnio.storage;

import reactor.core.publisher.Flux;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

public class FluxByteBufferResponseTransformer implements AsyncResponseTransformer<GetObjectResponse, Download> {
    private CompletableFuture<Download> future = new CompletableFuture<>();
    private Download.DownloadBuilder downloadBuilder = new Download.DownloadBuilder();

    @Override
    public CompletableFuture<Download> prepare() {
        return future;
    }

    @Override
    public void onResponse(GetObjectResponse sdkResponse) {
        downloadBuilder
                .contentLength(sdkResponse.contentLength())
                .contentType(sdkResponse.contentType());
    }

    @Override
    public void onStream(SdkPublisher<ByteBuffer> publisher) {
        future.complete(downloadBuilder
                .content(Flux.from(publisher))
                .build());
    }

    @Override
    public void exceptionOccurred(Throwable error) {
        future.completeExceptionally(error);
    }

}