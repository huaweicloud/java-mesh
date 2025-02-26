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

package io.sermant.demo.spring.client.util;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;

import io.sermant.demo.spring.common.HttpClientType;
import io.sermant.demo.spring.common.entity.Result;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.springframework.http.HttpMethod;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * controller
 *
 * @author zhp
 * @since 2025-01-13
 **/
public class HttpUtil {
    private static final int MAX_TOTAL = 200;

    private static final int MAX_PER_ROUTE = 20;

    private static final int CONNECT_TIMEOUT = 2000;

    private static final int SOCKET_TIMEOUT = 2000;

    private static final String EMPTY_STR = "";

    private static final CloseableHttpClient HTTP_CLIENT;

    private static final OkHttpClient CLIENT;

    static {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(MAX_TOTAL);
        connectionManager.setDefaultMaxPerRoute(MAX_PER_ROUTE);
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(CONNECT_TIMEOUT)
                .setSocketTimeout(SOCKET_TIMEOUT)
                .setConnectionRequestTimeout(CONNECT_TIMEOUT)
                .build();
        HTTP_CLIENT = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();
        CLIENT = new OkHttpClient();
        CLIENT.setConnectTimeout(CONNECT_TIMEOUT, TimeUnit.MILLISECONDS);
        CLIENT.setWriteTimeout(SOCKET_TIMEOUT, TimeUnit.MILLISECONDS);
        CLIENT.setReadTimeout(SOCKET_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    private HttpUtil() {
    }

    /**
     * send Http get request
     *
     * @param url request url
     * @param headerMap request header information
     * @param httpClientType Client type for sending HTTP requests
     * @return response
     */
    public static Result sendGetRequest(String url, HttpClientType httpClientType, Map<String, String> headerMap) {
        if (httpClientType == HttpClientType.HTTP_CLIENT) {
            return sendGetRequestWithHttpClient(url, headerMap);
        }
        if (httpClientType == HttpClientType.OK_HTTP2) {
            return sendGetRequestWithOkHttp(url, headerMap);
        }
        if (httpClientType == HttpClientType.HTTP_URL_CONNECTION) {
            return sendRequestWithHttpUrlConnection(url, null, headerMap, HttpMethod.GET.name());
        }
        return new Result(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Unsupported HttpClient", null);
    }

    /**
     * send Http Post request
     *
     * @param url request url
     * @param body request body
     * @param httpClientType Client type for sending HTTP requests
     * @param headerMap the header information of request
     * @return response
     */
    public static Result sendPostRequest(String url, Object body, HttpClientType httpClientType,
            Map<String, String> headerMap) {
        if (httpClientType == HttpClientType.HTTP_CLIENT) {
            return sendPostRequestWithHttpClient(url, body, headerMap);
        }
        if (httpClientType == HttpClientType.OK_HTTP2) {
            return sendPostRequestWithOkHttp(url, body, headerMap);
        }
        if (httpClientType == HttpClientType.HTTP_URL_CONNECTION) {
            return sendRequestWithHttpUrlConnection(url, body, headerMap, HttpMethod.POST.name());
        }
        return new Result(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Unsupported HttpClient", null);
    }

    private static Result sendGetRequestWithHttpClient(String url, Map<String, String> headerMap) {
        HttpGet httpGet = new HttpGet(url);
        return sendRequestWithHttpClient(headerMap, httpGet);
    }

    private static Result sendRequestWithHttpClient(Map<String, String> headerMap, HttpRequestBase httpRequestBase) {
        for (Map.Entry<String, String> entry : headerMap.entrySet()) {
            httpRequestBase.addHeader(entry.getKey(), entry.getValue());
        }
        try (CloseableHttpResponse response = HTTP_CLIENT.execute(httpRequestBase)) {
            return new Result(response.getStatusLine().getStatusCode(), EMPTY_STR,
                    EntityUtils.toString(response.getEntity()));
        } catch (IOException e) {
            return new Result(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage(), null);
        }
    }

    private static Result sendPostRequestWithHttpClient(String url, Object body, Map<String, String> headerMap) {
        HttpPost httpPost = new HttpPost(url);
        if (body instanceof HttpEntity) {
            httpPost.setEntity((HttpEntity) body);
        }
        return sendRequestWithHttpClient(headerMap, httpPost);
    }

    private static Result sendGetRequestWithOkHttp(String url, Map<String, String> headerMap) {
        Request.Builder builder = new Request.Builder();
        for (Map.Entry<String, String> entry : headerMap.entrySet()) {
            builder.addHeader(entry.getKey(), entry.getValue());
        }
        Request request = builder.url(url).build();
        return sendRequestWithOkHttp(request);
    }

    private static Result sendPostRequestWithOkHttp(String url, Object body, Map<String, String> headerMap) {
        Request.Builder builder = new Request.Builder();
        for (Map.Entry<String, String> entry : headerMap.entrySet()) {
            builder.addHeader(entry.getKey(), entry.getValue());
        }
        if (body instanceof RequestBody) {
            builder.post((RequestBody) body);
        }
        Request request = builder.url(url).build();
        return sendRequestWithOkHttp(request);
    }

    private static Result sendRequestWithOkHttp(Request request) {
        try {
            Response response = CLIENT.newCall(request).execute();
            try (ResponseBody responseBody = response.body()) {
                return new Result(response.code(), response.message(), new String(responseBody.bytes()));
            }
        } catch (IOException e) {
            return new Result(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage(), null);
        }
    }

    private static Result sendRequestWithHttpUrlConnection(String urlStr, Object body,
            Map<String, String> headerMap, String httpMethod) {
        HttpURLConnection connection = null;
        BufferedReader in = null;
        try {
            connection = buildConnection(urlStr, headerMap, httpMethod);
            if (body != null) {
                connection.setDoOutput(true);
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = body.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
            }
            in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            return new Result(getResponseCode(connection), EMPTY_STR, response);
        } catch (IOException e) {
            return new Result(getResponseCode(connection), e.getMessage(), null);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    private static int getResponseCode(HttpURLConnection connection) {
        if (connection == null) {
            return HttpStatus.SC_INTERNAL_SERVER_ERROR;
        }
        try {
            return connection.getResponseCode();
        } catch (IOException e) {
            return HttpStatus.SC_INTERNAL_SERVER_ERROR;
        }
    }

    private static HttpURLConnection buildConnection(String urlStr, Map<String, String> headerMap,
            String methodType) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(methodType);
        connection.setConnectTimeout(CONNECT_TIMEOUT);
        connection.setReadTimeout(SOCKET_TIMEOUT);
        for (Map.Entry<String, String> entry : headerMap.entrySet()) {
            connection.addRequestProperty(entry.getKey(), entry.getValue());
        }
        return connection;
    }
}
