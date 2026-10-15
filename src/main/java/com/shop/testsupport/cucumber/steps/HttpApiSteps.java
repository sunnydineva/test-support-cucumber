package com.shop.testsupport.cucumber.steps;

import com.shop.testsupport.cucumber.http.HttpTestContext;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.Locale;
import java.util.Map;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

public class HttpApiSteps {

    private final HttpTestContext http = TestWorld.state().http();

    @Given("API base URL is {string}")
    public void apiBaseUrlIs(String baseUrl) {
        http.setBaseUrl(baseUrl);
    }

    @Given("request header {string} is {string}")
    public void requestHeaderIs(String name, String value) {
        http.putRequestHeader(name, value);
    }

    @Given("request headers are:")
    public void requestHeadersAre(DataTable table) {
        for (Map<String, String> row : table.asMaps()) {
            http.putRequestHeader(firstPresent(row, "name", "header", "key"), firstPresent(row, "value"));
        }
    }

    @Given("request body is:")
    public void requestBodyIs(String body) {
        http.setRequestBody(body);
    }

    @When("I send {word} request to {string}")
    public void sendRequestTo(String method, String pathOrUrl) {
        RequestSpecification request = RestAssured.given()
                .relaxedHTTPSValidation()
                .headers(http.getRequestHeaders());

        if (http.getRequestBody() != null) {
            request.contentType(ContentType.JSON).body(http.getRequestBody());
        }

        Response response = request.request(method.toUpperCase(Locale.ROOT), resolveUrl(pathOrUrl));
        http.setLastResponse(response);
        http.clearRequestBody();
    }

    @Then("response status should be {int}")
    public void responseStatusShouldBe(int expectedStatus) {
        assertThat(http.getLastResponse().statusCode()).isEqualTo(expectedStatus);
    }

    @Then("response header {string} should be {string}")
    public void responseHeaderShouldBe(String name, String value) {
        assertThat(http.getLastResponse().header(name)).isEqualTo(value);
    }

    @Then("response body should be:")
    public void responseBodyShouldBe(String expectedBody) {
        assertThatJson(http.getLastResponse().asString()).isEqualTo(expectedBody);
    }

    @Then("response body should contain:")
    public void responseBodyShouldContain(String expectedJsonSubset) throws Exception {
        JSONAssert.assertEquals(expectedJsonSubset, http.getLastResponse().asString(), false);
    }

    @Then("response JSON path {string} should be {string}")
    public void responseJsonPathShouldBe(String jsonPath, String expectedValue) {
        assertThat(http.getLastResponse().jsonPath().getString(jsonPath)).isEqualTo(expectedValue);
    }

    @Then("response JSON path {string} should be {int}")
    public void responseJsonPathShouldBe(String jsonPath, int expectedValue) {
        assertThat(http.getLastResponse().jsonPath().getInt(jsonPath)).isEqualTo(expectedValue);
    }

    private String resolveUrl(String pathOrUrl) {
        if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) {
            return pathOrUrl;
        }
        return http.getBaseUrl() + (pathOrUrl.startsWith("/") ? pathOrUrl : "/" + pathOrUrl);
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
