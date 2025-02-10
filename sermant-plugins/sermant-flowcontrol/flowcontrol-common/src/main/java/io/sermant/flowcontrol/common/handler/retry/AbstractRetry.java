/*
 * Copyright (C) 2022-2022 Huawei Technologies Co., Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.sermant.flowcontrol.common.handler.retry;

import io.sermant.core.common.LoggerFactory;
import io.sermant.core.plugin.config.PluginConfigManager;
import io.sermant.core.service.xds.entity.XdsRetryPolicy;
import io.sermant.core.utils.CollectionUtils;
import io.sermant.core.utils.StringUtils;
import io.sermant.flowcontrol.common.config.CommonConst;
import io.sermant.flowcontrol.common.config.FlowControlConfig;
import io.sermant.flowcontrol.common.support.ReflectMethodCacheSupport;
import io.sermant.flowcontrol.common.xds.retry.RetryCondition;
import io.sermant.flowcontrol.common.xds.retry.RetryConditionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * retryAbstraction，provide a common method
 *
 * @author zhouss
 * @since 2022-02-10
 */
public abstract class AbstractRetry extends ReflectMethodCacheSupport implements Retry {
    /**
     * retry exception class
     */
    protected Class<? extends Throwable>[] classes;

    /**
     * Attempt to load based on user-defined retry exception
     *
     * @param classNames exception class name
     * @return classes
     */
    protected final Class<? extends Throwable>[] findClass(String[] classNames) {
        if (classNames == null || classNames.length == 0) {
            return new Class[0];
        }
        final List<Class<?>> result = new ArrayList<>(classNames.length);
        for (String className : classNames) {
            try {
                result.add(Class.forName(className, false, Thread.currentThread().getContextClassLoader()));
            } catch (ClassNotFoundException exception) {
                LoggerFactory.getLogger().info(String.format(Locale.ENGLISH,
                        "Can not find retry exception class %s", className));
            }
        }
        return result.toArray(new Class[0]);
    }

    @Override
    public boolean isNeedRetry(Set<String> statusList, Object result) {
        if (result == null) {
            return false;
        }
        final Optional<String> code = getCode(result);
        return code.filter(statusList::contains).isPresent();
    }

    @Override
    public boolean isNeedRetry(Object result, XdsRetryPolicy retryPolicy) {
        if (result == null) {
            return false;
        }
        List<String> conditions = retryPolicy.getRetryConditions();
        if (CollectionUtils.isEmpty(conditions)) {
            return false;
        }
        Optional<String> statusCodeOptional = this.getCode(result);
        if (!statusCodeOptional.isPresent()) {
            return false;
        }
        String statusCode = statusCodeOptional.get();
        if (isSuccess(statusCode)) {
            return false;
        }
        for (String conditionName : conditions) {
            Optional<RetryCondition> retryConditionOptional = RetryConditionType
                    .getRetryConditionWithResultByName(conditionName);
            if (!retryConditionOptional.isPresent()) {
                continue;
            }
            if (retryConditionOptional.get().isNeedRetry(this, null, statusCode, result)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isNeedRetry(Throwable ex, XdsRetryPolicy retryPolicy) {
        if (ex == null) {
            return false;
        }
        for (String conditionName : retryPolicy.getRetryConditions()) {
            Optional<RetryCondition> retryConditionOptional = RetryConditionType
                    .getRetryConditionWithExceptionByName(conditionName);
            if (!retryConditionOptional.isPresent()) {
                continue;
            }
            if (retryConditionOptional.get().isNeedRetry(this, ex, null, null)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determine if the request is successful
     *
     * @param statusCode status code
     * @return if the request is successful,true : success false: failure
     */
    public static boolean isSuccess(String statusCode) {
        if (StringUtils.isEmpty(statusCode)) {
            return false;
        }
        int code = Integer.parseInt(statusCode);
        return code >= CommonConst.MIN_SUCCESS_STATUS_CODE && code <= CommonConst.MAX_SUCCESS_STATUS_CODE;
    }

    /**
     * implemented by subclasses， if subclass implement {@link #isNeedRetry(Set, Object)}, no need to implement the get
     * code method
     *
     * @param result interface response result
     * @return response status code
     * @throws UnsupportedOperationException unsupported operation
     */
    public Optional<String> getCode(Object result) {
        return Optional.empty();
    }

    /**
     * Get the name of the response header in the response information
     *
     * @param result interface response result
     * @return response header names
     * @throws UnsupportedOperationException unsupported operation
     */
    public Optional<Set<String>> getHeaderNames(Object result) {
        return Optional.empty();
    }

    /**
     * get retry exception
     *
     * @return Class<? extends Throwable>[]
     * @throws IllegalArgumentException parameter is thrown illegally
     */
    protected final Class<? extends Throwable>[] getRetryExceptions() {
        if (classes != null) {
            return classes;
        }
        synchronized (this) {
            final RetryFramework retryFramework = retryType();
            final FlowControlConfig pluginConfig = PluginConfigManager.getPluginConfig(FlowControlConfig.class);
            String[] retryExceptions;
            if (retryFramework == RetryFramework.SPRING_CLOUD || retryFramework == RetryFramework.SPRING) {
                retryExceptions = pluginConfig.getSpringRetryExceptions();
            } else if (retryFramework == RetryFramework.ALIBABA_DUBBO) {
                retryExceptions = pluginConfig.getAlibabaDubboRetryExceptions();
            } else if (retryFramework == RetryFramework.APACHE_DUBBO) {
                retryExceptions = pluginConfig.getApacheDubboRetryExceptions();
            } else {
                throw new IllegalArgumentException("Not supported retry framework! " + retryFramework);
            }
            classes = findClass(retryExceptions);
        }
        return classes;
    }
}
