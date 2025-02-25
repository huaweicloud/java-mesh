/*
 * Copyright (C) 2025-2025 Sermant Authors. All rights reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package io.sermant.xds.service.flowcontrol;

import com.alibaba.fastjson.JSONObject;
import io.sermant.xds.service.entity.HttpClientType;
import io.sermant.xds.service.entity.Result;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * xDS flow control test
 *
 * @author zhp
 * @since 2025-01-13
 **/
public class XdsFlowControlTest {
    private static final int CONNECT_TIMEOUT = 30000;

    private static final int SOCKET_TIMEOUT = 30000;

    private static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(25);

    private static final String OKHTTP_URL_PREFIX =
            "http://127.0.0.1:8080/flowControl/testOkHttp2?host=spring-server&version=";

    private static final String OKHTTP3_URL_PREFIX =
            "http://127.0.0.1:8082/flowControl/testOkHttp3?host=spring-server&version=";

    private static final String HTTP_CLIENT_URL_PREFIX =
            "http://127.0.0.1:8080/flowControl/testHttpClient?host=spring-server&version=";

    private static final String HTTP_URL_CONNECTION_URL_PREFIX =
            "http://127.0.0.1:8080/flowControl/testHttpUrlConnection?host=spring-server&version=";

    /**
     * test fault
     */
    @Test
    @EnabledIfSystemProperty(named = "xds.service.integration.test.type", matches = "FLOW_CONTROL_FAULT")
    public void testFault() throws InterruptedException {
        // Test the case where the request delay probability is 0
        testFaultProbability(buildFaultUrl(HttpClientType.HTTP_CLIENT, "v1"), 0, 0, 2000);
        testFaultProbability(buildFaultUrl(HttpClientType.OK_HTTP2, "v1"), 0, 0, 2000);
        testFaultProbability(buildFaultUrl(HttpClientType.HTTP_URL_CONNECTION, "v1"), 0, 0, 2000);
        testFaultProbability(buildFaultUrl(HttpClientType.OK_HTTP3, "v1"), 0, 0, 2000);

        // Test the case where the request delay probability is 50%
        testFaultProbability(buildFaultUrl(HttpClientType.HTTP_CLIENT, "v2"), 10, 40, 15000);
        testFaultProbability(buildFaultUrl(HttpClientType.OK_HTTP2, "v2"), 10, 40, 15000);
        testFaultProbability(buildFaultUrl(HttpClientType.HTTP_URL_CONNECTION, "v2"), 10, 40, 15000);
        testFaultProbability(buildFaultUrl(HttpClientType.OK_HTTP3, "v2"), 10, 40, 15000);

        // Test the case where the request delay probability is 100%
        testFaultProbability(buildFaultUrl(HttpClientType.HTTP_CLIENT, "v3"), 50, 50, 15000);
        testFaultProbability(buildFaultUrl(HttpClientType.OK_HTTP2, "v3"), 50, 50, 15000);
        testFaultProbability(buildFaultUrl(HttpClientType.HTTP_URL_CONNECTION, "v3"), 50, 50, 15000);
        testFaultProbability(buildFaultUrl(HttpClientType.OK_HTTP3, "v3"), 50, 50, 15000);

        // Test the case where the request abort probability is 0
        testFaultProbability(buildFaultUrl(HttpClientType.HTTP_CLIENT, "v4"), 0, 0, 2000);
        testFaultProbability(buildFaultUrl(HttpClientType.OK_HTTP2, "v4"), 0, 0, 2000);
        testFaultProbability(buildFaultUrl(HttpClientType.HTTP_URL_CONNECTION, "v4"), 0, 0, 2000);
        testFaultProbability(buildFaultUrl(HttpClientType.OK_HTTP3, "v4"), 0, 0, 2000);

        // Test the case where the request abort probability is 50%
        testFaultProbability(buildFaultUrl(HttpClientType.HTTP_CLIENT, "v5"), 10, 40, 2000);
        testFaultProbability(buildFaultUrl(HttpClientType.OK_HTTP2, "v5"), 10, 40, 2000);
        testFaultProbability(buildFaultUrl(HttpClientType.HTTP_URL_CONNECTION, "v5"), 10, 40, 2000);
        testFaultProbability(buildFaultUrl(HttpClientType.OK_HTTP3, "v5"), 10, 40, 2000);

        // Test the case where the request abort probability is 100%
        testFaultProbability(buildFaultUrl(HttpClientType.HTTP_CLIENT, "v6"), 50, 50, 2000);
        testFaultProbability(buildFaultUrl(HttpClientType.OK_HTTP2, "v6"), 50, 50, 2000);
        testFaultProbability(buildFaultUrl(HttpClientType.HTTP_URL_CONNECTION, "v6"), 50, 50, 2000);
        testFaultProbability(buildFaultUrl(HttpClientType.OK_HTTP3, "v6"), 50, 50, 2000);
    }

