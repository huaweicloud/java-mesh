/*
 * Copyright (C) 2022-2022 Huawei Technologies Co., Ltd. All rights reserved.
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

package io.sermant.discovery.service.lb.discovery.zk.listen;

import io.sermant.core.classloader.ClassLoaderManager;
import io.sermant.core.common.LoggerFactory;
import io.sermant.core.plugin.config.PluginConfigManager;
import io.sermant.core.plugin.service.PluginServiceManager;
import io.sermant.discovery.config.LbConfig;
import io.sermant.discovery.config.RegisterType;
import io.sermant.discovery.service.lb.discovery.InstanceChangeListener;
import io.sermant.discovery.service.lb.discovery.InstanceChangeListener.EventType;
import io.sermant.discovery.service.lb.discovery.InstanceListenable;
import io.sermant.discovery.service.lb.discovery.zk.ZkClient;
import io.sermant.discovery.service.lb.discovery.zk.ZkDiscoveryClient;
import io.sermant.discovery.service.lb.discovery.zk.ZkDiscoveryClient.ZkInstanceSerializer;
import io.sermant.discovery.service.lb.discovery.zk.ZkInstanceHelper;

import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent.Type;
import org.apache.curator.x.discovery.ServiceInstance;
import org.springframework.cloud.zookeeper.discovery.ZookeeperInstance;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ZK snooping implementation
 *
 * @author zhouss
 * @since 2022-10-12
 */
public class ZkInstanceListenable implements InstanceListenable {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    /**
     * When the service is updated, remove {@link LbConfig#getZkBasePath()}, and then the path is cut by "/", and only
     * when it is divided into 4 parts is the correct service instance information node path
     */
    private static final int VALID_LEN = 2;

    private static final String SEPARATOR = "/";

    private final Map<String, InstanceChangeListener> listenerCache = new ConcurrentHashMap<>();

    private final ZkInstanceSerializer<ZookeeperInstance> serializer =
            new ZkInstanceSerializer<>(ZookeeperInstance.class);

    private final AtomicBoolean isInitialized = new AtomicBoolean();

    private ZkClient zkClient;

    private final LbConfig lbConfig;

    private final Predicate<ServiceInstance<ZookeeperInstance>> predicate;

    private volatile TreeCache childrenCache;

    /**
     * Constructor
     */
    public ZkInstanceListenable() {
        this.lbConfig = PluginConfigManager.getPluginConfig(LbConfig.class);
        this.predicate = ZkInstanceHelper.predicate(lbConfig.isOnlyCurRegisterInstances());
    }

    @Override
    public void init() {
    }

    private ZkClient getZkClient() {
        if (zkClient != null) {
            return zkClient;
        }
        zkClient = PluginServiceManager.getPluginService(ZkClient.class);
        return zkClient;
    }

    @Override
    public void tryAdd(String serviceName, InstanceChangeListener listener) {
        checkState();
        listenerCache.putIfAbsent(serviceName, listener);
    }

    private void checkState() {
        if (isInitialized.compareAndSet(false, true)) {
            this.initPathCache();
        }
    }

    private void initPathCache() {
        final TreeCache pathCache = getPathCache();
        pathCache.getListenable().addListener((client, event) -> {
            final Type type = event.getType();
            if (!isTargetEvent(type) || event.getData() == null) {
                return;
            }
            final String path = event.getData().getPath();
            final Optional<String> serviceName = resolveServiceName(path);
            if (!serviceName.isPresent()) {
                return;
            }
            final InstanceChangeListener listener = listenerCache.get(serviceName.get());
            if (listener == null) {
                return;
            }
            final Optional<io.sermant.discovery.entity.ServiceInstance> deserialize = deserialize(event.getData());
            deserialize.ifPresent(serviceInstance -> listener.notify(formatEventType(type), serviceInstance));
        });
    }

    private Optional<String> resolveServiceName(String path) {
        if (path == null || path.length() <= lbConfig.getZkBasePath().length()) {
            return Optional.empty();
        }
        String serviceNodePath = path.substring(lbConfig.getZkBasePath().length());
        if (serviceNodePath.startsWith(SEPARATOR)) {
            serviceNodePath = serviceNodePath.substring(SEPARATOR.length());
        }
        final String[] parts = serviceNodePath.split(SEPARATOR);
        if (parts.length != VALID_LEN) {
            return Optional.empty();
        }
        return Optional.of(parts[0]);
    }

    private TreeCache getPathCache() {
        if (childrenCache != null) {
            return childrenCache;
        }
        synchronized (this) {
            if (childrenCache == null) {
                childrenCache = new TreeCache(getZkClient().getClient(), lbConfig.getZkBasePath());
                try {
                    childrenCache.start();
                } catch (Exception exception) {
                    LOGGER.log(Level.WARNING, "Can not start path cache!", exception);
                }
            }
        }
        return childrenCache;
    }

    private EventType formatEventType(Type type) {
        if (type == Type.NODE_REMOVED) {
            return EventType.DELETED;
        } else if (type == Type.NODE_UPDATED) {
            return EventType.UPDATED;
        } else {
            return EventType.ADDED;
        }
    }

    private Optional<io.sermant.discovery.entity.ServiceInstance> deserialize(ChildData childData) {
        final ClassLoader contextClassLoader = ClassLoaderManager.getContextClassLoaderOrUserClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(ZkDiscoveryClient.class.getClassLoader());
            final ServiceInstance<ZookeeperInstance> serviceInstance = serializer.deserialize(childData.getData());
            if (!predicate.test(serviceInstance)) {
                return Optional.empty();
            }
            return Optional.of(ZkInstanceHelper.convert2Instance(serviceInstance));
        } catch (Exception exception) {
            LOGGER.warning(String.format("Can not deserialize instance, may be it is not valid, path is [%s]",
                    childData.getPath()));
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
        return Optional.empty();
    }

    private boolean isTargetEvent(Type type) {
        return type == Type.NODE_ADDED || type == Type.NODE_REMOVED || type == Type.NODE_UPDATED;
    }

    @Override
    public void close() {
        if (this.childrenCache != null) {
            this.childrenCache.close();
        }
        listenerCache.clear();
    }

    @Override
    public RegisterType registerType() {
        return RegisterType.ZOOKEEPER;
    }
}
