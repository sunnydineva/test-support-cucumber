# test-support-cucumber

Reusable Cucumber step library for API and service-to-service tests.

It provides two groups of steps:

- real HTTP calls to an API under test
- WireMock-based mocks for downstream services
- Mockito/Spring mocks for service beans inside the service under test

## Build Locally

```bash
cd test-support-cucumber
mvn clean install
```

## Add To A Service

Add this to the service `pom.xml`:

```xml
<dependency>
    <groupId>org.sunnydineva</groupId>
    <artifactId>test-support-cucumber</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <scope>test</scope>
</dependency>
```

Create a Cucumber suite, for example `src/test/java/com/shop/order/CucumberIT.java`:

```java
package com.shop.order;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.shop.testsupport.cucumber.steps")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty, summary")
public class CucumberIT {
}
```

For Spring bean/service class mocks, also add a Cucumber Spring bootstrap class in
your service tests, for example `src/test/java/com/shop/order/CucumberSpringConfig.java`:

```java
package com.shop.order;

import com.shop.service.ProductService;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CucumberSpringConfig {

    @MockBean(name = "productService")
    ProductService productService;
}
```

Then include both glue packages:

```java
@ConfigurationParameter(
    key = GLUE_PROPERTY_NAME,
    value = "com.shop.testsupport.cucumber.steps,com.shop.order"
)
```

## Real API Call Example

```gherkin
Feature: API calls

  Scenario: Get products
    Given API base URL is "http://localhost:8081"
    And request header "Authorization" is "Bearer test-token"
    When I send GET request to "/api/products"
    Then response status should be 200
    And response body should contain:
      """
      {
        "content": []
      }
      """
```

## Mock Downstream Service Example

Configure the service under test to call the mock URL. For dynamic mock ports,
read the printed/system property value and wire it through your test bootstrap.
For the simplest local test, use a fixed port:

```gherkin
Feature: Order service with mocked product service

  Scenario: Create order with mocked product-service
    Given mock service "product-service" is running on port 18084
    And mock service "product-service" returns 200 for GET "/api/products/1" with body:
      """
      {
        "id": 1,
        "name": "Keyboard",
        "price": 99.99,
        "currency": "USD",
        "stockQuantity": 10,
        "categoryId": 1,
        "isActive": true
      }
      """
    And mock service "product-service" returns 200 for PUT "/api/products/1"
    And API base URL is "http://localhost:8085"
    And request body is:
      """
      {
        "items": [
          { "productId": 1, "quantity": 2 }
        ]
      }
      """
    When I send POST request to "/api/user/orders?userId=1&userEmail=test@example.com"
    Then response status should be 200
    And response body should contain:
      """
      {
        "userId": 1,
        "userEmail": "test@example.com",
        "status": "NEW"
      }
      """
    And mock service "product-service" should have received GET "/api/products/1"
    And mock service "product-service" should have received PUT "/api/products/1"
```

## Mock Spring Service Class Example

This is for mocking a service bean/class inside the application context, not a
service from another container. The bean must be declared as `@MockBean` in the
test bootstrap.

```gherkin
Feature: Controller with mocked service class

  Scenario: Product details are returned from mocked service bean
    Given Spring mock bean "productService" method "getProductById" returns for JSON arguments:
      """
      {
        "arguments": [1],
        "returns": {
          "id": 1,
          "name": "Keyboard",
          "price": 99.99,
          "currency": "USD",
          "stockQuantity": 10
        }
      }
      """
    And API base URL is "http://localhost:8080"
    When I send GET request to "/api/products/1"
    Then response status should be 200
    And response body should contain:
      """
      {
        "id": 1,
        "name": "Keyboard"
      }
      """
    And Spring mock bean "productService" method "getProductById" should have been called with JSON arguments:
      """
      [1]
      """
```

You can also stub by method name for any arguments:

```gherkin
Given Spring mock bean "productService" method "getFeaturedProducts" returns:
  """
  [
    { "id": 1, "name": "Keyboard" }
  ]
  """
```

Or make the mocked service throw:

```gherkin
Given Spring mock bean "productService" method "getProductById" throws "java.lang.IllegalStateException" with message "Product unavailable"
```

## Supported Steps

HTTP calls:

```gherkin
Given API base URL is "http://localhost:8080"
Given request header "Authorization" is "Bearer token"
Given request headers are:
  | name          | value            |
  | Authorization | Bearer token     |
  | X-Test        | true             |
Given request body is:
  """
  { "name": "test" }
  """
When I send GET request to "/api/path"
When I send POST request to "/api/path"
When I send PUT request to "/api/path"
When I send PATCH request to "/api/path"
When I send DELETE request to "/api/path"
Then response status should be 200
Then response header "Content-Type" should be "application/json"
Then response body should be:
  """
  { "id": 1 }
  """
Then response body should contain:
  """
  { "id": 1 }
  """
Then response JSON path "id" should be 1
Then response JSON path "status" should be "NEW"
```

Mocks:

```gherkin
Given mock service "product-service" is running
Given mock service "product-service" is running on port 18084
Given mock service "product-service" base URL is saved as system property "product-service.url"
Given mock service "product-service" returns 200 for GET "/api/products/1"
Given mock service "product-service" returns 200 for GET "/api/products/1" with body:
  """
  { "id": 1 }
  """
Given mock service "product-service" returns 200 for GET "/api/products/1" with response headers:
  | name         | value            |
  | Content-Type | application/json |
Given mock service "product-service" returns 200 for GET "/api/products/1" when request header "Authorization" is "Bearer token" with body:
  """
  { "id": 1 }
  """
Given mock service "product-service" returns 200 for POST "/api/products" when request body is:
  """
  { "name": "Keyboard" }
  """
Then mock service "product-service" should have received GET "/api/products/1"
Then mock service "product-service" should have received 1 GET "/api/products/1"
Then mock service "product-service" should have received GET "/api/products/1" with header "Authorization" "Bearer token"
Then mock service "product-service" should have received POST "/api/products" with body containing:
  """
  Keyboard
  """
```

Spring service class mocks:

```gherkin
Given Spring mock bean "productService" method "getProductById" returns:
  """
  { "id": 1, "name": "Keyboard" }
  """
Given Spring mock bean "productService" method "getProductById" returns for JSON arguments:
  """
  {
    "arguments": [1],
    "returns": { "id": 1, "name": "Keyboard" }
  }
  """
Given Spring mock bean "productService" method "getProductById" throws "java.lang.IllegalStateException" with message "Product unavailable"
Then Spring mock bean "productService" method "getProductById" should have been called
Then Spring mock bean "productService" method "getProductById" should have been called 1 times
Then Spring mock bean "productService" method "getProductById" should have been called with JSON arguments:
  """
  [1]
  """
```
