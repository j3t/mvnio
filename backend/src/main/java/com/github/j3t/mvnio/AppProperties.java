package com.github.j3t.mvnio;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;

import java.net.URI;

/**
 * Stores configuration parameters and their defaults.
 */
@Data
public class AppProperties {

    @Value("${s3.region:us-east-1}")
    private String s3Region;

    @Value("${s3.override-endpoint:false}")
    private boolean s3OverrideEndpoint;

    @Value("${s3.endpoint:http://localhost:9000}")
    private URI s3Endpoint;

    @Value("${maven.validate:true}")
    private boolean mavenValidate;
}
