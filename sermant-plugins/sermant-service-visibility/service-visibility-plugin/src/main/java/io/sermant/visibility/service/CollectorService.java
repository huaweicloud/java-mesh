/*
 * Copyright (C) 2021-2022 Huawei Technologies Co., Ltd. All rights reserved.
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

package io.sermant.visibility.service;

import io.sermant.core.plugin.service.PluginService;
import io.sermant.visibility.entity.ServerInfo;

/**
 * Information collection and processing services
 *
 * @author zhp
 * @since 2022-12-05
 */
public interface CollectorService extends PluginService {
    /**
     * Collect information and send it
     *
     * @param serverInfo Collect information
     */
    void sendServerInfo(ServerInfo serverInfo);

    /**
     * Service reconnection processing
     */
    void reconnectHandler();
}
