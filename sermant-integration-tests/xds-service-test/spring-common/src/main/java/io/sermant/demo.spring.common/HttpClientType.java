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

package io.sermant.demo.spring.common;

/**
 * HTTP client types
 *
 * @author zhp
 * @since 2025-01-13
 **/
public enum HttpClientType {
    /**
     * httpClient
     */
    HTTP_CLIENT("HTTP_CLIENT"),

    /**
     * okHttp2
     */
    OK_HTTP2("OK_HTTP2"),

    /**
     * okHttp3
     */
    OK_HTTP3("OK_HTTP3"),

    /**
     * httpUrlConnection
     */
    HTTP_URL_CONNECTION("HTTP_URL_CONNECTION");

    private final String clientName;

    /**
     * Constructor
     *
     * @param clientName clent name
     */
    HttpClientType(String clientName) {
        this.clientName = clientName;
    }

    public String getClientName() {
        return clientName;
    }
}
