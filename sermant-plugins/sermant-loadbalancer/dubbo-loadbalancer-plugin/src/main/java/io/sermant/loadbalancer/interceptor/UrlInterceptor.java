/*
 * Copyright (C) 2022-2024 Huawei Technologies Co., Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.sermant.loadbalancer.interceptor;

import io.sermant.core.classloader.ClassLoaderManager;
import io.sermant.core.common.LoggerFactory;
import io.sermant.core.plugin.agent.entity.ExecuteContext;
import io.sermant.core.plugin.agent.interceptor.AbstractInterceptor;
import io.sermant.core.plugin.config.PluginConfigManager;
import io.sermant.core.utils.ClassUtils;
import io.sermant.core.utils.ReflectUtils;
import io.sermant.loadbalancer.cache.DubboApplicationCache;
import io.sermant.loadbalancer.cache.DubboLoadbalancerCache;
import io.sermant.loadbalancer.config.DubboLoadbalancerType;
import io.sermant.loadbalancer.config.LbContext;
import io.sermant.loadbalancer.config.LoadbalancerConfig;
import io.sermant.loadbalancer.constants.DubboUrlParamsConstants;
import io.sermant.loadbalancer.rule.LoadbalancerRule;
import io.sermant.loadbalancer.rule.RuleManager;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

/**
 * URL enhancement class
 *
 * @author provenceee
 * @since 2022-01-20
 */
public class UrlInterceptor extends AbstractInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    private static final String ALIBABA_LOADER = "com.alibaba.dubbo.common.extension.ExtensionLoader";

    private static final String ALIBABA_LB = "com.alibaba.dubbo.rpc.cluster.LoadBalance";

    private static final String APACHE_LOADER = "org.apache.dubbo.common.extension.ExtensionLoader";

    private static final String APACHE_LB = "org.apache.dubbo.rpc.cluster.LoadBalance";

    private static final String LOAD_METHOD = "loadExtensionClasses";

    private static final String GET_LOADER_METHOD = "getExtensionLoader";

    private final LoadbalancerConfig config;

    private Set<String> supportRules;

    /**
     * constructor
     */
    public UrlInterceptor() {
        config = PluginConfigManager.getPluginConfig(LoadbalancerConfig.class);
    }

    @Override
    public ExecuteContext before(ExecuteContext context) {
        Object[] arguments = context.getArguments();
        if (arguments != null && arguments.length > 1
                && DubboUrlParamsConstants.DUBBO_LOAD_BALANCER_KEY.equals(arguments[1])) {
            LbContext.INSTANCE.setCurLoadbalancerType(LbContext.LOADBALANCER_DUBBO);

            // 如果为empty，继续执行原方法，即使用宿主的负载均衡策略
            // 如果不为empty，则使用返回的type并跳过原方法
            checkRules();
            getType(context).filter(this::isSupport).ifPresent(context::skip);
        }
        return context;
    }

    private boolean isSupport(String ruleName) {
        if (supportRules.isEmpty()) {
            LOGGER.fine("Supported dubbo lb rule dose not loaded, may be current version is not support!");
            return true;
        }
        if (supportRules.contains(ruleName)) {
            return true;
        }
        LOGGER.fine(String.format(Locale.ENGLISH, "Can not support rule [%s]", ruleName));
        return false;
    }

    private void checkRules() {
        if (supportRules != null) {
            return;
        }
        if (isAlibaba()) {
            fillSupportRules(ALIBABA_LOADER, ALIBABA_LB);
        } else {
            fillSupportRules(APACHE_LOADER, APACHE_LB);
        }
    }

    private void fillSupportRules(String extensionLoaderClazz, String lbClassName) {
        synchronized (this) {
            if (supportRules != null) {
                return;
            }
            supportRules = new HashSet<>();
            final Optional<Class<?>> lbClazz = ClassUtils
                    .loadClass(lbClassName, ClassLoaderManager.getContextClassLoaderOrUserClassLoader(), true);
            if (!lbClazz.isPresent()) {
                return;
            }

            // 获取指定的ExtensionLoader
            final Optional<Object> loader = ReflectUtils.invokeMethod(extensionLoaderClazz, GET_LOADER_METHOD,
                    new Class[]{Class.class},
                    new Object[]{lbClazz.get()});
            if (!loader.isPresent()) {
                return;
            }

            // 调用ExtensionLoader#getLoadedExtensions获取所有lb键, 即规则名
            final Optional<Object> rules = ReflectUtils.invokeMethod(loader.get(), LOAD_METHOD, null, null);
            if (rules.isPresent() && rules.get() instanceof Map) {
                supportRules.addAll((Collection<? extends String>) ((Map<?, ?>) rules.get()).keySet());
            } else {
                LOGGER.warning("Can get loaded lb extensions!");
            }
        }
    }

    private boolean isAlibaba() {
        return ClassUtils.loadClass(ALIBABA_LOADER, ClassLoaderManager.getContextClassLoaderOrUserClassLoader(), false)
                .isPresent();
    }

    @Override
    public ExecuteContext after(ExecuteContext context) {
        return context;
    }

    private Optional<String> getType(ExecuteContext context) {
        if (config == null || !RuleManager.INSTANCE.isConfigured()) {
            // 没有配置的情况下return empty
            return Optional.empty();
        }
        return getRemoteApplication(context).flatMap(application -> matchLoadbalancerType(application)
                .map(loadbalancerType -> loadbalancerType.name().toLowerCase(Locale.ROOT)));
    }

    private Optional<DubboLoadbalancerType> matchLoadbalancerType(String application) {
        final DubboLoadbalancerType cacheType = DubboLoadbalancerCache.INSTANCE.getNewCache()
                .get(application);
        if (cacheType != null) {
            return Optional.of(cacheType);
        }
        final Optional<LoadbalancerRule> targetServiceRule = RuleManager.INSTANCE
                .getTargetServiceRule(application);
        if (!targetServiceRule.isPresent()) {
            return Optional.empty();
        }
        final Optional<DubboLoadbalancerType> dubboLoadbalancerType = DubboLoadbalancerType
                .matchLoadbalancer(targetServiceRule.get().getRule());
        dubboLoadbalancerType.ifPresent(type -> DubboLoadbalancerCache.INSTANCE.getNewCache().put(application, type));
        return dubboLoadbalancerType;
    }

    private Optional<String> getRemoteApplication(ExecuteContext context) {
        final Object target = context.getObject();
        final Optional<String> applicationFromCache = getApplicationFromCache(target);
        if (applicationFromCache.isPresent()) {
            return applicationFromCache;
        }
        return getApplicationFromUrl(target);
    }

    private Optional<Object> invokeGetParameter(Object target, String key) {
        return ReflectUtils.invokeMethod(target, "getParameter", new Class[]{String.class},
                new Object[]{key});
    }

    private Optional<String> getApplicationFromUrl(Object target) {
        final Optional<Object> getParameter = invokeGetParameter(target,
                DubboUrlParamsConstants.DUBBO_REMOTE_APPLICATION);
        return getParameter.map(result -> (String) result);
    }

    private Optional<String> getApplicationFromCache(Object target) {
        final Optional<Object> interfaceName = invokeGetParameter(target, DubboUrlParamsConstants.DUBBO_INTERFACE);
        if (interfaceName.isPresent() && interfaceName.get() instanceof String) {
            final String application = DubboApplicationCache.INSTANCE.getApplicationCache().get(interfaceName.get());
            if (application != null) {
                return Optional.of(application);
            }
        }
        return Optional.empty();
    }
}
