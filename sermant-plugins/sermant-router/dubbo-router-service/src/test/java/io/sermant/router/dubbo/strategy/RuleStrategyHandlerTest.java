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

package io.sermant.router.dubbo.strategy;

import io.sermant.core.config.ConfigManager;
import io.sermant.core.event.config.EventConfig;
import io.sermant.core.plugin.config.PluginConfigManager;
import io.sermant.router.common.cache.DubboCache;
import io.sermant.router.common.config.RouterConfig;
import io.sermant.router.common.constants.RouterConstant;
import io.sermant.router.config.entity.Route;
import io.sermant.router.config.entity.Rule;
import io.sermant.router.dubbo.AlibabaInvoker;
import io.sermant.router.dubbo.ApacheInvoker;

import io.sermant.router.dubbo.handler.TagRouteHandler;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * label routing policy test
 *
 * @author provenceee
 * @since 2022-03-22
 */
public class RuleStrategyHandlerTest {
    private final List<Route> routes;

    private final Rule rule;

    private static MockedStatic<PluginConfigManager> mockPluginConfigManager;

    /**
     * Mock before UT execution
     */
    @BeforeClass
    public static void before() {
        mockPluginConfigManager = Mockito.mockStatic(PluginConfigManager.class);
        mockPluginConfigManager.when(() -> PluginConfigManager.getPluginConfig(RouterConfig.class))
                .thenReturn(Mockito.mock(RouterConfig.class));
    }

    /**
     * Release resources after UT execution
     */
    @AfterClass
    public static void after() {
        mockPluginConfigManager.close();
    }

    /**
     * constructor
     */
    public RuleStrategyHandlerTest() {
        rule = new Rule();
        routes = new ArrayList<>();
        Map<String, String> tags1 = new HashMap<>();
        tags1.put(RouterConstant.META_VERSION_KEY, "0.0.1");
        Route route1 = new Route();
        route1.setTags(tags1);
        route1.setWeight(100);
        routes.add(route1);
        Map<String, String> tags2 = new HashMap<>();
        tags2.put(RouterConstant.META_VERSION_KEY, "0.0.2");
        Route route2 = new Route();
        route2.setTags(tags2);
        route2.setWeight(100);
        routes.add(route2);
        rule.setRoute(routes);
    }

    /**
     * Test whether Alibaba Invoker hits an instance of version 0.0.1
     */
    @Test
    public void testAlibabaV1() {
        List<Object> invokers = new ArrayList<>();
        AlibabaInvoker<Object> invoker1 = new AlibabaInvoker<>("0.0.1");
        invokers.add(invoker1);
        AlibabaInvoker<Object> invoker2 = new AlibabaInvoker<>("0.0.2");
        invokers.add(invoker2);
        List<Object> matchInvokers = RuleStrategyHandler.INSTANCE.getFlowMatchInvokers("foo", invokers, rule);
        Assert.assertEquals(100, routes.get(0).getWeight().intValue());
        Assert.assertEquals(1, matchInvokers.size());
        Assert.assertEquals(invoker1, matchInvokers.get(0));
    }

    /**
     * Test whether Alibaba Invoker hits an instance of version 0.0.1
     */
    @Test
    public void testAlibabaV1ByRequest() {
        List<Object> invokers = new ArrayList<>();
        AlibabaInvoker<Object> invoker1 = new AlibabaInvoker<>("0.0.1");
        invokers.add(invoker1);
        AlibabaInvoker<Object> invoker2 = new AlibabaInvoker<>("0.0.2");
        invokers.add(invoker2);
        List<Object> matchInvokers = RuleStrategyHandler.INSTANCE.getMatchInvokersByRequest("foo", invokers,
                Collections.singletonMap(RouterConstant.META_VERSION_KEY, "0.0.1"));
        Assert.assertEquals(1, matchInvokers.size());
        Assert.assertEquals(invoker1, matchInvokers.get(0));
    }

