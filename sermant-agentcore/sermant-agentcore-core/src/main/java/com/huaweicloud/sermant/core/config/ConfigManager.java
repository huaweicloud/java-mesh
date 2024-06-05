/*
 * Copyright (C) 2021-2021 Huawei Technologies Co., Ltd. All rights reserved.
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

package com.huaweicloud.sermant.core.config;

import com.huaweicloud.sermant.core.classloader.ClassLoaderManager;
import com.huaweicloud.sermant.core.common.BootArgsIndexer;
import com.huaweicloud.sermant.core.common.LoggerFactory;
import com.huaweicloud.sermant.core.config.common.BaseConfig;
import com.huaweicloud.sermant.core.config.strategy.LoadConfigStrategy;
import com.huaweicloud.sermant.core.config.utils.ConfigKeyUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 配置储存器
 *
 * @author HapThorin
 * @version 1.0.0
 * @since 2021-08-26
 */
public abstract class ConfigManager {
    /**
     * 日志
     */
    private static final Logger LOGGER = LoggerFactory.getLogger();

    /**
     * 配置对象集合，键为配置对象的实现类Class，值为加载完毕的配置对象
     * <p>通过{@link #getConfig(Class)}方法获取配置对象
     */
    private static final Map<String, BaseConfig> CONFIG_MAP = new HashMap<>();

    private static final List<LoadConfigStrategy> LOAD_CONFIG_STRATEGIES = new ArrayList<>();

    private static Map<String, Object> argsMap;

    /**
     * 关闭配置管理器
     */
    public static void shutdown() {
        CONFIG_MAP.clear();
        LOAD_CONFIG_STRATEGIES.clear();
    }

    /**
     * 通过配置对象类型获取配置对象
     *
     * @param cls 配置对象类型
     * @param <R> 配置对象泛型
     * @return 配置对象
     */
    public static <R extends BaseConfig> R getConfig(Class<R> cls) {
        return (R) CONFIG_MAP.get(ConfigKeyUtil.getTypeKey(cls));
    }

    /**
     * 执行初始化，主要包含以下步骤：
     * <pre>
     *     1.处理启动配置参数
     *     2.获取加载配置策略
     *     3.加载配置文件
     *     4.查找所有配置对象
     *     5.加载所有配置对象
     *     6.将所有加载完毕的配置对象保留于{@code CONFIG_MAP}中
     * </pre>
     * <p>当加载配置策略不存在时，使用默认的加载策略，将不进行任何配置加载
     * <p>当配置文件不存在时，仅将{@code agentArgs}中的内容处理为配置信息承载对象
     * <p>当部分配置对象封装失败时，将不影响他们保存于{@code CONFIG_MAP}中，也不影响其他配置对象的封装
     *
     * @param args 启动配置参数
     */
    public static synchronized void initialize(Map<String, Object> args) {
        argsMap = args;
        for (LoadConfigStrategy<?> strategy : ServiceLoader.load(LoadConfigStrategy.class,
                ClassLoaderManager.getFrameworkClassLoader())) {
            LOAD_CONFIG_STRATEGIES.add(strategy);
        }
        loadConfig(BootArgsIndexer.getConfigFile(), ClassLoaderManager.getSermantClassLoader());
    }

    /**
     * 加载配置文件，将配置信息读取到配置对象中
     *
     * @param configFile 配置文件
     * @param classLoader 类加载器，该参数决定从哪个classLoader中进行api操作
     */
    protected static void loadConfig(File configFile, ClassLoader classLoader) {
        if (configFile.exists() && configFile.isFile()) {
            doLoadConfig(configFile, classLoader);
        } else {
            loadDefaultConfig(classLoader);
        }
    }

    /**
     * 加载默认配置
     *
     * @param classLoader 类加载器，该参数决定从哪个classLoader中进行api操作
     */
    private static synchronized void loadDefaultConfig(ClassLoader classLoader) {
        foreachConfig(new ConfigConsumer() {
            @Override
            public void accept(BaseConfig config) {
                final String typeKey = ConfigKeyUtil.getTypeKey(config.getClass());
                if (!CONFIG_MAP.containsKey(typeKey)) {
                    CONFIG_MAP.put(typeKey, config);
                }
            }
        }, classLoader);
    }

