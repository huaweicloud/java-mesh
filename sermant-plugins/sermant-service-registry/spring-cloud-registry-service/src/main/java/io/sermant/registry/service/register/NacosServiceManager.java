/*
 * Copyright 2013-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sermant.registry.service.register;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingMaintainService;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.client.naming.NacosNamingMaintainService;
import com.alibaba.nacos.client.naming.NacosNamingService;

import io.sermant.core.classloader.ClassLoaderManager;
import io.sermant.core.plugin.config.PluginConfigManager;
import io.sermant.registry.config.ConfigConstants;
import io.sermant.registry.config.NacosRegisterConfig;
import io.sermant.registry.config.RegisterServiceCommonConfig;
import io.sermant.registry.context.RegisterContext;

import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * NACOS Registration Service Manager
 *
 * @since 2022-10-20
 */
public class NacosServiceManager {
    private static final String SECURE_KEY = "secure";

    private volatile NamingService namingService;

    private volatile NamingMaintainService namingMaintainService;

    private final NacosRegisterConfig nacosRegisterConfig;

    private final RegisterServiceCommonConfig commonConfig;

    /**
     * Constructor
     */
    public NacosServiceManager() {
        nacosRegisterConfig = PluginConfigManager.getPluginConfig(NacosRegisterConfig.class);
        commonConfig = PluginConfigManager.getPluginConfig(RegisterServiceCommonConfig.class);
    }

    /**
     * Get registration services
     *
     * @return Naming Service
     * @throws NacosException nacos exception
     */
    public NamingService getNamingService() throws NacosException {
        if (Objects.isNull(this.namingService)) {
            buildNamingService(nacosRegisterConfig.getNacosProperties());
        }
        return namingService;
    }

    /**
     * Obtain the namingMaintain service
     *
     * @return namingMaintain service
     * @throws NacosException nacos exception
     */
    public NamingMaintainService getNamingMaintainService() throws NacosException {
        if (Objects.isNull(namingMaintainService)) {
            buildNamingMaintainService(nacosRegisterConfig.getNacosProperties());
        }
        return namingMaintainService;
    }

    private void buildNamingMaintainService(Properties properties) throws NacosException {
        if (Objects.isNull(namingMaintainService)) {
            synchronized (NacosServiceManager.class) {
                if (Objects.isNull(namingMaintainService)) {
                    namingMaintainService = createNamingMaintainService(properties);
                }
            }
        }
    }

    private void buildNamingService(Properties properties) throws NacosException {
        if (Objects.isNull(namingService)) {
            synchronized (NacosServiceManager.class) {
                if (Objects.isNull(namingService)) {
                    namingService = createNewNamingService(properties);
                }
            }
        }
    }

    private NamingService createNewNamingService(Properties properties) throws NacosException {
        ClassLoader tempClassLoader = ClassLoaderManager.getContextClassLoaderOrUserClassLoader();
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
        try {
            return new NacosNamingService(properties);
        } finally {
            Thread.currentThread().setContextClassLoader(tempClassLoader);
        }
    }

    private NamingMaintainService createNamingMaintainService(Properties properties) throws NacosException {
        ClassLoader tempClassLoader = ClassLoaderManager.getContextClassLoaderOrUserClassLoader();
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
        try {
            return new NacosNamingMaintainService(properties);
        } finally {
            Thread.currentThread().setContextClassLoader(tempClassLoader);
        }
    }

    /**
     * Build a Nacos registration instance
     *
     * @return 实例
     */
    public Instance buildNacosInstanceFromRegistration() {
        Instance instance = new Instance();
        instance.setIp(RegisterContext.INSTANCE.getClientInfo().getIp());
        instance.setPort(RegisterContext.INSTANCE.getClientInfo().getPort());
        instance.setWeight(nacosRegisterConfig.getWeight());
        instance.setClusterName(nacosRegisterConfig.getClusterName());
        instance.setEnabled(nacosRegisterConfig.isInstanceEnabled());
        Map<String, String> metadata = RegisterContext.INSTANCE.getClientInfo().getMeta();
        if (!metadata.containsKey(ConfigConstants.SECURE)) {
            metadata.put(ConfigConstants.SECURE, String.valueOf(commonConfig.isSecure()));
        }
        instance.setMetadata(metadata);
        instance.setEphemeral(nacosRegisterConfig.isEphemeral());
        return instance;
    }
}
