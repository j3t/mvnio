package com.github.j3t.mvnio.storage;

import java.nio.ByteBuffer;

import lombok.Builder;
import lombok.Data;
import reactor.core.publisher.Flux;

@Data
@Builder
public class Download {
    private Flux<ByteBuffer> content;
    private String contentType;
    private long contentLength;
}
