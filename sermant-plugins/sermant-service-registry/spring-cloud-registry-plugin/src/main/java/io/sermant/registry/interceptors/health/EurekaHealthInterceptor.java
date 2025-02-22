/*
 * Copyright (C) 2021-2022 Huawei Technologies Co., Ltd. All rights reserved.
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

package io.sermant.registry.interceptors.health;

import io.sermant.core.classloader.ClassLoaderManager;
import io.sermant.core.common.LoggerFactory;
import io.sermant.core.plugin.agent.entity.ExecuteContext;
import io.sermant.registry.context.RegisterContext;
import io.sermant.registry.handler.SingleStateCloseHandler;

import java.util.logging.Logger;

/**
 * Registration Center Health Status Change
 *
 * @author zhouss
 * @since 2021-12-13
 */
public class EurekaHealthInterceptor extends SingleStateCloseHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    @Override
    protected boolean needCloseRegisterCenter() {
        return super.needCloseRegisterCenter() && super.target != null;
    }

    @Override
    protected void close() throws Exception {
        // Turn off the Eureka timer
        final Class<?> discoveryClientClass = ClassLoaderManager.getContextClassLoaderOrUserClassLoader()
                .loadClass("com.netflix.discovery.DiscoveryClient");
        discoveryClientClass.getDeclaredMethod("shutdown").invoke(target);
        LOGGER.warning("Eureka register center has been closed by user.");
    }

    @Override
    public ExecuteContext doBefore(ExecuteContext context) {
        checkState(context, false);
        return context;
    }

    @Override
    public ExecuteContext doAfter(ExecuteContext context) {
        final Object result = context.getResult();
        if (result instanceof Boolean) {
            final boolean heartbeatResult = (Boolean) result;
            if (heartbeatResult) {
                RegisterContext.INSTANCE.compareAndSet(false, true);
            } else {
                RegisterContext.INSTANCE.compareAndSet(true, false);
            }
        }
        return context;
    }
}
