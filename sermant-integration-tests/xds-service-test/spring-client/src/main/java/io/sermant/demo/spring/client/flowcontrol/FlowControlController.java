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

package io.sermant.demo.spring.client.flowcontrol;

import io.sermant.demo.spring.client.util.HttpUtil;
import io.sermant.demo.spring.common.Constants;
import io.sermant.demo.spring.common.HttpClientType;
import io.sermant.demo.spring.common.entity.Result;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * flow control
 *
 * @author zhp
 * @since 2025-01-13
 **/
@RequestMapping("flowControl")
@RestController
public class FlowControlController {
    private static final String VERSION = "version";

    /**
     * Test the flow control functionality of the HttpClient client
     *
     * @param host the service address of upstream service
     * @param path request path
     * @param version version
     * @return result
     */
    @GetMapping("testHttpClient")
    public Result testHttpClient(String host, String path, String version) {
        Map<String, String> headers = new HashMap<>();
        headers.put(VERSION, version);
        return HttpUtil.sendGetRequest(Constants.HTTP_PROTOCOL + host + Constants.SLASH + path,
                HttpClientType.HTTP_CLIENT, headers);
    }

    /**
     * Test the flow control functionality of the OkHttp2
     *
     * @param host the service address of upstream service
     * @param path request path
     * @param version version
     * @return result
     */
    @GetMapping("testOkHttp2")
    public Result testOkHttp2(String host, String path, String version) {
        Map<String, String> headers = new HashMap<>();
        headers.put(VERSION, version);
        return HttpUtil.sendGetRequest(Constants.HTTP_PROTOCOL + host + Constants.SLASH + path,
                HttpClientType.OK_HTTP2, headers);
    }

    /**
     * Test the flow control functionality of the HttpUrlConnection
     *
     * @param host the service address of upstream service
     * @param path request path
     * @param version version
     * @return result
     */
    @GetMapping("testHttpUrlConnection")
    public Result testHttpUrlConnection(String host, String path, String version) {
        Map<String, String> headers = new HashMap<>();
        headers.put(VERSION, version);
        return HttpUtil.sendGetRequest(Constants.HTTP_PROTOCOL + host + Constants.SLASH + path,
                HttpClientType.HTTP_URL_CONNECTION, headers);
    }
}
