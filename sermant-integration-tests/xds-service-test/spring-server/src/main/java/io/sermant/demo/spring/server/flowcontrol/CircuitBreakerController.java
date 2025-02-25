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

package io.sermant.demo.spring.server.flowcontrol;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * CircuitBreakerController
 *
 * @author zhp
 * @since 2025-01-13
 **/
@RestController
public class CircuitBreakerController {
    @Value("${connectTimeout}")
    private int timeout;

    @Value("${statusCode}")
    private int statusCode;

    /**
     * testRequestCircuitBreaker
     *
     * @return the result of request circuit breaker
     */
    @GetMapping("/testRequestCircuitBreaker")
    public ResponseEntity<String> testRequestCircuitBreaker() {
        try {
            Thread.sleep(timeout);
        } catch (InterruptedException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("testRequestCircuitBreaker");
        }
        return ResponseEntity.ok().body("testRequestCircuitBreaker");
    }

    /**
     * testInstanceCircuitBreaker
     *
     * @return the result of instance circuit breaker
     */
    @GetMapping("/testInstanceCircuitBreaker")
    public ResponseEntity<String> testInstanceCircuitBreaker() {
        return ResponseEntity.status(statusCode).body(String.valueOf(statusCode));
    }
}
