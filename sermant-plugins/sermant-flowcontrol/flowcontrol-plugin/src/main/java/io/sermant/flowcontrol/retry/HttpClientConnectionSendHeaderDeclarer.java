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

package io.sermant.flowcontrol.retry;

import io.sermant.core.plugin.agent.declarer.InterceptDeclarer;
import io.sermant.core.plugin.agent.matcher.ClassMatcher;
import io.sermant.core.plugin.agent.matcher.MethodMatcher;
import io.sermant.flowcontrol.AbstractXdsDeclarer;

/**
 * http client request declarer
 *
 * @author zhp
 * @since 2024-11-30
 */
public class HttpClientConnectionSendHeaderDeclarer extends AbstractXdsDeclarer {
    /**
     * the fully qualified name of the enhanced class
     */
    private static final String[] ENHANCE_CLASS = {"org.apache.hc.core5.http.impl.io.DefaultBHttpClientConnection",
            "org.apache.http.impl.DefaultBHttpClientConnection"};

    @Override
    public ClassMatcher getClassMatcher() {
        return ClassMatcher.nameContains(ENHANCE_CLASS);
    }

    @Override
    public InterceptDeclarer[] getInterceptDeclarers(ClassLoader classLoader) {
        return new InterceptDeclarer[]{
                InterceptDeclarer.build(MethodMatcher.nameEquals("sendRequestHeader"),
                        new HttpRequestSendHeaderInterceptor())
        };
    }
}