    /**
     * Test whether Alibaba Invoker misses the instance of version 0.0.1
     */
    @Test
    public void testAlibabaMismatch() {
        List<Object> invokers = new ArrayList<>();
        AlibabaInvoker<Object> invoker1 = new AlibabaInvoker<>("0.0.1");
        invokers.add(invoker1);
        AlibabaInvoker<Object> invoker2 = new AlibabaInvoker<>("0.0.2");
        invokers.add(invoker2);
        routes.get(0).setWeight(0);

        // The test matches the route on the instance, and there is no random instance
        List<Object> matchInvokers = RuleStrategyHandler.INSTANCE.getFlowMatchInvokers("foo", invokers, rule);
        Assert.assertEquals(1, matchInvokers.size());
        Assert.assertEquals(invoker2, matchInvokers.get(0));

        // Test the situation where there is no matching route and instances with mismatched labels are selected
        List<Map<String, String>> tags = new ArrayList<>();
        tags.add(routes.get(0).getTags());
        List<Object> mismatchInvokers = RuleStrategyHandler.INSTANCE.getMismatchInvokers("foo", invokers, tags, true);
        Assert.assertEquals(1, mismatchInvokers.size());
        Assert.assertEquals(invoker2, mismatchInvokers.get(0));
    }

    /**
     * Test whether Apache Invoker hits an instance of version 0.0.1
     */
    @Test
    public void testApacheV1() {
        List<Object> invokers = new ArrayList<>();
        ApacheInvoker<Object> invoker1 = new ApacheInvoker<>("0.0.1");
        invokers.add(invoker1);
        ApacheInvoker<Object> invoker2 = new ApacheInvoker<>("0.0.2");
        invokers.add(invoker2);
        List<Object> matchInvokers = RuleStrategyHandler.INSTANCE.getFlowMatchInvokers("foo", invokers, rule);
        Assert.assertEquals(100, routes.get(0).getWeight().intValue());
        Assert.assertEquals(1, matchInvokers.size());
        Assert.assertEquals(invoker1, matchInvokers.get(0));
    }

    /**
     * Test whether Apache Invoker hits an instance of version 0.0.1
     */
    @Test
    public void testApacheV1ByRequest() {
        List<Object> invokers = new ArrayList<>();
        ApacheInvoker<Object> invoker1 = new ApacheInvoker<>("0.0.1");
        invokers.add(invoker1);
        ApacheInvoker<Object> invoker2 = new ApacheInvoker<>("0.0.2");
        invokers.add(invoker2);
        List<Object> matchInvokers = RuleStrategyHandler.INSTANCE
                .getMatchInvokersByRequest("foo", invokers,
                        Collections.singletonMap(RouterConstant.META_VERSION_KEY, "0.0.1"));
        Assert.assertEquals(100, routes.get(0).getWeight().intValue());
        Assert.assertEquals(1, matchInvokers.size());
        Assert.assertEquals(invoker1, matchInvokers.get(0));
    }

    /**
     * Test the situation where Apache invoker misses the 0.0.1 version instance
     */
    @Test
    public void testApacheMismatch() {
        List<Object> invokers = new ArrayList<>();
        ApacheInvoker<Object> invoker1 = new ApacheInvoker<>("0.0.1");
        invokers.add(invoker1);
        ApacheInvoker<Object> invoker2 = new ApacheInvoker<>("0.0.2");
        invokers.add(invoker2);
        routes.get(0).setWeight(0);

        // Test matching routing, no random instances found
        List<Object> matchInvokers = RuleStrategyHandler.INSTANCE.getFlowMatchInvokers("foo", invokers, rule);
        Assert.assertEquals(1, matchInvokers.size());
        Assert.assertEquals(invoker2, matchInvokers.get(0));

        // Test the situation where there is no matching route and instances with mismatched labels are selected
        List<Map<String, String>> tags = new ArrayList<>();
        tags.add(routes.get(0).getTags());
        List<Object> mismatchInvoker = RuleStrategyHandler.INSTANCE.getMismatchInvokers("foo", invokers, tags, true);
        Assert.assertEquals(1, mismatchInvoker.size());
        Assert.assertEquals(invoker2, mismatchInvoker.get(0));
    }

