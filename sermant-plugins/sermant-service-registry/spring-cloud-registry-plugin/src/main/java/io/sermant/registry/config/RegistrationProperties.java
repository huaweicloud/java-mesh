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

package io.sermant.registry.config;

import io.sermant.core.classloader.ClassLoaderManager;
import io.sermant.core.common.LoggerFactory;
import io.sermant.core.plugin.config.PluginConfigManager;
import io.sermant.core.plugin.service.PluginServiceManager;
import io.sermant.core.utils.ClassUtils;
import io.sermant.registry.context.RegisterContext;
import io.sermant.registry.utils.CommonUtils;
import io.sermant.registry.utils.HostUtils;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.commons.util.InetUtils.HostInfo;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;

import java.util.HashMap;
import java.util.Optional;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

/**
 * Registration Information
 *
 * @author zhouss
 * @since 2022-06-28
 */
@Order(value = Ordered.HIGHEST_PRECEDENCE)
public class RegistrationProperties implements BeanFactoryAware {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    private static final String INET_UTILS_CLASS = "org.springframework.cloud.commons.util.InetUtils";

    private static final String INET_UTILS_BEAN_NAME = "inetUtils";

    @Value("${dubbo.application.name:${spring.application.name:application}}")
    private String serviceName;

    @Value("${server.port:8080}")
    private int port;

    @Autowired
    private Environment environment;

    private BeanFactory beanFactory;

    /**
     * Initialize
     */
    @PostConstruct
    public void init() {
        RegisterContext.INSTANCE.getClientInfo().setServiceName(serviceName);
        RegisterContext.INSTANCE.getClientInfo().setServiceId(serviceName);
        RegisterContext.INSTANCE.getClientInfo().setPort(port);
        RegisterServiceCommonConfig config = PluginConfigManager.getPluginConfig(RegisterServiceCommonConfig.class);
        RegisterContext.INSTANCE.getClientInfo().setMeta(CommonUtils.putSecureToMetaData(new HashMap<>(), config));
        RegisterContext.INSTANCE.getClientInfo().setZone(
                environment.getProperty(SpringRegistryConstants.SPRING_LOAD_BALANCER_ZONE));
        configureHostIp();

        // Start the subscription configuration
        PluginServiceManager.getPluginService(RegistryConfigSubscribeServiceImpl.class)
                .subscribeRegistryConfig(RegisterContext.INSTANCE.getClientInfo().getServiceName());
    }

    private void configureHostIp() {
        final Optional<Object> inetUtilsOptional = tryGetInetUtils();
        if (inetUtilsOptional.isPresent()) {
            final Object rawUtils = inetUtilsOptional.get();
            if (!INET_UTILS_CLASS.equals(rawUtils.getClass().getName())) {
                configureHostIpByDefault();
            } else {
                InetUtils inetUtils = (InetUtils) rawUtils;
                final HostInfo hostInfo = inetUtils.findFirstNonLoopbackHostInfo();
                RegisterContext.INSTANCE.getClientInfo().setHost(hostInfo.getHostname());
                RegisterContext.INSTANCE.getClientInfo().setIp(hostInfo.getIpAddress());
            }
        } else {
            configureHostIpByDefault();
        }
    }

    private void configureHostIpByDefault() {
        RegisterContext.INSTANCE.getClientInfo().setHost(HostUtils.getHostName());
        RegisterContext.INSTANCE.getClientInfo().setIp(HostUtils.getMachineIp());
    }

    private Optional<Object> tryGetInetUtils() {
        try {
            return Optional.of(beanFactory.getBean(INET_UTILS_BEAN_NAME));
        } catch (BeansException ex) {
            LOGGER.fine("Can not find inetUtils by name inetUtils, try find by clazz");
            return tryGetInetUtilsByClazz();
        }
    }

    private Optional<Object> tryGetInetUtilsByClazz() {
        final Optional<Class<?>> clazz = ClassUtils
                .loadClass(INET_UTILS_CLASS, ClassLoaderManager.getContextClassLoaderOrUserClassLoader(), false);
        if (!clazz.isPresent()) {
            return Optional.empty();
        }
        try {
            return Optional.of(beanFactory.getBean(InetUtils.class));
        } catch (BeansException ex) {
            LOGGER.warning("Can not find inetUtils by clazz!");
        }
        return Optional.empty();
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }
}
