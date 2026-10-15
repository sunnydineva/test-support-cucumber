package com.shop.testsupport.cucumber.mock;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import java.util.LinkedHashMap;
import java.util.Map;

public class MockServiceRegistry {

    private final Map<String, WireMockServer> services = new LinkedHashMap<>();

    public WireMockServer start(String serviceName) {
        return services.computeIfAbsent(serviceName, ignored -> {
            WireMockServer server = new WireMockServer(WireMockConfiguration.options().dynamicPort());
            server.start();
            return server;
        });
    }

    public WireMockServer start(String serviceName, int port) {
        WireMockServer existing = services.get(serviceName);
        if (existing != null && existing.port() == port) {
            return existing;
        }
        if (existing != null) {
            existing.stop();
        }

        WireMockServer server = new WireMockServer(WireMockConfiguration.options().port(port));
        server.start();
        services.put(serviceName, server);
        return server;
    }

    public WireMockServer get(String serviceName) {
        WireMockServer server = services.get(serviceName);
        if (server == null) {
            throw new IllegalArgumentException("Mock service is not running: " + serviceName);
        }
        return server;
    }

    public String baseUrl(String serviceName) {
        return get(serviceName).baseUrl();
    }

    public WireMock wireMock(String serviceName) {
        WireMockServer server = get(serviceName);
        return new WireMock("localhost", server.port());
    }

    public void resetAll() {
        services.values().forEach(WireMockServer::resetAll);
    }

    public void stopAll() {
        services.values().forEach(WireMockServer::stop);
        services.clear();
    }
}
