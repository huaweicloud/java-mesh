/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sermant.dubbo.registry.utils;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;

import io.sermant.core.classloader.ClassLoaderManager;
import io.sermant.core.common.LoggerFactory;
import io.sermant.core.utils.StringUtils;
import io.sermant.registry.config.NacosRegisterConfig;
import io.sermant.registry.config.PropertyKeyConst;
import io.sermant.registry.config.RegisterServiceCommonConfig;

import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Nacos registers for the Naming service
 *
 * @since 2022-10-25
 */
public class NamingServiceUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger();
    private static final String BACKUP_KEY = "backup";

    private NamingServiceUtils() {
    }

    /**
     * Build a naming service
     *
     * @param parameters url
     * @param registerConfig registerConfig
     * @param commonConfig Common configuration
     * @return Naming services
     * @throws IllegalStateException Creating NamingService exception
     */
    public static NamingService buildNamingService(Map<String, String> parameters, NacosRegisterConfig registerConfig,
        RegisterServiceCommonConfig commonConfig) {
        Properties nacosProperties = buildNacosProperties(parameters, registerConfig, commonConfig);
        ClassLoader tempClassLoader = ClassLoaderManager.getContextClassLoaderOrUserClassLoader();
        Thread.currentThread().setContextClassLoader(NamingServiceUtils.class.getClassLoader());
        try {
            return NacosFactory.createNamingService(nacosProperties);
        } catch (NacosException e) {
            LOGGER.log(Level.SEVERE, String.format(Locale.ENGLISH, "create namingService failed: {%s}",
                    e.getErrMsg()), e);
            throw new IllegalStateException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(tempClassLoader);
        }
    }

    private static Properties buildNacosProperties(Map<String, String> parameters, NacosRegisterConfig registerConfig,
        RegisterServiceCommonConfig commonConfig) {
        Properties properties = new Properties();
        setServerAddr(parameters.get(BACKUP_KEY), properties, commonConfig);
        setProperties(parameters, properties, registerConfig);
        return properties;
    }

    private static void setServerAddr(String backup, Properties properties, RegisterServiceCommonConfig commonConfig) {
        StringBuilder serverAddrBuilder = new StringBuilder();
        serverAddrBuilder.append(commonConfig.getAddress());
        if (!StringUtils.isEmpty(backup)) {
            serverAddrBuilder.append(',').append(backup);
        }
        String serverAddr = serverAddrBuilder.toString();
        properties.put(PropertyKeyConst.SERVER_ADDR, serverAddr);
    }

    private static void setProperties(Map<String, String> parameters, Properties properties,
        NacosRegisterConfig registerConfig) {
        properties.putAll(parameters);
        if (!StringUtils.isEmpty(registerConfig.getUsername())) {
            properties.put(PropertyKeyConst.USERNAME, registerConfig.getUsername());
        }
        if (!StringUtils.isEmpty(registerConfig.getPassword())) {
            properties.put(PropertyKeyConst.PASSWORD, registerConfig.getPassword());
        }
        if (!StringUtils.isEmpty(registerConfig.getLogName())) {
            properties.put(PropertyKeyConst.NACOS_NAMING_LOG_NAME, registerConfig.getLogName());
        }
        if (!StringUtils.isEmpty(registerConfig.getNamingLoadCacheAtStart())) {
            properties.put(PropertyKeyConst.NAMING_LOAD_CACHE_AT_START, registerConfig.getNamingLoadCacheAtStart());
        }
    }
}
