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

import io.sermant.flowcontrol.common.xds.retry.condition.ResetBeforeRequestRetryCondition;
import io.sermant.flowcontrol.common.xds.retry.condition.ClientErrorRetryCondition;
import io.sermant.flowcontrol.common.xds.retry.condition.ConnectFailureRetryCondition;
import io.sermant.flowcontrol.common.xds.retry.condition.GatewayErrorRetryCondition;
import io.sermant.flowcontrol.common.xds.retry.condition.ResetRetryCondition;
import io.sermant.flowcontrol.common.xds.retry.condition.ServerErrorRetryCondition;
import io.sermant.flowcontrol.common.xds.retry.condition.RetriableHeadersRetryCondition;
import io.sermant.flowcontrol.common.xds.retry.condition.RetriableStatusCodesRetryCondition;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertTrue;

/**
 * RetryConditionManagerTest
 *
 * @author zhp
 * @since 2024-11-29
 */
public class RetryConditionManagerTest {
    @Test
    public void testRetryCondition() {
        Optional<RetryCondition> result = RetryConditionType.getRetryConditionByName(
                RetryConditionType.SERVER_ERROR.getConditionName());
        assertTrue(result.isPresent() && result.get() instanceof ServerErrorRetryCondition);
        result = RetryConditionType.getRetryConditionByName(
                RetryConditionType.CLIENT_ERROR.getConditionName());
        assertTrue(result.isPresent() && result.get() instanceof ClientErrorRetryCondition);
        result = RetryConditionType.getRetryConditionByName(
                RetryConditionType.CONNECT_FAILURE.getConditionName());
        assertTrue(result.isPresent() && result.get() instanceof ConnectFailureRetryCondition);
        result = RetryConditionType.getRetryConditionByName(
                RetryConditionType.RESET_BEFORE_REQUEST.getConditionName());
        assertTrue(result.isPresent() && result.get() instanceof ResetBeforeRequestRetryCondition);
        result = RetryConditionType.getRetryConditionByName(
                RetryConditionType.GATEWAY_ERROR.getConditionName());
        assertTrue(result.isPresent() && result.get() instanceof GatewayErrorRetryCondition);
        result = RetryConditionType.getRetryConditionByName(
                RetryConditionType.RESET.getConditionName());
        assertTrue(result.isPresent() && result.get() instanceof ResetRetryCondition);
        result = RetryConditionType.getRetryConditionByName(
                RetryConditionType.RETRIABLE_HEADERS.getConditionName());
        assertTrue(result.isPresent() && result.get() instanceof RetriableHeadersRetryCondition);
        result = RetryConditionType.getRetryConditionByName(
                RetryConditionType.RETRIABLE_STATUS_CODES.getConditionName());
        assertTrue(result.isPresent() && result.get() instanceof RetriableStatusCodesRetryCondition);
    }
}
