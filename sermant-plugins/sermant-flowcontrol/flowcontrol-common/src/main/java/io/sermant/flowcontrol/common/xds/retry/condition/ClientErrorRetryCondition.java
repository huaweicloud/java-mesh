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

/**
 * Retry condition check, determine if the current error is a client-side error, and trigger a retry if it is a
 * client-side error
 *
 * @author zhp
 * @since 2024-11-29
 */
public class ClientErrorRetryCondition implements RetryCondition {
    private static final int MIN_4XX_FAILURE = 400;

    private static final int MAX_4XX_FAILURE = 499;

    @Override
    public boolean isNeedRetry(Retry retry, Throwable ex, String statusCode, Object result) {
        if (StringUtils.isEmpty(statusCode)) {
            return false;
        }
        int code = Integer.parseInt(statusCode);
        return code >= MIN_4XX_FAILURE && code <= MAX_4XX_FAILURE;
    }
}
