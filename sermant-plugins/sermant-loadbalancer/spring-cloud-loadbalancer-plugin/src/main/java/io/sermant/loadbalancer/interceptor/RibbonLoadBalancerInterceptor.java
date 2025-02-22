/*
 * Copyright (C) 2022-2022 Huawei Technologies Co., Ltd. All rights reserved.
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

import com.netflix.loadbalancer.AbstractLoadBalancerRule;
import com.netflix.loadbalancer.BaseLoadBalancer;
import com.netflix.loadbalancer.IRule;

import io.sermant.core.classloader.ClassLoaderManager;
import io.sermant.core.common.LoggerFactory;
import io.sermant.core.plugin.agent.entity.ExecuteContext;
import io.sermant.core.plugin.agent.interceptor.AbstractInterceptor;
import io.sermant.core.plugin.config.PluginConfigManager;
import io.sermant.loadbalancer.cache.RibbonLoadbalancerCache;
import io.sermant.loadbalancer.config.LbContext;
import io.sermant.loadbalancer.config.LoadbalancerConfig;
import io.sermant.loadbalancer.config.RibbonLoadbalancerType;
import io.sermant.loadbalancer.rule.RuleManager;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * Ribbon BaseLoadBalancer load balancing enhancement class
 *
 * @author provenceee
 * @since 2022-02-24
 */
public class RibbonLoadBalancerInterceptor extends AbstractInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    /**
     * default ribbon load balancing key
     */
    private static final String DEFAULT_RIBBON_LOADBALANCER_KEY = "default";

    /**
     * Store which services are backed up by load balancing, regardless of thread safety
     */
    private final Set<String> backUpMarks = new HashSet<>();

    /**
     * service load balancing cache key: serviceName, value: loadBalancing
     */
    private final Map<String, Map<RibbonLoadbalancerType, AbstractLoadBalancerRule>> servicesRuleMap =
            new ConcurrentHashMap<>();

    private final Function<RibbonLoadbalancerType, Optional<AbstractLoadBalancerRule>> ruleCreator = type -> {
        final String clazzName = type.getClazzName();
        final ClassLoader contextClassLoader = ClassLoaderManager.getContextClassLoaderOrUserClassLoader();
        try {
            final Class<?> ruleClazz = contextClassLoader.loadClass(clazzName);
            return Optional.of((AbstractLoadBalancerRule) ruleClazz.newInstance());
        } catch (ClassNotFoundException exception) {
            LOGGER.warning(String.format(Locale.ENGLISH, "Can not load rule clazz [%s]", clazzName));
        } catch (InstantiationException | IllegalAccessException ex) {
            LOGGER.warning(String.format(Locale.ENGLISH, "Can not create rule clazz [%s] by no args constructor",
                    clazzName));
        }
        return Optional.empty();
    };

    private final LoadbalancerConfig config;

    /**
     * construction method
     */
    public RibbonLoadBalancerInterceptor() {
        this.config = PluginConfigManager.getPluginConfig(LoadbalancerConfig.class);
    }

    @Override
    public ExecuteContext before(ExecuteContext context) {
        LbContext.INSTANCE.setCurLoadbalancerType(LbContext.LOADBALANCER_RIBBON);
        setRule(context);
        return context;
    }

    @Override
    public ExecuteContext after(ExecuteContext context) {
        return context;
    }

    private AbstractLoadBalancerRule getTargetRule(String serviceName, RibbonLoadbalancerType type) {
        return getTargetServiceRuleMap(serviceName)
                .computeIfAbsent(type, curType -> ruleCreator.apply(curType).orElse(null));
    }

    private Map<RibbonLoadbalancerType, AbstractLoadBalancerRule> getTargetServiceRuleMap(String serviceName) {
        return servicesRuleMap.computeIfAbsent(serviceName, key -> new ConcurrentHashMap<>());
    }

    private void setRule(ExecuteContext context) {
        if (!RuleManager.INSTANCE.isConfigured()) {
            return;
        }
        final Object rawLoadbalancerKey = context.getArguments()[0];
        if (rawLoadbalancerKey != null && !(rawLoadbalancerKey instanceof String)) {
            return;
        }
        String loadbalancerKey = (String) rawLoadbalancerKey;
        if (!isNeedUseSermantLb(loadbalancerKey)) {
            return;
        }
        final Optional<String> serviceNameOptional = getServiceName(context);
        if (!serviceNameOptional.isPresent()) {
            return;
        }
        String serviceName = serviceNameOptional.get();
        backUp(serviceName, (BaseLoadBalancer) context.getObject());
        final Optional<RibbonLoadbalancerType> targetType = RibbonLoadbalancerCache.INSTANCE
                .getTargetServiceLbType(serviceName);
        if (targetType.isPresent()) {
            doSet(context.getObject(), getTargetRule(serviceName, targetType.get()));
        } else {
            tryUseDefaultType(serviceName, context);
        }
    }

    private void tryUseDefaultType(String serviceName, ExecuteContext context) {
        final String defaultRule = config.getDefaultRule();
        if (defaultRule == null) {
            RibbonLoadbalancerCache.INSTANCE.put(serviceName, null);
            return;
        }
        final Optional<RibbonLoadbalancerType> ribbonLoadbalancerType = RibbonLoadbalancerType
                .matchLoadbalancer(defaultRule);
        if (ribbonLoadbalancerType.isPresent()) {
            final AbstractLoadBalancerRule rule = getTargetRule(serviceName, ribbonLoadbalancerType.get());
            doSet(context.getObject(), rule);
            RibbonLoadbalancerCache.INSTANCE.put(serviceName, ribbonLoadbalancerType.get());
        }
    }

    private boolean isNeedUseSermantLb(String loadbalancerKey) {
        if (loadbalancerKey == null || DEFAULT_RIBBON_LOADBALANCER_KEY.equals(loadbalancerKey)) {
            return true;
        }
        return config.isForceUseSermantLb();
    }

    /**
     * getServiceName
     *
     * @param context context
     * @return service name
     */
    private Optional<String> getServiceName(ExecuteContext context) {
        final Object object = context.getObject();
        if (object instanceof BaseLoadBalancer) {
            return Optional.ofNullable(((BaseLoadBalancer) object).getName());
        }
        return Optional.empty();
    }

    private void doSet(Object obj, AbstractLoadBalancerRule rule) {
        BaseLoadBalancer loadBalancer = (BaseLoadBalancer) obj;
        if (rule == null || loadBalancer.getRule().getClass() == rule.getClass()) {
            // If the original load balancer is the same as required, it does not need to be modified,
            // and directly return, without affecting the original method
            return;
        }
        loadBalancer.setRule(rule);
    }

    private void backUp(String serviceName, BaseLoadBalancer loadBalancer) {
        if (backUpMarks.contains(serviceName)) {
            return;
        }
        backUpMarks.add(serviceName);
        final IRule rule = loadBalancer.getRule();
        final Optional<RibbonLoadbalancerType> ribbonLoadbalancerType = RibbonLoadbalancerType
                .matchLoadbalancerByClazz(rule.getClass().getName());
        if (ribbonLoadbalancerType.isPresent()) {
            RibbonLoadbalancerCache.INSTANCE.backUpOriginType(serviceName, ribbonLoadbalancerType.get());
        } else {
            LOGGER.warning(String.format(Locale.ENGLISH, "Can not resolve rule [%s] when back up",
                    rule.getClass().getName()));
        }
    }
}