    /**
     * test retry
     */
    @Test
    @EnabledIfSystemProperty(named = "xds.service.integration.test.type", matches = "FLOW_CONTROL_RETRY")
    public void testRetry() {
        // Test does not meet the matching rules, retry will not be triggered
        resetRequestCount();
        Result result = doGet(buildGateWayErrorUrl(HttpClientType.HTTP_CLIENT, "v2"));
        Assertions.assertEquals(HttpStatus.SC_BAD_GATEWAY, result.getCode());
        resetRequestCount();
        result = doGet(buildGateWayErrorUrl(HttpClientType.OK_HTTP2, "v2"));
        Assertions.assertEquals(HttpStatus.SC_BAD_GATEWAY, result.getCode());
        resetRequestCount();
        result = doGet(buildGateWayErrorUrl(HttpClientType.OK_HTTP3, "v2"));
        Assertions.assertEquals(HttpStatus.SC_BAD_GATEWAY, result.getCode());
        resetRequestCount();
        result = doGet(buildGateWayErrorUrl(HttpClientType.HTTP_URL_CONNECTION, "v2"));
        Assertions.assertEquals(HttpStatus.SC_BAD_GATEWAY, result.getCode());
        resetRequestCount();

        doGet(buildGateWayErrorUrl(HttpClientType.HTTP_CLIENT, "v1"));
        doGet(buildGateWayErrorUrl(HttpClientType.HTTP_CLIENT, "v1"));

        // Test meet the matching rules, retry will not be triggered
        result = doGet(buildGateWayErrorUrl(HttpClientType.HTTP_CLIENT, "v1"));
        Assertions.assertEquals("3", result.getData());
        result = doGet(buildGateWayErrorUrl(HttpClientType.OK_HTTP2, "v1"));
        Assertions.assertEquals("3", result.getData());
        result = doGet(buildGateWayErrorUrl(HttpClientType.OK_HTTP2, "v1"));
        Assertions.assertEquals("4", result.getData());
        result = doGet(buildGateWayErrorUrl(HttpClientType.HTTP_URL_CONNECTION, "v1"));
        Assertions.assertEquals("4", result.getData());
        result = doGet(buildGateWayErrorUrl(HttpClientType.OK_HTTP3, "v1"));
        Assertions.assertEquals("5", result.getData());
        resetRequestCount();

        // Test the retry be triggered
        testAllRetryCondition("http://127.0.0.1:8080/flowControl/testHttpClient");
        testAllRetryCondition("http://127.0.0.1:8080/flowControl/testOkHttp2");
        testAllRetryCondition("http://127.0.0.1:8080/flowControl/testHttpUrlConnection");
        testAllRetryCondition("http://127.0.0.1:8082/flowControl/testOkHttp3");
    }

    private static void testAllRetryCondition(String urlPrefix) {
        // Test the case of retries when a gateway error occurs
        testRequestFailureCount(urlPrefix + "?host=spring-server&version=v1&path=testGateWayError");
        // Test the case of retries when the response contains the specified response header
        testRequestFailureCount(urlPrefix + "?host=spring-server&version=v1&path=testRetryOnHeader");
        // Test the case of retries when the response contains the specified response code
        testRequestFailureCount(urlPrefix + "?host=spring-server&version=v1&path=testRetryOnStatusCode");
        // Test the case of retries when a request timeout occurs
        testRequestFailureCount(urlPrefix + "?host=spring-server&version=v1&path=testTimeOut");
        // Test the case of retries when the response code is 4xx
        testRequestFailureCount(urlPrefix + "?host=spring-server&version=v1&path=test4xxError");
        // Test the case of retries when the response code is 5xx
        testRequestFailureCount(urlPrefix + "?host=spring-server&version=v1&path=test5xxError");
    }