    /**
     * 配置执行从配置文件中加载
     *
     * @param configFile 配置文件
     * @param classLoader 类加载器，当前配置加载策略api在agentcore-implement包中，所以使用FrameworkClassLoader加载
     */
    private static synchronized void doLoadConfig(File configFile,
            ClassLoader classLoader) {
        foreachConfig(config -> {
            final String typeKey = ConfigKeyUtil.getTypeKey(config.getClass());
            final BaseConfig retainedConfig = CONFIG_MAP.get(typeKey);
            if (retainedConfig == null) {
                CONFIG_MAP.put(typeKey, doLoad(configFile, config, false));
            } else if (retainedConfig.getClass() == config.getClass()) {
                LOGGER.fine(String.format(Locale.ROOT, "Skip load config [%s] repeatedly. ",
                        config.getClass().getName()));
            } else {
                LOGGER.warning(String.format(Locale.ROOT, "Type key of %s is %s, same as %s's. ",
                        config.getClass().getName(), typeKey, retainedConfig.getClass().getName()));
            }
        }, classLoader);
    }

    /**
     * 加载配置逻辑
     *
     * @param configFile 配置文件
     * @param baseConfig 配置类
     * @param isDynamic is the config loaded dynamically
     * @return 加载后的配置类
     */
    public static BaseConfig doLoad(File configFile, BaseConfig baseConfig, boolean isDynamic) {
        // 通过FrameworkClassLoader 获取配置加载策略
        final LoadConfigStrategy<?> loadConfigStrategy = getLoadConfigStrategy(configFile,
                ClassLoaderManager.getFrameworkClassLoader());
        final Object holder = loadConfigStrategy.getConfigHolder(configFile, argsMap);
        return ((LoadConfigStrategy) loadConfigStrategy).loadConfig(holder, baseConfig, isDynamic);
    }

    /**
     * 通过spi的方式获取加载配置策略
     * <p>需要在{@code META-INF/services}目录中添加加载配置策略{@link LoadConfigStrategy}文件，并键入实现
     * <p>如果声明多个实现，仅第一个有效
     * <p>如果未声明任何实现，使用默认的加载配置策略{@link LoadConfigStrategy.DefaultLoadConfigStrategy}
     * <p>该默认策略不会进行任何业务操作
     *
     * @param configFile 配置文件
     * @param classLoader 用于查找加载策略的类加载器，允许在类加载中添加新的配置加载策略
     * @return 加载配置策略
     */
    private static LoadConfigStrategy<?> getLoadConfigStrategy(File configFile, ClassLoader classLoader) {
        for (LoadConfigStrategy<?> strategy : LOAD_CONFIG_STRATEGIES) {
            if (strategy.canLoad(configFile)) {
                return strategy;
            }
        }
        if (classLoader != ClassLoader.getSystemClassLoader()) {
            for (LoadConfigStrategy<?> strategy : ServiceLoader.load(LoadConfigStrategy.class, classLoader)) {
                if (strategy.canLoad(configFile)) {
                    return strategy;
                }
            }
        }
        LOGGER.log(Level.WARNING,
                String.format(Locale.ROOT, "Missing implement of [%s], use [%s].", LoadConfigStrategy.class.getName(),
                        LoadConfigStrategy.DefaultLoadConfigStrategy.class.getName()));
        return new LoadConfigStrategy.DefaultLoadConfigStrategy();
    }

    /**
     * 遍历所有通过spi方式声明的配置对象
     * <p>需要在{@code META-INF/services}目录中添加配置基类{@link BaseConfig}文件，并添加实现
     * <p>文件中声明的所有实现都将会进行遍历，每一个实现类都会通过spi获取实例，然后调用{@code configConsumer}进行消费
     *
     * @param configConsumer 配置处理方法
     * @param classLoader 类加载器
     */
    private static void foreachConfig(ConfigConsumer configConsumer,
            ClassLoader classLoader) {
        for (BaseConfig config : ServiceLoader.load((Class<? extends BaseConfig>) BaseConfig.class, classLoader)) {
            configConsumer.accept(config);
        }
    }

    /**
     * 配置对象消费者
     *
     * @since 2021-12-31
     */
    public interface ConfigConsumer {
        /**
         * 处理BaseConfig
         *
         * @param config BaseConfig
         */
        void accept(BaseConfig config);
    }
}
