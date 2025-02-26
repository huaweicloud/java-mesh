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

import io.sermant.flowcontrol.common.exception.InvokerWrapperException;
import io.sermant.flowcontrol.common.handler.retry.Retry;
import io.sermant.flowcontrol.common.util.StringUtils;
import io.sermant.flowcontrol.common.xds.retry.RetryCondition;

import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.util.Locale;
import java.util.concurrent.TimeoutException;

/**
 * Retry condition check, determine if the current error is a connect error, and trigger a retry if it is a connect
 * error
 *
 * @author zhp
 * @since 2024-11-29
 */
public class ConnectFailureRetryCondition implements RetryCondition {
    @Override
    public boolean isNeedRetry(Retry retry, Throwable ex, String statusCode, Object result) {
        if (ex == null) {
            return false;
        }
        if (!(ex instanceof InvokerWrapperException)) {
            return isConnectErrorException(ex);
        }
        InvokerWrapperException invokerWrapperException = (InvokerWrapperException) ex;
        if (invokerWrapperException.getRealException() != null) {
            return isConnectErrorException(invokerWrapperException.getRealException());
        }
        return false;
    }

    private boolean isConnectErrorException(Throwable ex) {
        if (isRequestTimeoutException(ex)) {
            return false;
        }
        return ex instanceof InterruptedIOException || ex instanceof ConnectException || ex instanceof TimeoutException
                || ex instanceof NoRouteToHostException;
    }

    private boolean isRequestTimeoutException(Throwable ex) {
        String message = ex.getMessage();
        if (StringUtils.isEmpty(message)) {
            return false;
        }
        message = message.toLowerCase(Locale.ROOT);
        return (ex instanceof InterruptedIOException || ex instanceof TimeoutException)
                && (message.contains("read timed out") || message.contains("timeout"));
    }
}
