/*
 * Copyright (C) 2025-2025 Sermant Authors. All rights reserved.
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

package io.sermant.integration.configuration;

import io.sermant.integration.controller.FlowController;

import org.apache.dubbo.rpc.RpcException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * global flow control exception handling
 *
 * @author chengyouling
 * @since 2025-02-22
 */
@ControllerAdvice(assignableTypes = {FlowController.class})
public class FlowControlExceptionHandler {
    /**
     * 异常处理
     *
     * @param exception 异常
     * @return 异常结果
     */
    @ExceptionHandler(RpcException.class)
    public ResponseEntity<String> handleError(RpcException exception) {
        return new ResponseEntity<>(exception.getMessage(), HttpStatus.OK);
    }
}
