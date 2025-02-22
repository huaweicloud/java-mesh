/*
 * Copyright (C) 2022-2022 Huawei Technologies Co., Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sermant.discovery.interceptors.httpclient;

import io.sermant.core.classloader.ClassLoaderManager;
import io.sermant.core.common.LoggerFactory;
import io.sermant.core.plugin.agent.entity.ExecuteContext;
import io.sermant.core.plugin.service.PluginServiceManager;
import io.sermant.core.utils.ClassUtils;
import io.sermant.core.utils.LogUtils;
import io.sermant.discovery.entity.ErrorCloseableHttpResponse;
import io.sermant.discovery.entity.HttpCommonRequest;
import io.sermant.discovery.entity.ServiceInstance;
import io.sermant.discovery.interceptors.MarkInterceptor;
import io.sermant.discovery.retry.InvokerContext;
import io.sermant.discovery.service.InvokerService;
import io.sermant.discovery.utils.HttpConstants;
import io.sermant.discovery.utils.PlugEffectWhiteBlackUtils;
import io.sermant.discovery.utils.RequestInterceptorUtils;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.Configurable;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.util.EntityUtils;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * http interception only for version 4. x
 *
 * @author zhouss
 * @since 2022-10-10
 */
public class HttpClient4xInterceptor extends MarkInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    private static final String ERROR_RESPONSE_CLASS = "io.sermant.discovery.entity.ErrorCloseableHttpResponse";

    private static final String COMMON_REQUEST_CLASS = "io.sermant.discovery.entity.HttpCommonRequest";

    private final AtomicBoolean isLoaded = new AtomicBoolean();

    @Override
    public ExecuteContext doBefore(ExecuteContext context) {
        LogUtils.printHttpRequestBeforePoint(context);
        final InvokerService invokerService = PluginServiceManager.getPluginService(InvokerService.class);
        HttpHost httpHost = (HttpHost) context.getArguments()[0];
        final HttpRequest httpRequest = (HttpRequest) context.getArguments()[1];
        if (!PlugEffectWhiteBlackUtils.isHostEqualRealmName(httpHost.getHostName())) {
            return context;
        }
        final Map<String, String> hostAndPath = RequestInterceptorUtils.recoverUrl(httpRequest.getRequestLine()
                .getUri());
        if (hostAndPath.isEmpty()) {
            return context;
        }
        if (!PlugEffectWhiteBlackUtils.isPlugEffect(hostAndPath.get(HttpConstants.HTTP_URI_SERVICE))) {
            return context;
        }
        RequestInterceptorUtils.printRequestLog("HttpClient", hostAndPath);
        invokerService.invoke(
                        buildInvokerFunc(hostAndPath, httpRequest, context),
                        buildExFunc(httpRequest, ClassLoaderManager.getContextClassLoaderOrUserClassLoader()),
                        hostAndPath.get(HttpConstants.HTTP_URI_SERVICE))
                .ifPresent(result -> this.setResultOrThrow(context, result,
                        hostAndPath.get(HttpConstants.HTTP_URI_PATH)));
        return context;
    }

    private void setResultOrThrow(ExecuteContext context, Object result, String url) {
        if (result instanceof IOException) {
            LOGGER.log(Level.SEVERE, "Http client request failed, uri is " + url, (Exception) result);
            context.setThrowableOut((Exception) result);
            return;
        }
        context.skip(result);
    }

    private Function<InvokerContext, Object> buildInvokerFunc(Map<String, String> hostAndPath, HttpRequest httpRequest,
            ExecuteContext context) {
        final String method = httpRequest.getRequestLine().getMethod();
        final ClassLoader appClassloader = ClassLoaderManager.getContextClassLoaderOrUserClassLoader();
        final AtomicReference<HttpResponse> lastResult = new AtomicReference<>();
        return invokerContext -> {
            tryClose(lastResult.get());
            final ClassLoader pluginClassloader = ClassLoaderManager.getContextClassLoaderOrUserClassLoader();
            Thread.currentThread().setContextClassLoader(appClassloader);
            try {
                final ServiceInstance serviceInstance = invokerContext.getServiceInstance();
                String uriNew = RequestInterceptorUtils.buildUrlWithIp(hostAndPath,
                        serviceInstance.getIp(), serviceInstance.getPort());
                final HttpRequest curRequest = rebuildRequest(uriNew, method, httpRequest);
                context.getArguments()[0] = rebuildHttpHost(hostAndPath.get(HttpConstants.HTTP_URL_SCHEME),
                        serviceInstance);
                context.getArguments()[1] = curRequest;
            } finally {
                Thread.currentThread().setContextClassLoader(pluginClassloader);
            }
            final Object result = RequestInterceptorUtils.buildFunc(context, invokerContext).get();
            if (result instanceof HttpResponse) {
                lastResult.set((HttpResponse) result);
            }
            return result;
        };
    }

    private void tryClose(HttpResponse httpResponse) {
        if (httpResponse == null) {
            return;
        }
        try {
            try {
                EntityUtils.consume(httpResponse.getEntity());
            } finally {
                if (httpResponse instanceof Closeable) {
                    ((Closeable) httpResponse).close();
                }
            }
        } catch (IOException ex) {
            LOGGER.warning("An exception occurred when attempting to close the httpResponse.");
        }
    }

    private Function<Throwable, Object> buildExFunc(HttpRequest httpRequest, ClassLoader appClassloader) {
        return ex -> {
            if (ex instanceof IOException) {
                return ex;
            }
            final ClassLoader pluginClassloader = ClassLoaderManager.getContextClassLoaderOrUserClassLoader();
            Thread.currentThread().setContextClassLoader(appClassloader);
            try {
                return new ErrorCloseableHttpResponse(ex, httpRequest.getProtocolVersion());
            } finally {
                Thread.currentThread().setContextClassLoader(pluginClassloader);
            }
        };
    }

    @Override
    protected void ready() {
        if (isLoaded.compareAndSet(false, true)) {
            final ClassLoader classLoader = HttpClient.class.getClassLoader();
            ClassUtils.defineClass(ERROR_RESPONSE_CLASS, classLoader);
            ClassUtils.defineClass(COMMON_REQUEST_CLASS, classLoader);
        }
    }

    private HttpHost rebuildHttpHost(String scheme, ServiceInstance serviceInstance) {
        return new HttpHost(serviceInstance.getHost(), serviceInstance.getPort(), scheme);
    }

    private HttpRequest rebuildRequest(String uriNew, String method, HttpRequest httpRequest) {
        try {
            HttpUriRequest httpUriRequest = (HttpUriRequest) httpRequest;
            switch (method) {
                case "POST":
                    return buildHttpPost(uriNew, httpUriRequest);
                case "GET":
                    return buildHttpGet(uriNew, httpUriRequest);
                case "PUT":
                    return buildHttpPut(uriNew, httpUriRequest);
                case "DELETE":
                    return buildHttpDelete(uriNew, httpUriRequest);
                case "PATCH":
                    return buildHttpPatch(uriNew, httpUriRequest);
                case "HEAD":
                    return buildHttpHead(uriNew, httpUriRequest);
                case "OPTIONS":
                    return buildHttpOptions(uriNew, httpUriRequest);
                case "TRACE":
                    return buildHttpTrace(uriNew, httpUriRequest);
                default:
                    return new HttpCommonRequest(method, uriNew);
            }
        } catch (ClassCastException exception) {
            LOGGER.severe("Method implementation of HttpClient is not supported!" + exception.getMessage());
            return new HttpCommonRequest(method, uriNew);
        }
    }

    private HttpTrace buildHttpTrace(String uriNew, HttpUriRequest httpUriRequest) {
        HttpTrace httpTrace = new HttpTrace(uriNew);
        httpTrace.setHeaders(httpUriRequest.getAllHeaders());
        httpTrace.setProtocolVersion(httpUriRequest.getProtocolVersion());
        httpTrace.setParams(httpUriRequest.getParams());
        if (httpUriRequest instanceof Configurable) {
            Configurable configurable = (Configurable) httpUriRequest;
            httpTrace.setConfig(configurable.getConfig());
        }
        return httpTrace;
    }

    private HttpOptions buildHttpOptions(String uriNew, HttpUriRequest httpUriRequest) {
        HttpOptions httpOptions = new HttpOptions(uriNew);
        httpOptions.setHeaders(httpUriRequest.getAllHeaders());
        httpOptions.setProtocolVersion(httpUriRequest.getProtocolVersion());
        httpOptions.setParams(httpUriRequest.getParams());
        if (httpUriRequest instanceof Configurable) {
            Configurable configurable = (Configurable) httpUriRequest;
            httpOptions.setConfig(configurable.getConfig());
        }
        return httpOptions;
    }

    private HttpHead buildHttpHead(String uriNew, HttpUriRequest httpUriRequest) {
        HttpHead httpHead = new HttpHead(uriNew);
        httpHead.setHeaders(httpUriRequest.getAllHeaders());
        httpHead.setProtocolVersion(httpUriRequest.getProtocolVersion());
        httpHead.setParams(httpUriRequest.getParams());
        if (httpUriRequest instanceof Configurable) {
            Configurable configurable = (Configurable) httpUriRequest;
            httpHead.setConfig(configurable.getConfig());
        }
        return httpHead;
    }

    private HttpPatch buildHttpPatch(String uriNew, HttpUriRequest httpUriRequest) {
        HttpEntityEnclosingRequest httpEntityEnclosingRequest = (HttpEntityEnclosingRequest) httpUriRequest;
        HttpPatch httpPatch = new HttpPatch(uriNew);
        httpPatch.setEntity(httpEntityEnclosingRequest.getEntity());
        httpPatch.setHeaders(httpUriRequest.getAllHeaders());
        httpPatch.setProtocolVersion(httpUriRequest.getProtocolVersion());
        httpPatch.setParams(httpUriRequest.getParams());
        if (httpUriRequest instanceof Configurable) {
            Configurable configurable = (Configurable) httpUriRequest;
            httpPatch.setConfig(configurable.getConfig());
        }
        return httpPatch;
    }

    private HttpDelete buildHttpDelete(String uriNew, HttpUriRequest httpUriRequest) {
        HttpDelete httpDelete = new HttpDelete(uriNew);
        httpDelete.setHeaders(httpUriRequest.getAllHeaders());
        httpDelete.setProtocolVersion(httpUriRequest.getProtocolVersion());
        httpDelete.setParams(httpUriRequest.getParams());
        if (httpUriRequest instanceof Configurable) {
            Configurable configurable = (Configurable) httpUriRequest;
            httpDelete.setConfig(configurable.getConfig());
        }
        return httpDelete;
    }

    private HttpPut buildHttpPut(String uriNew, HttpUriRequest httpUriRequest) {
        HttpEntityEnclosingRequest httpEntityEnclosingRequest = (HttpEntityEnclosingRequest) httpUriRequest;
        HttpPut httpPut = new HttpPut(uriNew);
        httpPut.setEntity(httpEntityEnclosingRequest.getEntity());
        httpPut.setHeaders(httpUriRequest.getAllHeaders());
        httpPut.setProtocolVersion(httpUriRequest.getProtocolVersion());
        httpPut.setParams(httpUriRequest.getParams());
        if (httpUriRequest instanceof Configurable) {
            Configurable configurable = (Configurable) httpUriRequest;
            httpPut.setConfig(configurable.getConfig());
        }
        return httpPut;
    }

    private HttpGet buildHttpGet(String uriNew, HttpUriRequest httpUriRequest) {
        HttpGet httpGet = new HttpGet(uriNew);
        httpGet.setHeaders(httpUriRequest.getAllHeaders());
        httpGet.setProtocolVersion(httpUriRequest.getProtocolVersion());
        httpGet.setParams(httpUriRequest.getParams());
        if (httpUriRequest instanceof Configurable) {
            Configurable configurable = (Configurable) httpUriRequest;
            httpGet.setConfig(configurable.getConfig());
        }
        return httpGet;
    }

    private HttpPost buildHttpPost(String uriNew, HttpUriRequest httpUriRequest) {
        HttpEntityEnclosingRequest httpEntityEnclosingRequest = (HttpEntityEnclosingRequest) httpUriRequest;
        HttpPost httpPost = new HttpPost(uriNew);
        httpPost.setEntity(httpEntityEnclosingRequest.getEntity());
        httpPost.setHeaders(httpUriRequest.getAllHeaders());
        httpPost.setProtocolVersion(httpUriRequest.getProtocolVersion());
        httpPost.setParams(httpUriRequest.getParams());
        if (httpUriRequest instanceof Configurable) {
            Configurable configurable = (Configurable) httpUriRequest;
            httpPost.setConfig(configurable.getConfig());
        }
        return httpPost;
    }

    @Override
    public ExecuteContext after(ExecuteContext context) throws Exception {
        LogUtils.printHttpRequestAfterPoint(context);
        return context;
    }

    @Override
    public ExecuteContext onThrow(ExecuteContext context) {
        LogUtils.printHttpRequestOnThrowPoint(context);
        return context;
    }
}
