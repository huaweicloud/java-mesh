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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * RetryController
 *
 * @author zhp
 * @since 2025-01-13
 **/
@RestController
public class RetryController {
    @Value("${retryTimeout}")
    private int timeout;

    private int requestCount = 0;

    /**
     * testGateWayError
     *
     * @return the result of retry on GetWayError
     */
    @RequestMapping("testGateWayError")
    public ResponseEntity<String> testGateWayError() {
        if (requestCount > 0) {
            requestCount++;
            return ResponseEntity.ok().body(String.valueOf(requestCount));
        }
        requestCount++;
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(String.valueOf(requestCount));
    }

    /**
     * testRetryOnHeader
     *
     * @return the result of retry on response header
     */
    @GetMapping("/testRetryOnHeader")
    public ResponseEntity<String> testRetryOnHeader() {
        if (requestCount > 0) {
            requestCount++;
            return ResponseEntity.ok().body(String.valueOf(requestCount));
        }
        requestCount++;
        HttpHeaders headers = new HttpHeaders();
        headers.add("needRetry", "true");
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).headers(headers).body(String.valueOf(requestCount));
    }

    /**
     * testRetryOnStatusCode
     *
     * @return the result of retry on status code
     */
    @GetMapping("/testRetryOnStatusCode")
    public ResponseEntity<String> testRetryOnStatusCode() {
        if (requestCount > 0) {
            requestCount++;
            return ResponseEntity.ok().body(String.valueOf(requestCount));
        }
        requestCount++;
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(String.valueOf(requestCount));
    }

    /**
     * testTimeOut
     *
     * @return the result of retry on request time out
     */
    @GetMapping("/testTimeOut")
    public ResponseEntity<String> testTimeOut() {
        if (requestCount > 0) {
            requestCount++;
            return ResponseEntity.ok().body(String.valueOf(requestCount));
        }
        requestCount++;
        try {
            Thread.sleep(timeout);
        } catch (InterruptedException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(String.valueOf(requestCount));
        }
        return ResponseEntity.ok().body(String.valueOf(requestCount));
    }

    /**
     * testConnectError
     *
     * @return the result of retry on 4xx error
     */
    @GetMapping("/test4xxError")
    public ResponseEntity<String> test4xxError() {
        if (requestCount > 0) {
            requestCount++;
            return ResponseEntity.ok().body(String.valueOf(requestCount));
        }
        requestCount++;
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(String.valueOf(requestCount));
    }

    /**
     * test5xxError
     *
     * @return the result of retry on 5xx error
     */
    @GetMapping("/test5xxError")
    public ResponseEntity<String> test5xxError() {
        if (requestCount > 0) {
            requestCount++;
            return ResponseEntity.ok().body(String.valueOf(requestCount));
        }
        requestCount++;
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(String.valueOf(requestCount));
    }

    /**
     * get the request count
     *
     * @return the result of request count
     */
    @GetMapping("/getRequestCount")
    public ResponseEntity<String> getRequestCount() {
        return ResponseEntity.ok().body(String.valueOf(requestCount));
    }

    /**
     * reset the request count
     *
     * @return the result of request count
     */
    @GetMapping("/reset")
    public ResponseEntity<String> reset() {
        requestCount = 0;
        return ResponseEntity.ok().body(String.valueOf(requestCount));
    }
}
