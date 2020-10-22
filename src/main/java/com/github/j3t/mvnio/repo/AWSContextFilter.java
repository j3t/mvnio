package com.github.j3t.mvnio.repo;

import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;

public class AWSContextFilter implements WebFilter {
    public static final String AWS_CREDENTIALS_PROVIDER = "AWS_CREDENTIALS_PROVIDER";

    @Override
    public Mono<Void> filter(ServerWebExchange serverWebExchange, WebFilterChain webFilterChain) {

        if (serverWebExchange.getRequest().getHeaders().containsKey("authorization")) {
            String base64Credentials = serverWebExchange.getRequest()
                    .getHeaders()
                    .getFirst("authorization")
                    .substring("Basic".length()).trim();
            String credentials = new String(Base64.getDecoder().decode(base64Credentials), UTF_8);
            final String[] values = credentials.split(":", 2);

            AwsCredentialsProvider provider = () -> AwsBasicCredentials.create(values[0], values[1]);

            return webFilterChain
                    .filter(serverWebExchange)
                    .subscriberContext(ctx -> ctx.put(AWS_CREDENTIALS_PROVIDER, provider));
        }

        return webFilterChain.filter(serverWebExchange);
    }
}
