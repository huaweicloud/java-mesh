/*
 * Copyright (C) 2022-2022 Huawei Technologies Co., Ltd. All rights reserved.
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

package io.sermant.registry.declarers.cloud3.x;

import io.sermant.core.plugin.agent.declarer.InterceptDeclarer;
import io.sermant.core.plugin.agent.matcher.ClassMatcher;
import io.sermant.core.plugin.agent.matcher.MethodMatcher;
import io.sermant.registry.declarers.AbstractDoubleRegistryDeclarer;
import io.sermant.registry.interceptors.cloud3.x.ZookeeperInstanceSupplierInterceptor;

/**
 * Compatible with obtaining instance lists for springCloud 3.x
 * <p>
 * Since version 3.x, InstanceSupplier is added to the zookeeper registration and discovery to ontain instance,
 * which will wrap the original DiscoveryClient, so the original interception point will no longer be used
 * </p>
 *
 * @author zhouss
 * @since 2022-03-29
 */
public class ZookeeperInstanceSupplierDeclarer extends AbstractDoubleRegistryDeclarer {
    /**
     * The fully qualified name of the enhanced class
     */
    private static final String ENHANCE_CLASS =
            "org.springframework.cloud.zookeeper.discovery.ZookeeperServiceInstanceListSupplier";

    /**
     * The fully qualified name of the interception class
     */
    private static final String INTERCEPT_CLASS = ZookeeperInstanceSupplierInterceptor.class.getCanonicalName();

    @Override
    public ClassMatcher getClassMatcher() {
        return ClassMatcher.nameEquals(ENHANCE_CLASS);
    }

    @Override
    public InterceptDeclarer[] getInterceptDeclarers(ClassLoader classLoader) {
        return new InterceptDeclarer[]{
                InterceptDeclarer.build(MethodMatcher.nameEquals("filteredByZookeeperStatusUp"), INTERCEPT_CLASS)
        };
    }
}
