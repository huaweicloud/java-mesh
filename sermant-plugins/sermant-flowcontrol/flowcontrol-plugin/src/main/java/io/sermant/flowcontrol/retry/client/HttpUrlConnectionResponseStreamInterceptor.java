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

import io.github.resilience4j.retry.RetryConfig;
import io.sermant.core.common.LoggerFactory;
import io.sermant.core.plugin.agent.entity.ExecuteContext;
import io.sermant.core.service.xds.entity.ServiceInstance;
import io.sermant.core.service.xds.entity.XdsRetryPolicy;
import io.sermant.core.utils.CollectionUtils;
import io.sermant.core.utils.MapUtils;
import io.sermant.core.utils.ReflectUtils;
import io.sermant.core.utils.StringUtils;
import io.sermant.flowcontrol.AbstractXdsHttpClientInterceptor;
import io.sermant.flowcontrol.common.config.CommonConst;
import io.sermant.flowcontrol.common.handler.retry.AbstractRetry;
import io.sermant.flowcontrol.common.util.XdsThreadLocalUtil;
import io.sermant.flowcontrol.common.xds.retry.RetryCondition;
import io.sermant.flowcontrol.common.xds.retry.RetryConditionType;
import sun.net.www.http.HttpClient;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Intercept the HttpUrlConnection#getInputSteam method
 *
 * @author zhp
 * @since 2024-12-20
 */
