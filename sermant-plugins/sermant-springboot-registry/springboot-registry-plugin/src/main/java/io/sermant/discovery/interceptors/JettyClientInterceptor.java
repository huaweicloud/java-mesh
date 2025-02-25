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

package io.sermant.discovery.interceptors;

import io.sermant.core.plugin.agent.entity.ExecuteContext;
import io.sermant.core.plugin.agent.interceptor.AbstractInterceptor;
import io.sermant.core.utils.ClassUtils;
import io.sermant.discovery.entity.JettyClientWrapper;
import io.sermant.discovery.utils.HttpConstants;
import io.sermant.discovery.utils.PlugEffectWhiteBlackUtils;
import io.sermant.discovery.utils.RequestInterceptorUtils;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpConversation;

import java.net.URI;
import java.util.Map;

/**
 * webclient Jetty interception point
 *
 * @author provenceee
 * @since 2023-04-25
 */
public class JettyClientInterceptor extends AbstractInterceptor {
    private static final String WRAPPER_CLASS_NAME = "io.sermant.discovery.entity.JettyClientWrapper";

    private static volatile boolean init;

    @Override
    public ExecuteContext before(ExecuteContext context) {
        init();
        Object[] arguments = context.getArguments();
        URI uri = (URI) context.getArguments()[1];
        if (!PlugEffectWhiteBlackUtils.isHostEqualRealmName(uri.getHost())) {
            return context;
        }
        Map<String, String> hostAndPath = RequestInterceptorUtils.recoverHostAndPath(uri.getPath());
        if (!PlugEffectWhiteBlackUtils.isPlugEffect(hostAndPath.get(HttpConstants.HTTP_URI_SERVICE))) {
            return context;
        }
        context.skip(new JettyClientWrapper((HttpClient) context.getObject(), (HttpConversation) arguments[0], uri));
        return context;
    }

    @Override
    public ExecuteContext after(ExecuteContext context) {
        return context;
    }

    private void init() {
        if (!init) {
            synchronized (JettyClientInterceptor.class) {
                if (!init) {
                    ClassUtils.defineClass(WRAPPER_CLASS_NAME, getClass().getClassLoader());
                    init = true;
                }
            }
        }
    }
}