    /**
     * test Circuit Breaker
     */
    @Test
    @EnabledIfSystemProperty(named = "xds.service.integration.test.type", matches = "FLOW_CONTROL_CIRCUIT_BREAKER")
    public void testCircuitBreaker() throws InterruptedException {
        // Test Circuit Breaker Function Based on Active Request Count
        EXECUTOR_SERVICE.execute(() -> doGet(buildRequestCircuitBreakerUrl(HttpClientType.HTTP_CLIENT)));
        EXECUTOR_SERVICE.execute(() -> doGet(buildRequestCircuitBreakerUrl(HttpClientType.OK_HTTP3)));
        Thread.sleep(500);
        EXECUTOR_SERVICE.execute(() -> {
            Result result = doGet(buildRequestCircuitBreakerUrl(HttpClientType.HTTP_CLIENT));
            Assertions.assertNotEquals(HttpStatus.SC_OK, result.getCode());
        });
        EXECUTOR_SERVICE.execute(() -> {
            Result result = doGet(buildRequestCircuitBreakerUrl(HttpClientType.OK_HTTP2));
            Assertions.assertNotEquals(HttpStatus.SC_OK, result.getCode());
        });
        EXECUTOR_SERVICE.execute(() -> {
            Result result = doGet(buildRequestCircuitBreakerUrl(HttpClientType.OK_HTTP3));
            Assertions.assertNotEquals(HttpStatus.SC_OK, result.getCode());
        });
        EXECUTOR_SERVICE.execute(() -> {
            Result result = doGet(buildRequestCircuitBreakerUrl(HttpClientType.HTTP_URL_CONNECTION));
            Assertions.assertNotEquals(HttpStatus.SC_OK, result.getCode());
        });

        // Test Instance Circuit Breaker Base on GateWayError
        testRemovedCircuitBreakerInstance("v2");

        // Test Instance Circuit Breaker Base on 5XX error
        testRemovedCircuitBreakerInstance("v3");
        Thread.sleep(10000);

        // Test instance recovery
        Result result = doGet(buildInstanceCircuitBreakerUrl(HttpClientType.HTTP_CLIENT, "v2"));
        Result result1 = doGet(buildInstanceCircuitBreakerUrl(HttpClientType.HTTP_CLIENT, "v2"));
        Assertions.assertTrue(HttpStatus.SC_OK != result.getCode() || result1.getCode() != HttpStatus.SC_OK);
        result = doGet(buildInstanceCircuitBreakerUrl(HttpClientType.OK_HTTP2, "v2"));
        result1 = doGet(buildInstanceCircuitBreakerUrl(HttpClientType.OK_HTTP2, "v2"));
        Assertions.assertTrue(HttpStatus.SC_OK != result.getCode() || result1.getCode() != HttpStatus.SC_OK);
        result = doGet(buildInstanceCircuitBreakerUrl(HttpClientType.OK_HTTP3, "v2"));
        result1 = doGet(buildInstanceCircuitBreakerUrl(HttpClientType.OK_HTTP3, "v2"));
        Assertions.assertTrue(HttpStatus.SC_OK != result.getCode() || result1.getCode() != HttpStatus.SC_OK);
        result = doGet(buildInstanceCircuitBreakerUrl(HttpClientType.HTTP_URL_CONNECTION, "v2"));
        result1 = doGet(buildInstanceCircuitBreakerUrl(HttpClientType.HTTP_URL_CONNECTION, "v2"));
        Assertions.assertTrue(HttpStatus.SC_OK != result.getCode() || result1.getCode() != HttpStatus.SC_OK);
    }