public class HttpUrlConnectionResponseStreamInterceptor extends AbstractXdsHttpClientInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    /**
     * Constructor
     */
    public HttpUrlConnectionResponseStreamInterceptor() {
        super(new HttpUrlConnectionRetry());
    }

    @Override
    protected ExecuteContext doBefore(ExecuteContext context) throws Exception {
        XdsThreadLocalUtil.removeConnectionStatus();
        executeWithRetryPolicy(context);
        return context;
    }

    @Override
    public ExecuteContext doAfter(ExecuteContext context) {
        return context;
    }

    @Override
    public ExecuteContext doThrow(ExecuteContext context) {
        return context;
    }

    @Override
    protected boolean canInvoke(ExecuteContext context) {
        return XdsThreadLocalUtil.isConnected() && XdsThreadLocalUtil.getScenarioInfo() != null;
    }

    @Override
    protected int getStatusCode(ExecuteContext context) {
        HttpURLConnection connection = (HttpURLConnection) context.getObject();
        try {
            return connection.getResponseCode();
        } catch (IOException io) {
            LOGGER.log(Level.SEVERE, "Failed to get response code.", io);
            return CommonConst.DEFAULT_RESPONSE_CODE;
        }
    }

    @Override
    protected boolean isNeedRetry(io.github.resilience4j.retry.Retry retry, Object result, Throwable throwable) {
        final RetryConfig retryConfig = retry.getRetryConfig();
        boolean isNeedRetry = retryConfig.getExceptionPredicate().test(throwable);
        if (isNeedRetry) {
            final long interval = retryConfig.getIntervalBiFunction().apply(1, null);
            try {
                // wait according to the first wait time
                Thread.sleep(interval);
            } catch (InterruptedException e) {
                LOGGER.log(Level.WARNING, "Interruption error:", e);
            }
        }
        return isNeedRetry;
    }

    @Override
    protected void preRetry(Object obj, Method method, Object[] allArguments, Object result, boolean isFirstInvoke) {
        tryCloseOldInputStream(result);
        if (isFirstInvoke) {
            return;
        }
        HttpURLConnection connection = (HttpURLConnection) obj;
        ReflectUtils.setFieldValue(connection,"inputStream", null);
        ReflectUtils.setFieldValue(connection,"cachedInputStream", null);
        ReflectUtils.setFieldValue(connection, "cachedHeaders", null);
        ReflectUtils.setFieldValue(connection, "filteredHeaders", null);
        Optional<ServiceInstance> serviceInstanceOptional = chooseServiceInstanceForXds();
        if (!serviceInstanceOptional.isPresent()) {
            return;
        }
        ServiceInstance instance = serviceInstanceOptional.get();
        resetStats(obj);
        URL url = connection.getURL();
        URL newUrl = buildUrl(url, instance);
        ReflectUtils.setFieldValue(connection, "url", newUrl);
        resetHttpClient(connection, newUrl);
    }

    private static URL buildUrl(URL url, ServiceInstance instance) {
        try {
            return new URL(url.getProtocol(), instance.getHost(), instance.getPort(), url.getFile());
        } catch (MalformedURLException e) {
            LOGGER.log(Level.SEVERE, "Can not parse to URL for url.", e);
            return url;
        }
    }

    private void resetStats(Object object) {
        ReflectUtils.setFieldValue(object, "rememberedException", null);
        ReflectUtils.setFieldValue(object, "failedOnce", false);
        ReflectUtils.setFieldValue(object, "responseCode", CommonConst.DEFAULT_RESPONSE_CODE);
    }

    private void resetHttpClient(Object target, URL url) {
        final Optional<Object> http = ReflectUtils.getFieldValue(target, "http");
        if (http.isPresent() && http.get() instanceof HttpClient) {
            // Close the original httpclient
            ((HttpClient) http.get()).closeServer();
        }
        final Optional<Object> connectTimeout = ReflectUtils.getFieldValue(target, "connectTimeout");
        final Optional<Object> readTimeout = ReflectUtils.getFieldValue(target, "readTimeout");
        if (!connectTimeout.isPresent() || !readTimeout.isPresent()) {
            return;
        }
        final Optional<Object> instProxy = ReflectUtils.getFieldValue(target, "instProxy");
        try {
            HttpClient newClient;
            if (instProxy.isPresent() && instProxy.get() instanceof Proxy) {
                newClient = HttpClient.New(url, (Proxy) instProxy.get(), (int) connectTimeout.get(), true,
                        (sun.net.www.protocol.http.HttpURLConnection) target);
            } else {
                newClient = HttpClient.New(url, null, (int) connectTimeout.get(), true,
                        (sun.net.www.protocol.http.HttpURLConnection) target);
            }
            newClient.setReadTimeout((int) readTimeout.get());
            ReflectUtils.setFieldValue(target, "http", newClient);
        } catch (IOException e) {
            LOGGER.info("Can not create httpclient when invoke!");
        }
    }

    private void tryCloseOldInputStream(Object rawInputStream) {
        if (rawInputStream instanceof Closeable) {
            // Shut down processing for old input streams
            try {
                ((Closeable) rawInputStream).close();
            } catch (IOException e) {
                LOGGER.warning("Close old input stream failed when invoke");
            }
        }
    }

    /**
     * Http url connection retry
     *
     * @since 2022-02-21
     */
    public static class HttpUrlConnectionRetry extends AbstractRetry {
        @Override
        public Optional<String> getCode(Object result) {
            HttpURLConnection connection = XdsThreadLocalUtil.getHttpUrlConnection();
            if (connection == null) {
                return Optional.empty();
            }
            try {
                return Optional.of(String.valueOf(connection.getResponseCode()));
            } catch (IOException io) {
                LOGGER.log(Level.SEVERE, "Failed to get response code.", io);
                return Optional.empty();
            }
        }

        @Override
        public boolean isNeedRetry(Object result, XdsRetryPolicy retryPolicy) {
            return this.isNeedRetry((Throwable) null, retryPolicy);
        }

        @Override
        public boolean isNeedRetry(Throwable throwable, XdsRetryPolicy retryPolicy) {
            List<String> conditions = retryPolicy.getRetryConditions();
            if (CollectionUtils.isEmpty(conditions)) {
                return false;
            }
            Optional<String> statusCodeOptional = this.getCode(null);
            String statusCode = statusCodeOptional.orElse(StringUtils.EMPTY);
            if (isSuccess(statusCode)) {
                return false;
            }
            for (String conditionName : conditions) {
                Optional<RetryCondition> retryConditionOptional =
                        RetryConditionType.getRetryConditionByName(conditionName);
                if (!retryConditionOptional.isPresent()) {
                    continue;
                }
                if (retryConditionOptional.get().isNeedRetry(this, throwable, statusCode, null)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public Optional<Set<String>> getHeaderNames(Object result) {
            HttpURLConnection connection = XdsThreadLocalUtil.getHttpUrlConnection();
            if (connection == null) {
                return Optional.empty();
            }
            Map<String,List<String>> headers = connection.getHeaderFields();
            if (MapUtils.isEmpty(headers)) {
                return Optional.empty();
            }
            return Optional.of(headers.keySet());
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
