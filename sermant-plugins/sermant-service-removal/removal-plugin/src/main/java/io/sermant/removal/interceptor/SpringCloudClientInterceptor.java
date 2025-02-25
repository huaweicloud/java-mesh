/*
 * Copyright (C) 2023-2023 Huawei Technologies Co., Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sermant.removal.interceptor;

import com.netflix.client.ClientRequest;

import io.sermant.core.utils.StringUtils;

/**
 * SpringCloud service invoking enhancement class
 *
 * @author zhp
 * @since 2023-02-17
 */
public class SpringCloudClientInterceptor extends AbstractCallInterceptor<ClientRequest> {
    @Override
    protected int getIndex() {
        return 0;
    }

    @Override
    protected String getHost(ClientRequest request) {
        if (request.getUri() == null) {
            return StringUtils.EMPTY;
        }
        return StringUtils.getString(request.getUri().getHost());
    }

    @Override
    protected String getPort(ClientRequest request) {
        if (request.getUri() == null) {
            return StringUtils.EMPTY;
        }
        return StringUtils.getString(request.getUri().getPort());
    }
}
