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

package io.sermant.dynamic.config.interceptors;

import io.sermant.core.classloader.ClassLoaderManager;
import io.sermant.core.common.LoggerFactory;
import io.sermant.core.plugin.agent.entity.ExecuteContext;
import io.sermant.core.service.ServiceManager;
import io.sermant.core.service.inject.ClassInjectDefine;
import io.sermant.core.service.inject.ClassInjectDefine.Plugin;
import io.sermant.core.service.inject.ClassInjectService;

import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Intercept loadFactories for injecting custom configuration sources
 *
 * @author zhouss
 * @since 2022-04-08
 */
public class SpringFactoriesInterceptor extends DynamicConfigSwitchSupport {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    private static final List<ClassInjectDefine> CLASS_DEFINES = new ArrayList<>();

    private static final AtomicBoolean IS_INJECTED = new AtomicBoolean();

    private volatile Boolean hasMethodLoadSpringFactories;

    /**
     * 初始化加载注入定义
     */
    public SpringFactoriesInterceptor() {
        for (ClassInjectDefine define : ServiceLoader.load(ClassInjectDefine.class, this.getClass().getClassLoader())) {
            if (define.plugin() == Plugin.DYNAMIC_CONFIG_PLUGIN) {
                CLASS_DEFINES.add(define);
            }
        }
    }

    @Override
    public ExecuteContext doAfter(ExecuteContext context) {
        if (isHasMethodLoadSpringFactories()) {
            // 仅当在高版本采用LoadSpringFactories的方式注入, 高版本存在缓存会更加高效, 仅需注入一次
            if (IS_INJECTED.compareAndSet(false, true)) {
                injectConfigurations(context.getResult());
            }
        } else {
            final Object rawFactoryType = context.getArguments()[0];
            if (rawFactoryType instanceof Class) {
                final Class<?> factoryType = (Class<?>) rawFactoryType;
                injectConfigurationsWithLowVersion(context.getResult(), factoryType.getName());
            }
        }
        return context;
    }

    private boolean isHasMethodLoadSpringFactories() {
        if (hasMethodLoadSpringFactories == null) {
            try {
                SpringFactoriesLoader.class.getDeclaredMethod("loadSpringFactories", ClassLoader.class);
                hasMethodLoadSpringFactories = Boolean.TRUE;
            } catch (NoSuchMethodException ex) {
                LoggerFactory.getLogger().fine(
                        "It is low version spring framework, class will be injected by loadFactoryNames");
                hasMethodLoadSpringFactories = Boolean.FALSE;
            }
        }
        return hasMethodLoadSpringFactories;
    }

    private void injectConfigurationsWithLowVersion(Object result, String factoryName) {
        final ClassInjectService service = ServiceManager.getService(ClassInjectService.class);
        final ClassLoader contextClassLoader = ClassLoaderManager.getContextClassLoaderOrUserClassLoader();
        if (result instanceof List) {
            final List<String> convertedResult = (List<String>) result;
            CLASS_DEFINES.forEach(classInjectDefine -> {
                final List<String> injectClasses = service.injectConfiguration(factoryName, null,
                        classInjectDefine, contextClassLoader);
                injectClasses.stream().filter(injectClass -> !convertedResult.contains(injectClass))
                        .forEach(convertedResult::add);
            });
        }
    }

    private void injectConfigurations(Object result) {
        final ClassInjectService service = ServiceManager.getService(ClassInjectService.class);
        final ClassLoader contextClassLoader = ClassLoaderManager.getContextClassLoaderOrUserClassLoader();
        final boolean isMultiValueMap = result instanceof MultiValueMap;
        if (result instanceof Map) {
            // spring 高版本处理, 针对List其为不可变list，需做一层处理
            CLASS_DEFINES.forEach(classInjectDefine -> service.injectConfiguration((Map<String, List<String>>) result,
                    classInjectDefine, contextClassLoader, !isMultiValueMap));
        } else {
            LOGGER.warning(String.format(Locale.ENGLISH,
                    "[DynamicConfig] Can not inject dynamic configuration! the type of cache is [%s]",
                    result.getClass().getName()));
        }
    }
}
