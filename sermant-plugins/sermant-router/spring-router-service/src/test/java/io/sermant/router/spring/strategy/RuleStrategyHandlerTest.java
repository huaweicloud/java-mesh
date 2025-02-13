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

package io.sermant.router.spring.strategy;

import io.sermant.core.plugin.config.PluginConfigManager;
import io.sermant.router.common.config.RouterConfig;
import io.sermant.router.common.constants.RouterConstant;
import io.sermant.router.config.entity.Route;
import io.sermant.router.config.entity.Rule;
import io.sermant.router.spring.TestDefaultServiceInstance;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.cloud.client.ServiceInstance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test RuleStrategyHandler
 *
 * @author provenceee
 * @since 2022-09-09
 */
public class RuleStrategyHandlerTest {
    private static MockedStatic<PluginConfigManager> mockPluginConfigManager;

    private final List<Route> routes;

    private final Rule rule;

    /**
     * Perform mock before the UT is executed
     */
    @BeforeClass
    public static void before() {
        mockPluginConfigManager = Mockito.mockStatic(PluginConfigManager.class);
        mockPluginConfigManager.when(() -> PluginConfigManager.getPluginConfig(RouterConfig.class))
                .thenReturn(Mockito.mock(RouterConfig.class));
    }

    /**
     * Resources are released after UT is executed
     */
    @AfterClass
    public static void after() {
        mockPluginConfigManager.close();
    }

    /**
     * Constructor
     */
    public RuleStrategyHandlerTest() {
        rule = new Rule();
        routes = new ArrayList<>();
        Map<String, String> tags1 = new HashMap<>();
        tags1.put("version", "0.0.1");
        Route route1 = new Route();
        route1.setTags(tags1);
        route1.setWeight(100);
        routes.add(route1);
        Map<String, String> tags2 = new HashMap<>();
        tags2.put("version", "0.0.2");
        Route route2 = new Route();
        route2.setTags(tags2);
        route2.setWeight(100);
        routes.add(route2);
        rule.setRoute(routes);
    }

    /**
     * Test the instance that hits version 0.0.1
     */
    @Test
    public void testMatchV1() {
        List<Object> instances = new ArrayList<>();
        ServiceInstance instance1 = TestDefaultServiceInstance.getTestDefaultServiceInstance("0.0.1");
        instances.add(instance1);
        ServiceInstance instance2 = TestDefaultServiceInstance.getTestDefaultServiceInstance("0.0.2");
        instances.add(instance2);
        List<Object> matchInvoker = RuleStrategyHandler.INSTANCE.getFlowMatchInstances("foo", instances, rule);
        Assert.assertEquals(100, routes.get(0).getWeight().intValue());
        Assert.assertEquals(1, matchInvoker.size());
        Assert.assertEquals(instance1, matchInvoker.get(0));
    }

    /**
     * Test the instance that hits version 0.0.1
     */
    @Test
    public void testMatchV1ByRequest() {
        List<Object> instances = new ArrayList<>();
        ServiceInstance instance1 = TestDefaultServiceInstance.getTestDefaultServiceInstance("0.0.1");
        instances.add(instance1);
        ServiceInstance instance2 = TestDefaultServiceInstance.getTestDefaultServiceInstance("0.0.2");
        instances.add(instance2);
        List<Object> matchInvoker = RuleStrategyHandler.INSTANCE.getMatchInstancesByRequest("foo", instances,
                Collections.singletonMap("version", "0.0.1"));
        Assert.assertEquals(1, matchInvoker.size());
        Assert.assertEquals(instance1, matchInvoker.get(0));
    }

    /**
     * Test for missing instances of version 0.0.1
     */
    @Test
    public void testMismatchV1() {
        List<Object> instances = new ArrayList<>();
        ServiceInstance instance1 = TestDefaultServiceInstance.getTestDefaultServiceInstance("0.0.1");
        instances.add(instance1);
        ServiceInstance instance2 = TestDefaultServiceInstance.getTestDefaultServiceInstance("0.0.2");
        instances.add(instance2);
        routes.get(0).setWeight(0);

        // The test matches the route on the instance, and there is no random instance
        List<Object> matchInstances = RuleStrategyHandler.INSTANCE.getFlowMatchInstances("foo", instances, rule);
        Assert.assertEquals(1, matchInstances.size());
        Assert.assertEquals(instance2, matchInstances.get(0));

        // If the test does not match the previous route, the instance with the label does not match
        List<Map<String, String>> tags = new ArrayList<>();
        tags.add(routes.get(0).getTags());
        List<Object> mismatchInstances = RuleStrategyHandler.INSTANCE
                .getMismatchInstances("foo", instances, tags, true);
        Assert.assertEquals(1, mismatchInstances.size());
        Assert.assertEquals(instance2, mismatchInstances.get(0));
    }