    private static void testRemovedCircuitBreakerInstance(String version) {
        for (int i = 0; i < 40; i++) {
            doGet(buildInstanceCircuitBreakerUrl(HttpClientType.HTTP_CLIENT, version));
            doGet(buildInstanceCircuitBreakerUrl(HttpClientType.OK_HTTP3, version));
        }
        for (int i = 0; i < 40; i++) {
            Result result = doGet(buildInstanceCircuitBreakerUrl(HttpClientType.HTTP_CLIENT, version));
            Assertions.assertEquals(HttpStatus.SC_OK, result.getCode());
            result = doGet(buildInstanceCircuitBreakerUrl(HttpClientType.OK_HTTP2, version));
            Assertions.assertEquals(HttpStatus.SC_OK, result.getCode());
            result = doGet(buildInstanceCircuitBreakerUrl(HttpClientType.OK_HTTP3, version));
            Assertions.assertEquals(HttpStatus.SC_OK, result.getCode());
            result = doGet(buildInstanceCircuitBreakerUrl(HttpClientType.HTTP_URL_CONNECTION, version));
            Assertions.assertEquals(HttpStatus.SC_OK, result.getCode());
        }
    }

    /**
     * test Circuit Breaker
     */
    @Test
    @EnabledIfSystemProperty(named = "xds.service.integration.test.type", matches = "FLOW_CONTROL_RATE_LIMIT")
    public void testRateLimit() throws InterruptedException {
        // Test the case where the request delay probability is 0
        for (int i = 0; i < 10; i++) {
            Result result = doGet(buildRateLimitUrl(HttpClientType.HTTP_CLIENT, "v1"));
            Assertions.assertEquals(HttpStatus.SC_OK, result.getCode());
            result = doGet(buildRateLimitUrl(HttpClientType.OK_HTTP2, "v1"));
            Assertions.assertEquals(HttpStatus.SC_OK, result.getCode());
            result = doGet(buildRateLimitUrl(HttpClientType.OK_HTTP3, "v1"));
            Assertions.assertEquals(HttpStatus.SC_OK, result.getCode());
            result = doGet(buildRateLimitUrl(HttpClientType.HTTP_URL_CONNECTION, "v1"));
            Assertions.assertEquals(HttpStatus.SC_OK, result.getCode());
        }

        // Test the case where the probability is 50%
        testRateLimitProbability(buildRateLimitUrl(HttpClientType.HTTP_CLIENT, "v2"), 30, 70);
        testRateLimitProbability(buildRateLimitUrl(HttpClientType.OK_HTTP2, "v2"), 30, 70);
        testRateLimitProbability(buildRateLimitUrl(HttpClientType.OK_HTTP3, "v2"), 30, 70);
        testRateLimitProbability(buildRateLimitUrl(HttpClientType.HTTP_URL_CONNECTION, "v2"), 30, 70);

        // Test the case where the probability is 100%
        testRateLimitProbability(buildRateLimitUrl(HttpClientType.HTTP_CLIENT, "v3"), 90, 104);
        testRateLimitProbability(buildRateLimitUrl(HttpClientType.OK_HTTP2, "v3"), 90, 104);
        testRateLimitProbability(buildRateLimitUrl(HttpClientType.OK_HTTP3, "v3"), 90, 104);
        testRateLimitProbability(buildRateLimitUrl(HttpClientType.HTTP_URL_CONNECTION, "v3"), 90, 104);

        // Test token filling
        Thread.sleep(10000);
        Result result = doGet(buildRateLimitUrl(HttpClientType.HTTP_CLIENT, "v2"));
        Assertions.assertEquals(HttpStatus.SC_OK, result.getCode());
        result = doGet(buildRateLimitUrl(HttpClientType.HTTP_URL_CONNECTION, "v2"));
        Assertions.assertEquals(HttpStatus.SC_OK, result.getCode());
        result = doGet(buildRateLimitUrl(HttpClientType.OK_HTTP2, "v2"));
        Assertions.assertEquals(HttpStatus.SC_OK, result.getCode());
        result = doGet(buildRateLimitUrl(HttpClientType.OK_HTTP3, "v2"));
        Assertions.assertEquals(HttpStatus.SC_OK, result.getCode());
    }

