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

package io.sermant.core.plugin.subscribe.processor;

import io.sermant.core.operation.OperationManager;
import io.sermant.core.operation.converter.api.YamlConverter;
import io.sermant.core.service.dynamicconfig.common.DynamicConfigEvent;
import io.sermant.core.service.dynamicconfig.common.DynamicConfigEventType;
import io.sermant.core.service.dynamicconfig.common.DynamicConfigListener;
import io.sermant.core.utils.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

/**
 * Multiple label scenarios, processed in a centralized manner based on the specified priority, and the configuration
 * sequence is overwritten based on the order
 *
 * @author zhouss
 * @since 2022-04-22
 */
public class ConfigOrderIntegratedProcessor implements ConfigProcessor {
    /**
     * map initialization capacity
     */
    private static final int CAP_SIZE = 8;

    /**
     * origin listener
     */
    private final DynamicConfigListener originListener;

    /**
     * ConfigDataHolder list
     */
    private List<ConfigDataHolder> dataHolders;

    private final YamlConverter yamlConverter = OperationManager.getOperation(YamlConverter.class);

    /**
     * constructor
     *
     * @param listener origin listener
     */
    public ConfigOrderIntegratedProcessor(DynamicConfigListener listener) {
        this.originListener = listener;
    }

    /**
     * Add a data holder
     *
     * @param dataHolder data holder
     */
    @Override
    public final void addHolder(ConfigDataHolder dataHolder) {
        if (this.dataHolders == null) {
            this.dataHolders = new ArrayList<>(CAP_SIZE);
        }
        this.dataHolders.add(dataHolder);
        Collections.sort(dataHolders);
    }

    @Override
    public final void process(String rawGroup, DynamicConfigEvent event) {
        final Optional<ConfigDataHolder> targetHolder = findTargetHolder(rawGroup);
        synchronized (this) {
            originListener.process(targetHolder.map(dataHolder -> rebuildEvent(dataHolder, event)).orElse(event));
        }
    }

    /**
     * rebuild event
     *
     * @param targetHolder Target data holder
     * @param originEvent origin event
     * @return DynamicConfigEvent
     */
    private DynamicConfigEvent rebuildEvent(ConfigDataHolder targetHolder, DynamicConfigEvent originEvent) {
        if (updateHolder(targetHolder, originEvent)) {
            return new OrderConfigEvent(originEvent.getKey(), originEvent.getGroup(),
                    yamlConverter.dump(buildOrderData(originEvent)), originEvent.getEventType(), buildOrderData());
        }
        return originEvent;
    }

    /**
     * Build data that is overlaid by order
     *
     * @return orderData
     */
    private Map<String, Object> buildOrderData() {
        final Map<String, Object> result = new HashMap<>(CAP_SIZE);
        for (ConfigDataHolder dataHolder : dataHolders) {
            for (Map<String, Object> data : dataHolder.getHolder().values()) {
                handleConfig(result, data);
            }
        }
        return result;
    }

    private Map<String, Object> buildOrderData(DynamicConfigEvent originEvent) {
        final Map<String, Object> result = new HashMap<>(CAP_SIZE);
        dataHolders.forEach(dataHolder -> {
            final Map<String, Object> curContent = dataHolder.getHolder().get(originEvent.getKey());
            if (curContent != null) {
                handleConfig(result, curContent);
            }
        });
        return result;
    }

    private void handleConfig(Map<String, Object> result, Map<String, Object> config) {
        if (config == null || config.isEmpty()) {
            return;
        }
        for (Entry<String, Object> entry : config.entrySet()) {
            String key = entry.getKey();
            final Object value = entry.getValue();
            if (value instanceof Map) {
                Object resultValue = result.get(key);
                Map<String, Object> map = resultValue instanceof Map ? (Map<String, Object>) resultValue
                        : new HashMap<>();
                handleConfig(map, (Map<String, Object>) value);
                result.put(key, map);
            } else if (value instanceof Collection) {
                result.put(key, value);
            } else {
                // Other types are reserved directly
                result.put(key, value == null ? "" : value);
            }
        }
    }

    private boolean updateHolder(ConfigDataHolder targetHolder, DynamicConfigEvent originEvent) {
        final Map<String, Object> olderDataMap =
                targetHolder.getHolder().getOrDefault(originEvent.getKey(), new HashMap<>(CAP_SIZE));
        olderDataMap.clear();
        if (originEvent.getEventType() != DynamicConfigEventType.DELETE) {
            Optional<Object> convert = yamlConverter.convert(originEvent.getContent(), Object.class);
            if (!convert.isPresent()) {
                return false;
            }
            Object obj = convert.get();
            if (obj instanceof Map) {
                olderDataMap.putAll((Map<String, Object>) obj);
            } else {
                olderDataMap.put(originEvent.getKey(), obj);
            }
        }
        targetHolder.getHolder().put(originEvent.getKey(), olderDataMap);
        return true;
    }

    private Optional<ConfigDataHolder> findTargetHolder(String group) {
        return dataHolders.stream().filter(dataHolder -> StringUtils.equals(dataHolder.getGroup(), group)).findAny();
    }
}
