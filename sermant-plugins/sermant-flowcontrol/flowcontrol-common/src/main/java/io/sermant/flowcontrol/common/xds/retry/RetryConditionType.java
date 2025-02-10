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

package io.sermant.flowcontrol.common.xds.retry;

import io.sermant.flowcontrol.common.config.CommonConst;
import io.sermant.flowcontrol.common.xds.retry.condition.ClientErrorRetryCondition;
import io.sermant.flowcontrol.common.xds.retry.condition.ConnectFailureRetryCondition;
import io.sermant.flowcontrol.common.xds.retry.condition.GatewayErrorRetryCondition;
import io.sermant.flowcontrol.common.xds.retry.condition.ResetBeforeRequestRetryCondition;
import io.sermant.flowcontrol.common.xds.retry.condition.ResetRetryCondition;
import io.sermant.flowcontrol.common.xds.retry.condition.RetriableHeadersRetryCondition;
import io.sermant.flowcontrol.common.xds.retry.condition.RetriableStatusCodesRetryCondition;
import io.sermant.flowcontrol.common.xds.retry.condition.ServerErrorRetryCondition;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Retry Condition Manager
 *
 * @author zhp
 * @since 2024-11-29
 */
public enum RetryConditionType {
    /**
     * The type of conditional judgment for server errors
     */
    SERVER_ERROR("5xx", new ServerErrorRetryCondition(),
            CommonConst.RETRY_CONDITION_BY_STATUS_AND_EXCEPTION),

    /**
     * The type of conditional judgment for client errors
     */
    CLIENT_ERROR("retriable-4xx", new ClientErrorRetryCondition(),
            CommonConst.RETRY_CONDITION_BY_RESULT),

    /**
     * The type of conditional judgment for gateway errors
     */
    GATEWAY_ERROR("gateway-error", new GatewayErrorRetryCondition(),
            CommonConst.RETRY_CONDITION_BY_RESULT),

    /**
     * The type of conditional judgment for reset errors
     */
    RESET("reset", new ResetRetryCondition(), CommonConst.RETRY_CONDITION_BY_EXCEPTION),

    /**
     * The type of conditional judgment for resetting errors before request
     */
    RESET_BEFORE_REQUEST("reset-before-request", new ResetBeforeRequestRetryCondition(),
            CommonConst.RETRY_CONDITION_BY_EXCEPTION),

    /**
     * The type of conditional judgment for connect errors
     */
    CONNECT_FAILURE("connect-failure", new ConnectFailureRetryCondition(),
            CommonConst.RETRY_CONDITION_BY_EXCEPTION),

    /**
     * The type of conditional judgment for Specify response code
     */
    RETRIABLE_STATUS_CODES("retriable-status-codes", new RetriableStatusCodesRetryCondition(),
            CommonConst.RETRY_CONDITION_BY_RESULT),

    /**
     * The type of conditional judgment for Specify response headers
     */
    RETRIABLE_HEADERS("retriable-headers", new RetriableHeadersRetryCondition(),
            CommonConst.RETRY_CONDITION_BY_RESULT);

    private static final Map<String, RetryConditionType> RETRY_CONDITION_TYPE_ENUM_MAP
            = new HashMap<>();

    /**
     * the name of retry condition
     */
    private final String conditionName;

    /**
     * the instance of implements class for retry condition
     */
    private final RetryCondition retryCondition;

    /**
     * condition Type,
     * 0: Retry condition based on the result.
     * 1: Retry condition based on the exception.
     * 2: Retry condition based on both the status code and the exception.
     */
    private final int type;

    static {
        for (RetryConditionType retryConditionType : RetryConditionType.values()) {
            RETRY_CONDITION_TYPE_ENUM_MAP.put(retryConditionType.conditionName, retryConditionType);
        }
    }

    RetryConditionType(String conditionName, RetryCondition retryCondition, int type) {
        this.conditionName = conditionName;
        this.retryCondition = retryCondition;
        this.type = type;
    }

    public String getConditionName() {
        return conditionName;
    }

    public RetryCondition getRetryCondition() {
        return retryCondition;
    }

    /**
     * get the instance of Retry condition by condition name
     *
     * @param conditionName condition name
     * @return instance of implements class for retry condition
     */
    public static Optional<RetryCondition> getRetryConditionByName(String conditionName) {
        RetryConditionType retryConditionType = RETRY_CONDITION_TYPE_ENUM_MAP.get(conditionName);
        if (retryConditionType != null) {
            return Optional.of(retryConditionType.getRetryCondition());
        }
        return Optional.empty();
    }

    /**
     * get the instance of Retry condition based on the result by condition name
     *
     * @param conditionName condition name
     * @return instance of implements class for retry condition
     */
    public static Optional<RetryCondition> getRetryConditionWithResultByName(String conditionName) {
        RetryConditionType retryConditionType = RETRY_CONDITION_TYPE_ENUM_MAP.get(conditionName);
        if (retryConditionType != null && retryConditionType.type != 1) {
            return Optional.of(retryConditionType.getRetryCondition());
        }
        return Optional.empty();
    }

    /**
     * get the instance of Retry condition based on the result by condition name
     *
     * @param conditionName condition name
     * @return instance of implements class for retry condition
     */
    public static Optional<RetryCondition> getRetryConditionWithExceptionByName(String conditionName) {
        RetryConditionType retryConditionType = RETRY_CONDITION_TYPE_ENUM_MAP.get(conditionName);
        if (retryConditionType != null && retryConditionType.type != 0) {
            return Optional.of(retryConditionType.getRetryCondition());
        }
        return Optional.empty();
    }
}
