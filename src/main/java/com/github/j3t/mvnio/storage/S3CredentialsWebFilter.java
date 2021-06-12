package com.github.j3t.mvnio.storage;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.Base64;

import org.springframework.lang.NonNull;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import lombok.val;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

/**
 * Injects the S3 credentials provider with the provided client credentials into the subscriber context so that it can be used by the
 * {@link S3Repository} to perform bucket operations.
 */
public class S3CredentialsWebFilter implements WebFilter {

    public static final String S3_CREDENTIALS_PROVIDER = "S3_CREDENTIALS_PROVIDER";

    @NonNull
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, @NonNull WebFilterChain chain) {

        var authorization = exchange.getRequest().getHeaders().getOrEmpty("authorization");

        if (authorization.isEmpty() || !authorization.get(0).toLowerCase().startsWith("basic")) {
            return chain.filter(exchange);
        }

        var base64Credentials = authorization.get(0).substring("Basic".length()).trim();
        var credentials = new String(Base64.getDecoder().decode(base64Credentials), UTF_8);
        val values = credentials.split(":", 2);
        AwsCredentialsProvider credentialsProvider = () -> AwsBasicCredentials.create(values[0], values[1]);

        return chain.filter(exchange)
                .contextWrite(ctx -> ctx.put(S3_CREDENTIALS_PROVIDER, credentialsProvider));

    }

}
