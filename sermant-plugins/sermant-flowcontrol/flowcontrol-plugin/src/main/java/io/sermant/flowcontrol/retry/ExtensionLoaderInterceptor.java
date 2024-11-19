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

package io.sermant.flowcontrol.retry;

import io.sermant.core.classloader.ClassLoaderManager;
import io.sermant.core.plugin.agent.entity.ExecuteContext;
import io.sermant.core.utils.ClassUtils;
import io.sermant.flowcontrol.retry.cluster.AlibabaDubboCluster;
import io.sermant.flowcontrol.retry.cluster.AlibabaDubboClusterInvoker;
import io.sermant.flowcontrol.retry.cluster.ApacheDubboCluster;
import io.sermant.flowcontrol.retry.cluster.ApacheDubboClusterInvoker;
import io.sermant.flowcontrol.retry.cluster.ClusterInvokerCreator;
import io.sermant.flowcontrol.service.InterceptorSupporter;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ExtensionLoader Interceptor， for injection cluster
 *
 * @author zhouss
 * @since 2022-03-04
 */
public class ExtensionLoaderInterceptor extends InterceptorSupporter {
    private final AtomicBoolean isDefined = new AtomicBoolean();

    @Override
    protected ExecuteContext doBefore(ExecuteContext context) throws Exception {
        return context;
    }

    /**
     * postsetMethod, for injection cluster
     * <p>{@link AlibabaDubboCluster}</p>
     * <p>{@link AlibabaDubboClusterInvoker}</p>
     * <p>{@link ApacheDubboCluster}</p>
     * <p>{@link ApacheDubboClusterInvoker}</p>
     *
     * @param context execution context
     * @return execution context
     */
    @Override
    protected ExecuteContext doAfter(ExecuteContext context) {
        final Class<?> type = (Class<?>) context.getMemberFieldValue("type");
        if (type == null) {
            return context;
        }
        if (canInjectClusterInvoker(type.getName()) && isDefined.compareAndSet(false, true)) {
            if (!(context.getResult() instanceof Map)) {
                return context;
            }
            final Map<String, Class<?>> classes = (Map<String, Class<?>>) context.getResult();
            final String retryClusterInvoker = flowControlConfig.getRetryClusterInvoker();
            if (classes.get(retryClusterInvoker) != null) {
                return context;
            }
            final Optional<Class<?>> retryInvokerClass;
            final ClassLoader contextClassLoader = ClassLoaderManager.getContextClassLoaderOrUserClassLoader();
            if (APACHE_DUBBO_CLUSTER_CLASS_NAME.equals(type.getName())) {
                ClassUtils.defineClass(
                        "io.sermant.flowcontrol.retry.cluster.ApacheDubboClusterInvoker", contextClassLoader);
                retryInvokerClass = ClassUtils.defineClass(
                        "io.sermant.flowcontrol.retry.cluster.ApacheDubboCluster", contextClassLoader);
            } else if (ALIBABA_DUBBO_CLUSTER_CLASS_NAME.equals(type.getName())) {
                ClassUtils.defineClass(
                        "io.sermant.flowcontrol.retry.cluster.AlibabaDubboClusterInvoker", contextClassLoader);
                retryInvokerClass = ClassUtils.defineClass(
                        "io.sermant.flowcontrol.retry.cluster.AlibabaDubboCluster", contextClassLoader);
            } else {
                return context;
            }
            retryInvokerClass.ifPresent(invokerClass -> classes.put(retryClusterInvoker, invokerClass));
            ClusterInvokerCreator.INSTANCE.getClusterInvokerMap().putAll(classes);
        }
        return context;
    }
}