    private static void testRateLimitProbability(String url, int minFailureCount, int maxFailureCount) {
        Result result;
        int requestFailureCount = 0;
        for (int i = 0; i < 104; i++) {
            result = doGet(url);
            if (result.getCode() != HttpStatus.SC_OK) {
                requestFailureCount++;
            }
        }
        Assertions.assertTrue(requestFailureCount >= minFailureCount && requestFailureCount <= maxFailureCount);
    }

    private static void testRequestFailureCount(String url) {
        Result result = doGet(url);
        Assertions.assertEquals(HttpStatus.SC_OK, result.getCode());
        Assertions.assertEquals(3, getRequestCount());
        resetRequestCount();
    }

    private static void testFaultProbability(String url, int minFailureCount, int maxFailureCount, int sleepTime)
            throws InterruptedException {
        final AtomicInteger faultCount = new AtomicInteger();
        for (int i = 0; i < 50; i++) {
            EXECUTOR_SERVICE.execute(() -> {
                long start = System.currentTimeMillis();
                Result result = doGet(url);
                long elapsed = System.currentTimeMillis() - start;
                if (result.getCode() != HttpStatus.SC_OK || elapsed > 5000) {
                    faultCount.incrementAndGet();
                }
            });
        }
        Thread.sleep(sleepTime);
        int count = faultCount.get();
        Assertions.assertTrue(count >= minFailureCount && count <= maxFailureCount);
    }

    private static void resetRequestCount() {
        // The server has two instances, and it needs to be called twice
        doGet("http://127.0.0.1:8080/flowControl/testHttpClient?host=spring-server&version=v1&path=reset");
        doGet("http://127.0.0.1:8080/flowControl/testHttpClient?host=spring-server&version=v1&path=reset");
    }

    private static int getRequestCount() {
        // The server has two instances, and it needs to be called twice
        Result result = doGet("http://127.0.0.1:8080/flowControl/testHttpClient?host=spring-server&version=v1&path=getRequestCount");
        int requestFailureCount = Integer.parseInt(result.getData().toString());
        result = doGet("http://127.0.0.1:8080/flowControl/testHttpClient?host=spring-server&version=v1&path=getRequestCount");
        return requestFailureCount + Integer.parseInt(result.getData().toString());
    }


    private static Result doGet(String url) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(CONNECT_TIMEOUT)
                    .setSocketTimeout(SOCKET_TIMEOUT).build();
            HttpGet httpGet = new HttpGet(url);
            httpGet.setConfig(requestConfig);
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                return JSONObject.parseObject(EntityUtils.toString(response.getEntity()), Result.class);
            }
        } catch (IOException e) {
            return new Result(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage(), null);
        }
    }

    private static String buildRateLimitUrl(HttpClientType clientType, String version) {
        return buildUrl(clientType, version, "&path=testRateLimit");
    }

    private static String buildInstanceCircuitBreakerUrl(HttpClientType clientType, String version) {
        return buildUrl(clientType, version, "&path=testInstanceCircuitBreaker");
    }

    private static String buildRequestCircuitBreakerUrl(HttpClientType clientType) {
        return buildUrl(clientType, "v1", "&path=testRequestCircuitBreaker");
    }

    private static String buildGateWayErrorUrl(HttpClientType clientType, String version) {
        return buildUrl(clientType, version, "&path=testGateWayError");
    }

    private static String buildFaultUrl(HttpClientType clientType, String version) {
        return buildUrl(clientType, version, "&path=testFault");
    }

    private static String buildUrl(HttpClientType clientType, String version, String path) {
        if (clientType == HttpClientType.OK_HTTP2) {
            return OKHTTP_URL_PREFIX + version + path;
        }
        if (clientType == HttpClientType.OK_HTTP3) {
            return OKHTTP3_URL_PREFIX + version + path;
        }
        if (clientType == HttpClientType.HTTP_CLIENT) {
            return HTTP_CLIENT_URL_PREFIX + version + path;
        }
        if (clientType == HttpClientType.HTTP_URL_CONNECTION) {
            return HTTP_URL_CONNECTION_URL_PREFIX + version + path;
        }
        return "";
    }
}
