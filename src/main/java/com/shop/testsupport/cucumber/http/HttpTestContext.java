package com.shop.testsupport.cucumber.http;

import io.restassured.response.Response;

import java.util.LinkedHashMap;
import java.util.Map;

public class HttpTestContext {

    private String baseUrl = System.getProperty("test.api.base-url", "http://localhost:8080");
    private final Map<String, String> requestHeaders = new LinkedHashMap<>();
    private String requestBody;
    private Response lastResponse;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = trimTrailingSlash(baseUrl);
    }

    public Map<String, String> getRequestHeaders() {
        return requestHeaders;
    }

    public void putRequestHeader(String name, String value) {
        requestHeaders.put(name, value);
    }

    public void clearRequestHeaders() {
        requestHeaders.clear();
    }

    public String getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    public void clearRequestBody() {
        this.requestBody = null;
    }

    public Response getLastResponse() {
        if (lastResponse == null) {
            throw new IllegalStateException("No HTTP response is available. Send a request first.");
        }
        return lastResponse;
    }

    public void setLastResponse(Response lastResponse) {
        this.lastResponse = lastResponse;
    }

    public void resetRequest() {
        clearRequestHeaders();
        clearRequestBody();
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Base URL must not be blank");
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
