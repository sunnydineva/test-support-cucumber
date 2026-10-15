package com.shop.testsupport.cucumber.steps;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.shop.testsupport.cucumber.mock.MockServiceRegistry;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;

import java.util.Locale;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.requestMadeFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

public class MockServiceSteps {

    private final MockServiceRegistry mocks = TestWorld.state().mocks();

    @Given("mock service {string} is running")
    public void mockServiceIsRunning(String serviceName) {
        WireMockServer server = mocks.start(serviceName);
        System.setProperty(serviceName + ".mock.base-url", server.baseUrl());
    }

    @Given("mock service {string} is running on port {int}")
    public void mockServiceIsRunningOnPort(String serviceName, int port) {
        WireMockServer server = mocks.start(serviceName, port);
        System.setProperty(serviceName + ".mock.base-url", server.baseUrl());
    }

    @Given("mock service {string} base URL is saved as system property {string}")
    public void mockServiceBaseUrlIsSavedAsSystemProperty(String serviceName, String propertyName) {
        System.setProperty(propertyName, mocks.baseUrl(serviceName));
    }

    @Given("mock service {string} returns {int} for {word} {string}")
    public void mockServiceReturnsFor(String serviceName, int status, String method, String path) {
        stub(serviceName, method, path, aResponse().withStatus(status));
    }

    @Given("mock service {string} returns {int} for {word} {string} with body:")
    public void mockServiceReturnsForWithBody(String serviceName, int status, String method, String path, String body) {
        stub(serviceName, method, path, jsonResponse(status, body));
    }

    @Given("mock service {string} returns {int} for {word} {string} with response headers:")
    public void mockServiceReturnsForWithResponseHeaders(String serviceName, int status, String method, String path, DataTable table) {
        ResponseDefinitionBuilder response = aResponse().withStatus(status);
        for (Map<String, String> row : table.asMaps()) {
            response.withHeader(firstPresent(row, "name", "header", "key"), firstPresent(row, "value"));
        }
        stub(serviceName, method, path, response);
    }

    @Given("mock service {string} returns {int} for {word} {string} when request header {string} is {string} with body:")
    public void mockServiceReturnsForHeaderWithBody(
            String serviceName,
            int status,
            String method,
            String path,
            String headerName,
            String headerValue,
            String body) {
        MappingBuilder mapping = mapping(method, path).withHeader(headerName, equalTo(headerValue));
        mocks.get(serviceName).stubFor(mapping.willReturn(jsonResponse(status, body)));
    }

    @Given("mock service {string} returns {int} for {word} {string} when request body is:")
    public void mockServiceReturnsForRequestBody(String serviceName, int status, String method, String path, String expectedRequestBody) {
        MappingBuilder mapping = mapping(method, path).withRequestBody(equalToJson(expectedRequestBody, true, true));
        mocks.get(serviceName).stubFor(mapping.willReturn(aResponse().withStatus(status)));
    }

    @Then("mock service {string} should have received {word} {string}")
    public void mockServiceShouldHaveReceived(String serviceName, String method, String path) {
        mocks.get(serviceName).verify(requestPattern(method, path));
    }

    @Then("mock service {string} should have received {int} {word} {string}")
    public void mockServiceShouldHaveReceivedTimes(String serviceName, int count, String method, String path) {
        mocks.get(serviceName).verify(count, requestPattern(method, path));
    }

    @Then("mock service {string} should have received {word} {string} with header {string} {string}")
    public void mockServiceShouldHaveReceivedWithHeader(
            String serviceName,
            String method,
            String path,
            String headerName,
            String headerValue) {
        mocks.get(serviceName).verify(requestPattern(method, path).withHeader(headerName, equalTo(headerValue)));
    }

    @Then("mock service {string} should have received {word} {string} with body containing:")
    public void mockServiceShouldHaveReceivedWithBodyContaining(String serviceName, String method, String path, String bodyPart) {
        mocks.get(serviceName).verify(requestPattern(method, path).withRequestBody(containing(bodyPart)));
    }

    @Then("mock service {string} URL should be available")
    public void mockServiceUrlShouldBeAvailable(String serviceName) {
        assertThat(mocks.baseUrl(serviceName)).startsWith("http://localhost:");
    }

    private void stub(String serviceName, String method, String path, ResponseDefinitionBuilder response) {
        mocks.get(serviceName).stubFor(mapping(method, path).willReturn(response));
    }

    private ResponseDefinitionBuilder jsonResponse(int status, String body) {
        return aResponse()
                .withStatus(status)
                .withHeader("Content-Type", "application/json")
                .withBody(body);
    }

    private MappingBuilder mapping(String method, String path) {
        return switch (method.toUpperCase(Locale.ROOT)) {
            case "GET" -> get(urlEqualTo(path));
            case "POST" -> post(urlEqualTo(path));
            case "PUT" -> put(urlEqualTo(path));
            case "PATCH" -> patch(urlEqualTo(path));
            case "DELETE" -> delete(urlEqualTo(path));
            default -> throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        };
    }

    private RequestPatternBuilder requestPattern(String method, String path) {
        return switch (method.toUpperCase(Locale.ROOT)) {
            case "GET" -> getRequestedFor(urlEqualTo(path));
            case "POST" -> postRequestedFor(urlEqualTo(path));
            case "PUT" -> putRequestedFor(urlEqualTo(path));
            case "PATCH" -> patchRequestedFor(urlEqualTo(path));
            case "DELETE" -> deleteRequestedFor(urlEqualTo(path));
            default -> throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        };
    }

    private String firstPresent(Map<String, String> row, String... keys) {
        for (String key : keys) {
            String value = row.get(key);
            if (value != null) {
                return value;
            }
        }
        throw new IllegalArgumentException("Expected one of columns: " + String.join(", ", keys));
    }
}
