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
import io.sermant.flowcontrol.common.xds.retry.RetryConditionType;

/**
 * Retry condition check, determine if the current error is a server error, and trigger a retry if it is.
 *
 * @author zhp
 * @since 2024-11-29
 */
public class ServerErrorRetryCondition implements RetryCondition {
    private static final int MIN_5XX_FAILURE = 500;

    private static final int MAX_5XX_FAILURE = 599;

    @Override
    public boolean isNeedRetry(Retry retry, Throwable ex, String statusCode, Object result) {
        if (StringUtils.isEmpty(statusCode) && ex == null) {
            return false;
        }
        if (!StringUtils.isEmpty(statusCode)) {
            int code = Integer.parseInt(statusCode);
            return code >= MIN_5XX_FAILURE && code <= MAX_5XX_FAILURE;
        }
        RetryCondition connectFailure = RetryConditionType.CONNECT_FAILURE.getRetryCondition();
        RetryCondition resetErrorCondition = RetryConditionType.RESET.getRetryCondition();
        return resetErrorCondition.isNeedRetry(retry, ex, statusCode, result)
                || connectFailure.isNeedRetry(retry, ex, statusCode, result);
    }
}
