package com.harish.api.framework.base;

import io.restassured.specification.RequestSpecification;
import io.restassured.builder.RequestSpecBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.harish.api.framework.config.FrameworkConfig;
import com.harish.api.framework.manager.EnvironmentManager;

/**
 * BaseClient serves as the parent class for all REST API service clients.
 * 
 * Responsibilities:
 * - Initialize RequestSpecification with base configuration (URI, headers, timeouts).
 * - Provide a factory method to create clean RequestSpecification for each request.
 * - Encapsulate common request building logic.
 * 
 * Usage:
 * All microservice-specific clients (e.g., AccountServiceClient, PaymentServiceClient)
 * should extend BaseClient and use givenBaseRequest() to build individual API calls.
 * 
 * Example:
 *   public class AccountServiceClient extends BaseClient {
 *       public Account getAccountById(String accountId) {
 *           return givenBaseRequest()
 *               .pathParam("id", accountId)
 *               .when()
 *               .get("/api/accounts/{id}")
 *               .then()
 *               .extract()
 *               .as(Account.class);
 *       }
 *   }
 */
public class BaseClient {
    
    protected static final Logger logger = LogManager.getLogger(BaseClient.class);
    protected FrameworkConfig config;
    protected RequestSpecification baseRequestSpec;
    
    /**
     * Constructor initializes the base RequestSpecification from FrameworkConfig.
     * Called by subclasses via super().
     */
    public BaseClient() {
        this.config = EnvironmentManager.getConfig();
        this.baseRequestSpec = buildBaseRequestSpec();
        logger.info("BaseClient initialized with baseUrl: {}", config.getBaseUrl());
    }
    
    /**
     * Builds the base RequestSpecification with common headers, timeouts, and URI.
     * This is called once during initialization.
     * 
     * Configuration applied:
     * - Base URI from FrameworkConfig
     * - Standard JSON content-type headers
     * - Request/response timeouts
     * - Relaxed HTTPS validation (for test environments)
     * - Request/response logging at DEBUG level
     * 
     * @return RequestSpecification configured with framework defaults
     */
    private RequestSpecification buildBaseRequestSpec() {
        return new RequestSpecBuilder()
                .setBaseUri(config.getBaseUrl())
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .setContentType("application/json")
                .setAccept("application/json")
                .setConnectTimeout(config.getConnectTimeout())
                .setReadTimeout(config.getReadTimeout())
                .setRelaxedHTTPSValidation()
                .log(io.restassured.http.ContentType.JSON)
                .build();
    }
    
    /**
     * Factory method to create a clean copy of the base RequestSpecification.
     * 
     * IMPORTANT: Each request gets a fresh copy to avoid state contamination.
     * This ensures:
     * - Path parameters don't leak between requests
     * - Query parameters are isolated per request
     * - Headers added to one request don't affect others
     * 
     * Usage in child classes:
     *   Response response = givenBaseRequest()
     *       .header("Authorization", "Bearer token")
     *       .pathParam("id", "123")
     *       .when()
     *       .get("/api/resource/{id}");
     * 
     * @return A fresh RequestSpecification based on base configuration
     */
    protected RequestSpecification givenBaseRequest() {
        // Clone the base spec by building a new one with same configuration
        return new RequestSpecBuilder()
                .addRequestSpecification(baseRequestSpec)
                .build();
    }
    
    /**
     * Utility method to add Authorization header with Bearer token.
     * Commonly used for JWT or OAuth2 tokens in banking APIs.
     * 
     * @param token the authentication token (without "Bearer " prefix)
     * @return RequestSpecification with auth header added
     * 
     * TODO: In production, retrieve tokens from secure credential manager (Vault, Secrets Manager)
     */
    protected RequestSpecification givenAuthenticatedRequest(String token) {
        logger.debug("Building authenticated request with token");
        return givenBaseRequest()
                .header("Authorization", "Bearer " + token);
    }
    
    /**
     * Utility to add custom headers to base request (e.g., idempotency keys, correlation IDs).
     * 
     * @param headerName the header name
     * @param headerValue the header value
     * @return RequestSpecification with custom header added
     */
    protected RequestSpecification givenRequestWithHeader(String headerName, String headerValue) {
        logger.debug("Adding custom header: {}", headerName);
        return givenBaseRequest()
                .header(headerName, headerValue);
    }
    
    /**
     * Getter for FrameworkConfig - accessible to subclasses for custom configuration access.
     * 
     * @return the current FrameworkConfig instance
     */
    public FrameworkConfig getConfig() {
        return config;
    }
}