    /**
     * In the rule, there is a selected tag in the route, but there are no instances that match the version. The
     * situation where the invoice hits the fallback version instance
     */
    @Test
    public void testAlibabaV1Fallback() {
        setFallbackRoute();
        List<Object> invokers = new ArrayList<>();
        AlibabaInvoker<Object> invoker1 = new AlibabaInvoker<>("0.0.3");
        invokers.add(invoker1);
        AlibabaInvoker<Object> invoker2 = new AlibabaInvoker<>("0.0.4");
        invokers.add(invoker2);

        // Route randomly hits route 1, but there is no instance of version 0.0.1; Hit fallback and return fallback
        // instance information
        List<Object> matchInvokers = RuleStrategyHandler.INSTANCE.getFlowMatchInvokers("foo", invokers, rule);
        Assert.assertEquals(100, routes.get(0).getWeight().intValue());
        Assert.assertEquals(1, matchInvokers.size());
        Assert.assertEquals(invoker1, matchInvokers.get(0));
    }

    private void setFallbackRoute() {
        List<Route> fallback = new ArrayList<>();
        Map<String, String> tags = new HashMap<>();
        tags.put(RouterConstant.META_VERSION_KEY, "0.0.3");
        Route route = new Route();
        route.setTags(tags);
        route.setWeight(100);
        fallback.add(route);
        rule.setFallback(fallback);
    }

    /**
     * In the rule, both route and fallback are set, and the weights all hit the tag, but the invokers all miss the
     * version instance
     */
    @Test
    public void testApacheV1NotMathRouteFallback() {
        setFallbackRoute();
        List<Object> invokers = new ArrayList<>();
        ApacheInvoker<Object> invoker1 = new ApacheInvoker<>("0.0.2");
        invokers.add(invoker1);
        ApacheInvoker<Object> invoker2 = new ApacheInvoker<>("0.0.4");
        invokers.add(invoker2);

        // Route randomly hits route 1, but there is no instance of version 0.0.1,
        // and fallback does not hit the tag instance, so all instance information is returned
        List<Object> matchInvokers = RuleStrategyHandler.INSTANCE.getFlowMatchInvokers("foo", invokers, rule);
        Assert.assertEquals(2, matchInvokers.size());
    }

    /**
     * The situation where routes are set in the rule but weight calculation misses, and the invoker hits the fallback
     * version instance
     */
    @Test
    public void testApacheV1MathFallback() {
        setFallbackRoute();
        routes.get(0).setWeight(0);
        routes.get(1).setWeight(0);

        List<Object> invokers = new ArrayList<>();
        ApacheInvoker<Object> invoker1 = new ApacheInvoker<>("0.0.2");
        invokers.add(invoker1);
        ApacheInvoker<Object> invoker2 = new ApacheInvoker<>("0.0.3");
        invokers.add(invoker2);

        // The weight calculation of Route misses the tag, while the weight calculation of fallback misses the tag,
        // and returns the instance information of the fallback rule hit
        List<Object> matchInvokers = RuleStrategyHandler.INSTANCE.getFlowMatchInvokers("foo", invokers, rule);
        Assert.assertEquals(1, matchInvokers.size());
        Assert.assertEquals(invoker2, matchInvokers.get(0));
    }

    /**
     * If the route parameter is set in the rule but the weight calculation fails and the fallback also misses the
     * instance, Invoker returns the instance without setting the rule version number
     */
    @Test
    public void testApacheV1BothNotMathFallbackRoute() {
        setFallbackRoute();
        routes.get(0).setWeight(0);
        routes.get(1).setWeight(0);

        List<Object> invokers = new ArrayList<>();
        ApacheInvoker<Object> invoker1 = new ApacheInvoker<>("0.0.1");
        invokers.add(invoker1);
        ApacheInvoker<Object> invoker2 = new ApacheInvoker<>("0.0.4");
        invokers.add(invoker2);

        // None of the route weights or fallback weights are tagged,
        // and the tag instance information is not set in the route
        List<Object> matchInvokers = RuleStrategyHandler.INSTANCE.getFlowMatchInvokers("foo", invokers, rule);
        Assert.assertEquals(1, matchInvokers.size());
        Assert.assertEquals(invoker2, matchInvokers.get(0));
    }
}
