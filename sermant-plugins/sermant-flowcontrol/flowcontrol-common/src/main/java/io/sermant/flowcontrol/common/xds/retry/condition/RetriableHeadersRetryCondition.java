/*
 * Copyright (C) 2024-2024 Sermant Authors. All rights reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package io.sermant.flowcontrol.common.xds.retry.condition;

import io.sermant.core.plugin.config.PluginConfigManager;
import io.sermant.core.utils.CollectionUtils;
import io.sermant.core.utils.StringUtils;
import io.sermant.flowcontrol.common.config.CommonConst;
import io.sermant.flowcontrol.common.config.XdsFlowControlConfig;
import io.sermant.flowcontrol.common.handler.retry.Retry;
import io.sermant.flowcontrol.common.xds.retry.RetryCondition;

import java.util.Optional;
import java.util.Set;

/**
 * Retry condition check, determine if the response contains the specified response header, and trigger a retry
 * if it does.
 *
 * @author zhp
 * @since 2024-11-29
 */
public class RetriableHeadersRetryCondition implements RetryCondition {
    private static final XdsFlowControlConfig CONFIG = PluginConfigManager.getPluginConfig(XdsFlowControlConfig.class);

    @Override
    public boolean isNeedRetry(Retry retry, Throwable ex, String statusCode, Object result) {
        if (CollectionUtils.isEmpty(CONFIG.getRetryHeaderNames())) {
            return false;
        }
        int code = StringUtils.isEmpty(statusCode) ? CommonConst.DEFAULT_RESPONSE_CODE : Integer.parseInt(statusCode);
        if (code >= CommonConst.MIN_SUCCESS_STATUS_CODE && code <= CommonConst.MAX_SUCCESS_STATUS_CODE) {
            return false;
        }
        Optional<Set<String>> headerNames = retry.getHeaderNames(result);
        if (!headerNames.isPresent()) {
            return false;
        }
        for (String name : headerNames.get()) {
            if (CONFIG.getRetryHeaderNames().contains(name)) {
                return true;
            }
        }
        return false;
    }
}
