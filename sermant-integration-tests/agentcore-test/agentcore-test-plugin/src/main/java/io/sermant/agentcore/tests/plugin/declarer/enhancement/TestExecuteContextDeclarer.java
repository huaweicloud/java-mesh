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

package io.sermant.agentcore.tests.plugin.declarer.enhancement;

import io.sermant.agentcore.tests.plugin.interceptor.enhancement.SetArgumentsInterceptor;
import io.sermant.agentcore.tests.plugin.interceptor.enhancement.SetFieldInterceptor;
import io.sermant.agentcore.tests.plugin.interceptor.enhancement.SkipInterceptor;
import io.sermant.core.plugin.agent.declarer.AbstractPluginDeclarer;
import io.sermant.core.plugin.agent.declarer.InterceptDeclarer;
import io.sermant.core.plugin.agent.matcher.ClassMatcher;
import io.sermant.core.plugin.agent.matcher.MethodMatcher;

/**
 * 测试ExecuteContext增强能力
 *
 * @author luanwenfei
 * @since 2023-09-07
 */
public class TestExecuteContextDeclarer extends AbstractPluginDeclarer {
    @Override
    public ClassMatcher getClassMatcher() {
        return ClassMatcher.nameEquals("com.example.sermant.agentcore.test.application.tests.enhancement."
                + "EnhancementTest");
    }

    @Override
    public InterceptDeclarer[] getInterceptDeclarers(ClassLoader classLoader) {
        return new InterceptDeclarer[]{
                InterceptDeclarer.build(MethodMatcher.nameEquals("testSkipFunction"), new SkipInterceptor()),
                InterceptDeclarer.build(MethodMatcher.nameEquals("testSetFiledFunction"), new SetFieldInterceptor()),
                InterceptDeclarer.build(MethodMatcher.nameEquals("testSetArguments"), new SetArgumentsInterceptor())
        };
    }
}
