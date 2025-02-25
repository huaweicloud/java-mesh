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

package io.sermant.registry.service.register;

import io.sermant.registry.entity.MicroServiceInstance;

import java.util.Map;

/**
 * NACOS Service Information
 *
 * @author chengyouling
 * @since 2022-10-24
 */
public class NacosServiceInstance implements MicroServiceInstance {
    private String serviceId;

    private String instanceId;

    private String host;

    private int port;

    private boolean secure;

    private Map<String, String> metadata;

    @Override
    public String getServiceName() {
        return serviceId;
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getIp() {
        return host;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public String getServiceId() {
        return serviceId;
    }

    @Override
    public String getInstanceId() {
        return instanceId;
    }

    @Override
    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    @Override
    public boolean isSecure() {
        return secure;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }
}
