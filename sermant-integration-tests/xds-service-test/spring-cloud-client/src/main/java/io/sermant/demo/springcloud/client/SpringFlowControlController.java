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

package io.sermant.demo.springcloud.client;

import io.sermant.demo.spring.common.Constants;
import io.sermant.demo.spring.common.entity.Result;
import okhttp3.OkHttpClient;

import org.apache.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * SpringRouterController
 *
 * @author zhp
 * @since 2025-01-13
 **/
@RequestMapping("flowControl")
@RestController
public class SpringFlowControlController {
    private static final int TIMEOUT = 3000;

    /**
     * Test the flow control functionality of the HttpClient client
     *
     * @param host the service address of upstream service
     * @param path request path
     * @param version version
     * @return result
     */
    @GetMapping("testOkHttp3")
    public Result testOkHttp3(String host, String path, String version) {
        String url = Constants.HTTP_PROTOCOL + host + Constants.SLASH + path;
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.readTimeout(TIMEOUT, TimeUnit.MILLISECONDS).writeTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                .connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS);
        OkHttpClient client = builder.build();
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url)
                .addHeader("version", version)
                .build();
        try (okhttp3.Response response = client.newCall(request).execute()) {
            if (response.body() != null) {
                return new Result(response.code(), "", new String(response.body().bytes()));
            }
            return new Result(response.code(), "", "");
        } catch (IOException e) {
            return new Result(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage(), null);
        }
    }
}
