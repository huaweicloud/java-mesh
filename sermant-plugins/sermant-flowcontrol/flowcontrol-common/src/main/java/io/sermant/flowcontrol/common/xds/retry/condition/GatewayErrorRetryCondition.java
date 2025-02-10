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

import io.sermant.flowcontrol.common.handler.retry.Retry;
import io.sermant.flowcontrol.common.util.StringUtils;
import io.sermant.flowcontrol.common.xds.retry.RetryCondition;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Retry condition check, determine if the current error is a gateway error, and trigger a retry if it is a gateway
 * error
 *
 * @author zhp
 * @since 2024-11-29
 */
public class GatewayErrorRetryCondition implements RetryCondition {
    private static final Set<String> GATE_WAY_FAILURE_CODE = new HashSet<>(Arrays.asList("502", "503", "504"));

    @Override
    public boolean isNeedRetry(Retry retry, Throwable ex, String statusCode, Object result) {
        return !StringUtils.isEmpty(statusCode) && GATE_WAY_FAILURE_CODE.contains(statusCode);
    }
}
