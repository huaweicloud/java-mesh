/*
 * Copyright (C) 2024-2024 Sermant Authors. All rights reserved.
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

package io.sermant.flowcontrol.retry.client;

import com.squareup.okhttp.Headers;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.Protocol;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;

import io.sermant.core.plugin.agent.entity.ExecuteContext;
import io.sermant.core.service.xds.entity.ServiceInstance;
import io.sermant.flowcontrol.AbstractXdsHttpClientInterceptor;
import io.sermant.flowcontrol.common.config.CommonConst;
import io.sermant.flowcontrol.common.entity.FlowControlResult;
import io.sermant.flowcontrol.common.entity.FlowControlScenario;
import io.sermant.flowcontrol.common.entity.HttpRequestEntity;
import io.sermant.flowcontrol.common.entity.RequestEntity;
import io.sermant.flowcontrol.common.handler.retry.AbstractRetry;
import io.sermant.flowcontrol.common.util.XdsRouterUtils;
import io.sermant.flowcontrol.common.util.XdsThreadLocalUtil;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;

/**
 * Intercept for versions below okHttp3.1
 *
 * @author zhp
 * @since 2024-12-20
 */
public class OkHttpClientInterceptorChainInterceptor extends AbstractXdsHttpClientInterceptor {
    /**
     * Constructor
     */
    public OkHttpClientInterceptorChainInterceptor() {
        super(new OkHttpRetry());
    }

    @Override
    protected ExecuteContext doBefore(ExecuteContext context) throws Exception {
        Object[] arguments = context.getArguments();
        if (!(arguments[0] instanceof Request)) {
            return context;
        }
        Request request = (Request) arguments[0];

        // Parse the service name, request path, and request header in the request information and convert them into
        // a request entity class
        final Optional<HttpRequestEntity> httpRequestEntity = convertToValidHttpEntity(request);
        if (!httpRequestEntity.isPresent()) {
            return context;
        }
        final FlowControlResult flowControlResult = new FlowControlResult();

        // Execute the flow control handler chain, with only fault for XDS
        getXdsHttpFlowControlService().onBefore(httpRequestEntity.get(), flowControlResult);

        // When triggering some flow control rules, it is necessary to skip execution and return the result directly
        if (flowControlResult.isSkip()) {
            Response.Builder builder = new Response.Builder();
            String msg = flowControlResult.buildResponseMsg();
            context.skip(builder.code(flowControlResult.getResponse().getCode())
                    .message(msg).request(request)
                    .body(ResponseBody.create(MediaType.parse(CommonConst.DEFAULT_CONTENT_TYPE), msg))
                    .protocol(Protocol.HTTP_1_1).build());
            return context;
        }

        // Determine whether the number of requests has reached the threshold, and trigger flow control when the
        // threshold is reached
        if (isNeedCircuitBreak()) {
            Response.Builder builder = new Response.Builder();
            context.skip(builder.code(CommonConst.INTERVAL_SERVER_ERROR)
                    .message(MESSAGE).request(request).protocol(Protocol.HTTP_1_1)
                    .body(ResponseBody.create(MediaType.parse(CommonConst.DEFAULT_CONTENT_TYPE), MESSAGE)).build());
            return context;
        }

        // Execute service invocation and retry logic
        executeWithRetryPolicy(context);
        return context;
    }

    private Optional<HttpRequestEntity> convertToValidHttpEntity(Request httpRequest) {
        URL uri = httpRequest.url();
        String serviceName = uri.getHost().split(CommonConst.ESCAPED_POINT)[0];
        if (!XdsRouterUtils.isXdsRouteRequired(serviceName)) {
            return Optional.empty();
        }
        final Map<String, String> headers = getHeaders(httpRequest);
        return Optional.of(new HttpRequestEntity.Builder()
                .setRequestType(RequestEntity.RequestType.CLIENT)
                .setApiPath(uri.getPath()).setHeaders(headers)
                .setMethod(httpRequest.method())
                .setServiceName(serviceName)
                .build());
    }

    private Map<String, String> getHeaders(Request httpRequest) {
        Map<String, String> headerMap = new HashMap<>();
        Headers headers = httpRequest.headers();
        for (String name : headers.names()) {
            headerMap.putIfAbsent(name, headers.get(name));
        }
        return headerMap;
    }

    @Override
    public int getStatusCode(ExecuteContext context) {
        Optional<String> statusCodeOptional = retry.getCode(context.getResult());
        return statusCodeOptional.map(Integer::parseInt).orElse(CommonConst.DEFAULT_RESPONSE_CODE);
    }

    @Override
    protected void preRetry(Object obj, Method method, Object[] allArguments, Object result, boolean isFirstInvoke) {
        Optional<ServiceInstance> serviceInstanceOptional = chooseServiceInstanceForXds();
        if (!serviceInstanceOptional.isPresent()) {
            return;
        }
        Request request = (Request) allArguments[0];
        ServiceInstance instance = serviceInstanceOptional.get();
        FlowControlScenario scenarioInfo = XdsThreadLocalUtil.getScenarioInfo();
        scenarioInfo.setAddress(instance.getHost() + CommonConst.CONNECT + instance.getPort());
        allArguments[0] = rebuildRequest(request, request.url(), instance);
    }

    private Request rebuildRequest(Request request, URL url, ServiceInstance instance) {
        try {
            URL newUrl = new URL(url.getProtocol(), instance.getHost(), instance.getPort(), url.getFile());
            return request.newBuilder().url(newUrl).build();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Convert url string to url failed.", e.getMessage());
            return request;
        }
    }

    /**
     * OkHttp3 retry
     *
     * @since 2022-02-21
     */
    public static class OkHttpRetry extends AbstractRetry {
        @Override
        public Optional<String> getCode(Object result) {
            if (!(result instanceof Response)) {
                return Optional.empty();
            }
            Response httpResponse = (Response) result;
            return Optional.of(String.valueOf(httpResponse.code()));
        }

        @Override
        public Optional<Set<String>> getHeaderNames(Object result) {
            if (!(result instanceof Response)) {
                return Optional.empty();
            }
            Response httpResponse = (Response) result;
            return Optional.of(httpResponse.headers().names());
        }

        @Override
        public Class<? extends Throwable>[] retryExceptions() {
            return getRetryExceptions();
        }

        @Override
        public RetryFramework retryType() {
            return RetryFramework.SPRING;
        }
    }
}