    /**
     * If the route in the rule has a hit tag, but there is no instance that matches the version, and the invoker hits
     * the fallback instance
     */
    @Test
    public void testMatchV1Fallback() {
        setFallbackRoute();

        List<Object> instances = new ArrayList<>();
        ServiceInstance instance1 = TestDefaultServiceInstance.getTestDefaultServiceInstance("0.0.3");
        instances.add(instance1);
        ServiceInstance instance2 = TestDefaultServiceInstance.getTestDefaultServiceInstance("0.0.4");
        instances.add(instance2);

        // Route randomly hits route 1 but does not have an instance of version 0.0.1
        List<Object> matchInvoker = RuleStrategyHandler.INSTANCE.getFlowMatchInstances("foo", instances, rule);
        Assert.assertEquals(100, routes.get(0).getWeight().intValue());
        Assert.assertEquals(1, matchInvoker.size());
        Assert.assertEquals(instance1, matchInvoker.get(0));
    }

    private void setFallbackRoute() {
        List<Route> fallback = new ArrayList<>();
        Map<String, String> tags = new HashMap<>();
        tags.put(RouterConstant.VERSION, "0.0.3");
        Route route = new Route();
        route.setTags(tags);
        route.setWeight(100);
        fallback.add(route);
        rule.setFallback(fallback);
    }

    /**
     * If route and fallback are set in the rule, and the weights are all hit tags, but the invoker does not hit the
     * version instance
     */
    @Test
    public void testSpringV1NotMathRouteFallback() {
        setFallbackRoute();

        List<Object> instances = new ArrayList<>();
        ServiceInstance instance1 = TestDefaultServiceInstance.getTestDefaultServiceInstance("0.0.2");
        instances.add(instance1);
        ServiceInstance instance2 = TestDefaultServiceInstance.getTestDefaultServiceInstance("0.0.4");
        instances.add(instance2);
        // Route randomly hits route 1, but there is no instance of version 0.0.1, and fallback does not hit the tag
        // instance, so all instance information is returned
        List<Object> matchInvoker = RuleStrategyHandler.INSTANCE.getFlowMatchInstances("foo", instances, rule);
        Assert.assertEquals(2, matchInvoker.size());
    }

    /**
     * If routes is set in the rule but the weight calculation fails, and the invoker hits the fallback instance
     */
    @Test
    public void testSpringV1MathFallback() {
        setFallbackRoute();
        routes.get(0).setWeight(0);
        routes.get(1).setWeight(0);

        List<Object> instances = new ArrayList<>();
        ServiceInstance instance1 = TestDefaultServiceInstance.getTestDefaultServiceInstance("0.0.2");
        instances.add(instance1);
        ServiceInstance instance2 = TestDefaultServiceInstance.getTestDefaultServiceInstance("0.0.3");
        instances.add(instance2);
        // None of the route weights are missed, and the fallback weights are calculated as the hit tags,
        // and the information about the instances hit by the fallback rule is returned
        List<Object> matchInvoker = RuleStrategyHandler.INSTANCE.getFlowMatchInstances("foo", instances, rule);
        Assert.assertEquals(1, matchInvoker.size());
        Assert.assertEquals(instance2, matchInvoker.get(0));
    }

    /**
     * If the route parameter is set in the rule but the weight calculation fails and the fallback also misses the
     * instance, Invoker returns the instance without setting the rule version number
     */
    @Test
    public void testSpringV1BothNotMathFallbackRoute() {
        setFallbackRoute();
        routes.get(0).setWeight(0);
        routes.get(1).setWeight(0);

        List<Object> instances = new ArrayList<>();
        ServiceInstance instance1 = TestDefaultServiceInstance.getTestDefaultServiceInstance("0.0.1");
        instances.add(instance1);
        ServiceInstance instance2 = TestDefaultServiceInstance.getTestDefaultServiceInstance("0.0.4");
        instances.add(instance2);
        // None of the route weights or fallback weights are tagged, and the tag instance information is not set
        // in the route
        List<Object> matchInvoker = RuleStrategyHandler.INSTANCE.getFlowMatchInstances("foo", instances, rule);
        Assert.assertEquals(1, matchInvoker.size());
        Assert.assertEquals(instance2, matchInvoker.get(0));
    }
}
