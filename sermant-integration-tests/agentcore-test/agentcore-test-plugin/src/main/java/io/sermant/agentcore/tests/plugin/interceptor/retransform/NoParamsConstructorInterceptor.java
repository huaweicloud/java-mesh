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

package io.sermant.agentcore.tests.plugin.interceptor.retransform;

import io.sermant.core.plugin.agent.entity.ExecuteContext;
import io.sermant.core.plugin.agent.interceptor.AbstractInterceptor;

import java.lang.reflect.Method;

/**
 * 测试无参构造函数的拦截
 *
 * @author tangle
 * @since 2023-09-07
 */
public class NoParamsConstructorInterceptor extends AbstractInterceptor {
    @Override
    public ExecuteContext before(ExecuteContext context) throws Exception {
        Class<?> targetClass = Class.forName(
                "com.example.sermant.agentcore.test.application.tests.retransform.ReTransformTest");
        Method targetMethod = targetClass.getMethod("setConstructorFlag", boolean.class);
        targetMethod.invoke(null, true);
        return context;
    }

    @Override
    public ExecuteContext after(ExecuteContext context) throws Exception {
        return context;
    }
}
