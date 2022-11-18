package com.github.j3t.mvnio.error;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.PathContainer;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ServerWebExchange;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Slf4j
@ControllerAdvice
public class ErrorHandler {

    private static void logError(int status, Exception e) {
        if (status < 500) {
            log.debug("status: {}, message: {}", status, e.getMessage());
        } else {
            log.error("status: {}, message: {}", status, e.getMessage(), e);
        }
    }

    private static String getBucket(ServerWebExchange swe) {
        List<PathContainer.Element> elements = swe.getRequest().getPath().pathWithinApplication().elements();
        return elements.size() >= 4 ? elements.get(3).value() : "";
    }

    @ExceptionHandler
    @ResponseBody
    public Mono<ResponseEntity<String>> handleException(Exception e, ServerWebExchange swe) {
        var status = 500;
        var httpHeaders = new HttpHeaders();
        String message = e.getMessage();

        if (e instanceof ClientError) {
            status = ((ClientError) e).getReturnCode();
            if (status == 401) {
                String bucket = getBucket(swe);
                httpHeaders.set("WWW-Authenticate", "Basic realm=\"s3\", bucket=\"" + bucket + "\"");
            }
        } else if (e instanceof NoSuchBucketException) {
            status = 404;
            message = "Repository not exists";
        } else if (e instanceof NoSuchKeyException) {
            status = 404;
            message = "Artifact not exists";
        } else if (e instanceof S3Exception) {
            status = ((S3Exception) e).statusCode();
        }

        logError(status, e);

        return Mono.just(ResponseEntity.status(status).headers(httpHeaders).body(message));
    }
}
