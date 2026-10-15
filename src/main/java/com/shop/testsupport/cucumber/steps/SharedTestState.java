package com.shop.testsupport.cucumber.steps;

import com.shop.testsupport.cucumber.http.HttpTestContext;
import com.shop.testsupport.cucumber.mock.MockServiceRegistry;

public class SharedTestState {

    private final HttpTestContext http = new HttpTestContext();
    private final MockServiceRegistry mocks = new MockServiceRegistry();

    public HttpTestContext http() {
        return http;
    }

    public MockServiceRegistry mocks() {
        return mocks;
    }
}
