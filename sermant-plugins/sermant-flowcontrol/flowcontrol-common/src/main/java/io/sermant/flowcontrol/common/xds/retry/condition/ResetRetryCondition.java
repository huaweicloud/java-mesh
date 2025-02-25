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

import io.sermant.core.utils.StringUtils;
import io.sermant.flowcontrol.common.exception.InvokerWrapperException;
import io.sermant.flowcontrol.common.handler.retry.Retry;
import io.sermant.flowcontrol.common.xds.retry.RetryCondition;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.Locale;
import java.util.concurrent.TimeoutException;

/**
 * Retry condition check, determine if the current error is a connect reset error, and trigger a retry if
 * it is a connect reset error
 *
 * @author zhp
 * @since 2024-11-29
 */
public class ResetRetryCondition implements RetryCondition {
    @Override
    public boolean isNeedRetry(Retry retry, Throwable ex, String statusCode, Object result) {
        if (ex == null) {
            return false;
        }
        Throwable realException = ex;
        if (ex instanceof InvokerWrapperException) {
            InvokerWrapperException invokerWrapperException = (InvokerWrapperException) ex;
            if (invokerWrapperException.getRealException() != null) {
                realException = invokerWrapperException.getRealException();
            }
        }
        String message = realException.getMessage();
        if (StringUtils.isEmpty(message)) {
            return false;
        }
        message = message.toLowerCase(Locale.ROOT);
        if (isRequestTimeoutException(realException, message)) {
            return true;
        }
        return realException instanceof IOException
                && (message.contains("connection reset") || message.toLowerCase(Locale.ROOT).contains("broken pipe"));
    }

    private static boolean isRequestTimeoutException(Throwable ex, String message) {
        return (ex instanceof InterruptedIOException || ex instanceof TimeoutException)
                && (message.contains("read timed out") || message.contains("timeout"));
    }
}
