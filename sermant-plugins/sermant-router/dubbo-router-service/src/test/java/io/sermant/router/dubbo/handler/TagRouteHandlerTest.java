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

package io.sermant.router.dubbo.handler;

import io.sermant.core.config.ConfigManager;
import io.sermant.core.event.config.EventConfig;
import io.sermant.core.plugin.config.PluginConfigManager;
import io.sermant.router.common.cache.DubboCache;
import io.sermant.router.common.config.RouterConfig;
import io.sermant.router.common.constants.RouterConstant;
import io.sermant.router.config.cache.ConfigCache;
import io.sermant.router.dubbo.ApacheInvoker;
import io.sermant.router.dubbo.RuleInitializationUtils;
import io.sermant.router.dubbo.service.AbstractDirectoryServiceTest.ApacheInvocation;

import org.apache.dubbo.rpc.Invocation;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
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
 * TagRouteHandler unit test
 *
 * @author lilai
 * @since 2023-02-28
 */
public class TagRouteHandlerTest {
    private static TagRouteHandler tagRouteHandler;

    private static EventConfig config;

    private static MockedStatic<ConfigManager> mockConfigManager;

    private static MockedStatic<PluginConfigManager> mockPluginConfigManager;

    /**
     * Mock before UT execution
     */
    @BeforeClass
    public static void before() {
        config = new EventConfig();
        config.setEnable(false);
        mockConfigManager = Mockito.mockStatic(ConfigManager.class);
        mockConfigManager.when(() -> ConfigManager.getConfig(EventConfig.class)).thenReturn(config);
        tagRouteHandler = new TagRouteHandler();
        mockPluginConfigManager = Mockito.mockStatic(PluginConfigManager.class);
        mockPluginConfigManager.when(() -> PluginConfigManager.getPluginConfig(RouterConfig.class))
                .thenReturn(Mockito.mock(RouterConfig.class));
    }

    /**
     * Release resources after UT execution
     */
    @AfterClass
    public static void after() {
        mockConfigManager.close();
        DubboCache.INSTANCE.clear();
        mockPluginConfigManager.close();
    }

    /**
     * Test the getGetTargetExamples method
     */
    @Test
    public void testGetTargetInvokerByTagRules() {
        // initialize the routing rule
        RuleInitializationUtils.initTagMatchRule();
        List<Object> invokers = new ArrayList<>();
        ApacheInvoker<Object> invoker1 = new ApacheInvoker<>("1.0.0");
        invokers.add(invoker1);
        ApacheInvoker<Object> invoker2 = new ApacheInvoker<>("1.0.1");
        invokers.add(invoker2);
        Invocation invocation = new ApacheInvocation();
        invocation.setAttachment("bar", "bar1");
        Map<String, String> queryMap = new HashMap<>();
        queryMap.put("side", "consumer");
        queryMap.put("group", "red");
        queryMap.put("version", "0.0.1");
        queryMap.put("interface", "io.sermant.foo.FooTest");
        Map<String, String> parameters = new HashMap<>();
        parameters.put(RouterConstant.PARAMETERS_KEY_PREFIX + "group", "red");
        DubboCache.INSTANCE.setParameters(parameters);
        DubboCache.INSTANCE.putApplication("io.sermant.foo.FooTest", "foo");
        List<Object> targetInvokers = (List<Object>) tagRouteHandler.handle(DubboCache.INSTANCE.getApplication("io.sermant.foo.FooTest")
                , invokers, invocation, queryMap, "io.sermant.foo.FooTest");
        Assert.assertEquals(1, targetInvokers.size());
        Assert.assertEquals(invoker2, targetInvokers.get(0));
        ConfigCache.getLabel(RouterConstant.DUBBO_CACHE_NAME).resetRouteRule(Collections.emptyMap());
    }

    /**
     * Test the getGetTargetExamples method and configure global dimension rules
     */
    @Test
    public void testGetTargetInvokerByGlobalRules() {
        // Initialize routing rules
        RuleInitializationUtils.initGlobalTagMatchRules();
        List<Object> invokers = new ArrayList<>();
        ApacheInvoker<Object> invoker1 = new ApacheInvoker<>("1.0.0");
        invokers.add(invoker1);
        ApacheInvoker<Object> invoker2 = new ApacheInvoker<>("1.0.1");
        invokers.add(invoker2);
        Invocation invocation = new ApacheInvocation();
        invocation.setAttachment("bar", "bar1");
        Map<String, String> queryMap = new HashMap<>();
        queryMap.put("side", "consumer");
        queryMap.put("group", "red");
        queryMap.put("version", "0.0.1");
        queryMap.put("interface", "io.sermant.foo.FooTest");
        Map<String, String> parameters = new HashMap<>();
        parameters.put(RouterConstant.PARAMETERS_KEY_PREFIX + "group", "red");
        DubboCache.INSTANCE.setParameters(parameters);
        DubboCache.INSTANCE.putApplication("io.sermant.foo.FooTest", "foo");
        List<Object> targetInvokers = (List<Object>) tagRouteHandler.handle(DubboCache.INSTANCE.getApplication("io.sermant.foo.FooTest")
                , invokers, invocation, queryMap, "io.sermant.foo.FooTest");
        Assert.assertEquals(1, targetInvokers.size());
        Assert.assertEquals(invoker2, targetInvokers.get(0));
        ConfigCache.getLabel(RouterConstant.DUBBO_CACHE_NAME).resetGlobalRule(Collections.emptyList());
    }

    /**
     * Test the getGetTargetExamples method while configuring both service dimension rules and global dimension rules
     */
    @Test
    public void testGetTargetInvokerByTagRulesWithGlobalRules() {
        // initialize the routing rule
        RuleInitializationUtils.initGlobalAndServiceTagMatchRules();
        List<Object> invokers = new ArrayList<>();
        ApacheInvoker<Object> invoker1 = new ApacheInvoker<>("1.0.0");
        invokers.add(invoker1);
        ApacheInvoker<Object> invoker2 = new ApacheInvoker<>("1.0.1");
        invokers.add(invoker2);
        Invocation invocation = new ApacheInvocation();
        invocation.setAttachment("bar", "bar1");
        Map<String, String> queryMap = new HashMap<>();
        queryMap.put("side", "consumer");
        queryMap.put("group", "red");
        queryMap.put("version", "0.0.1");
        queryMap.put("interface", "io.sermant.foo.FooTest");
        Map<String, String> parameters = new HashMap<>();
        parameters.put(RouterConstant.PARAMETERS_KEY_PREFIX + "group", "red");
        DubboCache.INSTANCE.setParameters(parameters);
        DubboCache.INSTANCE.putApplication("io.sermant.foo.FooTest", "foo");
        List<Object> targetInvokers = (List<Object>) tagRouteHandler.handle(DubboCache.INSTANCE.getApplication("io.sermant.foo.FooTest")
                , invokers, invocation, queryMap, "io.sermant.foo.FooTest");
        Assert.assertEquals(1, targetInvokers.size());
        Assert.assertEquals(invoker2, targetInvokers.get(0));
        ConfigCache.getLabel(RouterConstant.DUBBO_CACHE_NAME).resetRouteRule(Collections.emptyMap());
        ConfigCache.getLabel(RouterConstant.DUBBO_CACHE_NAME).resetGlobalRule(Collections.emptyList());
    }

    /**
     * Test the getGetTargetExamples method
     */
    @Test
    public void testGetTargetInvokerByConsumerTagRules() {
        // initialize the routing rule
        RuleInitializationUtils.initConsumerTagRules();
        List<Object> invokers = new ArrayList<>();
        Map<String, String> parameters1 = new HashMap<>();
        parameters1.put(RouterConstant.PARAMETERS_KEY_PREFIX + "group", "red");
        ApacheInvoker<Object> invoker1 = new ApacheInvoker<>("1.0.0", parameters1);
        invokers.add(invoker1);
        Map<String, String> parameters2 = new HashMap<>();
        parameters1.put(RouterConstant.PARAMETERS_KEY_PREFIX + "group", "green");
        ApacheInvoker<Object> invoker2 = new ApacheInvoker<>("1.0.1", parameters2);
        invokers.add(invoker2);
        Invocation invocation = new ApacheInvocation();
        invocation.setAttachment("bar", "bar1");
        Map<String, String> queryMap = new HashMap<>();
        queryMap.put("side", "consumer");
        queryMap.put("group", "red");
        queryMap.put("version", "0.0.1");
        queryMap.put("interface", "io.sermant.foo.FooTest");
        Map<String, String> parameters = new HashMap<>();
        parameters.put(RouterConstant.PARAMETERS_KEY_PREFIX + "group", "red");
        DubboCache.INSTANCE.setParameters(parameters);
        DubboCache.INSTANCE.putApplication("io.sermant.foo.FooTest", "foo");
        List<Object> targetInvokers = (List<Object>) tagRouteHandler.handle(DubboCache.INSTANCE.getApplication("io.sermant.foo.FooTest")
                , invokers, invocation, queryMap, "io.sermant.foo.FooTest");
        Assert.assertEquals(1, targetInvokers.size());
        Assert.assertEquals(invoker1, targetInvokers.get(0));
        ConfigCache.getLabel(RouterConstant.DUBBO_CACHE_NAME).resetRouteRule(Collections.emptyMap());
    }

    /**
     * the following table describes the test rules:
     * given: Match is an exact match for az1, no policy is set,
     * downstream providers have az1 instances
     * when: Route to az1
     * then: Can match downstream provider instances of az1
     */
    @Test
    public void testGetTargetInvokerByTagRulesWithoutPolicySceneOne() {
        // initialize the routing rule
        RuleInitializationUtils.initAZTagMatchRule();
        List<Object> invokers = new ArrayList<>();
        ApacheInvoker<Object> invoker1 = new ApacheInvoker<>("1.0.0", "az1");
        invokers.add(invoker1);
        ApacheInvoker<Object> invoker2 = new ApacheInvoker<>("1.0.1", "az2");
        invokers.add(invoker2);
        Invocation invocation = new ApacheInvocation();
        Map<String, String> queryMap = new HashMap<>();
        queryMap.put("zone", "az1");
        queryMap.put("interface", "io.sermant.foo.FooTest");
        Map<String, String> parameters = new HashMap<>();
        parameters.putIfAbsent(RouterConstant.META_ZONE_KEY, "az1");
        DubboCache.INSTANCE.setParameters(parameters);
        DubboCache.INSTANCE.putApplication("io.sermant.foo.FooTest", "foo");
        List<Object> targetInvokers = (List<Object>) tagRouteHandler.handle(
                DubboCache.INSTANCE.getApplication("io.sermant.foo.FooTest")
                , invokers, invocation, queryMap, "io.sermant.foo.FooTest");
        Assert.assertEquals(1, targetInvokers.size());
        Assert.assertEquals(invoker1, targetInvokers.get(0));
        ConfigCache.getLabel(RouterConstant.DUBBO_CACHE_NAME).resetRouteRule(Collections.emptyMap());
    }

    /**
     * The test rule rules are as follows:
     * Given: match is an exact match for az1, no policy is set and downstream providers do not have az1 instances
     * when: route to az1
     * then: Unable to match downstream provider instances, return all instances
     */
    @Test
    public void testGetTargetInvokerByTagRulesWithoutPolicySceneTwo() {
        // initialize the routing rule
        RuleInitializationUtils.initAZTagMatchRule();
        List<Object> invokers = new ArrayList<>();
        ApacheInvoker<Object> invoker1 = new ApacheInvoker<>("1.0.0", "az2");
        invokers.add(invoker1);
        ApacheInvoker<Object> invoker2 = new ApacheInvoker<>("1.0.1", "az3");
        invokers.add(invoker2);
        Invocation invocation = new ApacheInvocation();
        Map<String, String> queryMap = new HashMap<>();
        queryMap.put("zone", "az1");
        queryMap.put("interface", "io.sermant.foo.FooTest");
        Map<String, String> parameters = new HashMap<>();
        parameters.putIfAbsent(RouterConstant.META_ZONE_KEY, "az1");
        DubboCache.INSTANCE.setParameters(parameters);
        DubboCache.INSTANCE.putApplication("io.sermant.foo.FooTest", "foo");
        List<Object> targetInvokers = (List<Object>) tagRouteHandler.handle(
                DubboCache.INSTANCE.getApplication("io.sermant.foo.FooTest")
                , invokers, invocation, queryMap, "io.sermant.foo.FooTest");
        Assert.assertEquals(2, targetInvokers.size());
        ConfigCache.getLabel(RouterConstant.DUBBO_CACHE_NAME).resetRouteRule(Collections.emptyMap());
    }

    /**
     * The test rule is as follows:
     * give: match is an exact match of az1, set the trigger threshold of policy to 60, and downstream providers have az1 instances from
     * when: route to az1
     * then: Can matches downstream provider instances of az1
     */
    @Test
    public void testGetTargetInvokerByTagRulesWithPolicySceneOne() {
        // initialize the routing rule
        RuleInitializationUtils.initAZTagMatchTriggerThresholdPolicyRule();
        List<Object> invokers = new ArrayList<>();
        ApacheInvoker<Object> invoker1 = new ApacheInvoker<>("1.0.0", "az1");
        invokers.add(invoker1);
        ApacheInvoker<Object> invoker2 = new ApacheInvoker<>("1.0.1", "az2");
        invokers.add(invoker2);
        ApacheInvoker<Object> invoker3 = new ApacheInvoker<>("1.0.1", "az1");
        invokers.add(invoker3);
        Invocation invocation = new ApacheInvocation();
        Map<String, String> queryMap = new HashMap<>();
        queryMap.put("zone", "az1");
        queryMap.put("interface", "io.sermant.foo.FooTest");
        Map<String, String> parameters = new HashMap<>();
        parameters.putIfAbsent(RouterConstant.META_ZONE_KEY, "az1");
        DubboCache.INSTANCE.setParameters(parameters);
        DubboCache.INSTANCE.putApplication("io.sermant.foo.FooTest", "foo");
        List<Object> targetInvokers = (List<Object>) tagRouteHandler.handle(
                DubboCache.INSTANCE.getApplication("io.sermant.foo.FooTest")
                , invokers, invocation, queryMap, "io.sermant.foo.FooTest");
        Assert.assertEquals(2, targetInvokers.size());
        ConfigCache.getLabel(RouterConstant.DUBBO_CACHE_NAME).resetRouteRule(Collections.emptyMap());
    }

    /**
     * The test rule is as follows:
     * give: match is an exact match of az1, set the trigger threshold of policy to 60,
     * and downstream providers have/do not have az1 instances
     * when: Route to az1, but trigger threshold rule: az1 has fewer available instances/ALL instances than
     * triggerThreshold
     * then: Return all instances
     */
    @Test
    public void testGetTargetInvokerByTagRulesWithPolicySceneTwo() {
        // initialize the routing rule
        RuleInitializationUtils.initAZTagMatchTriggerThresholdPolicyRule();
        // Scenario 1: The downstream provider has instances that meet the requirements
        List<Object> invokers = new ArrayList<>();
        ApacheInvoker<Object> invoker1 = new ApacheInvoker<>("1.0.0", "az1");
        invokers.add(invoker1);
        ApacheInvoker<Object> invoker2 = new ApacheInvoker<>("1.0.1", "az2");
        invokers.add(invoker2);
        Invocation invocation = new ApacheInvocation();
        Map<String, String> queryMap = new HashMap<>();
        queryMap.put("zone", "az1");
        queryMap.put("interface", "io.sermant.foo.FooTest");
        Map<String, String> parameters = new HashMap<>();
        parameters.putIfAbsent(RouterConstant.META_ZONE_KEY, "az1");
        DubboCache.INSTANCE.setParameters(parameters);
        DubboCache.INSTANCE.putApplication("io.sermant.foo.FooTest", "foo");
        List<Object> targetInvokers = (List<Object>) tagRouteHandler.handle(
                DubboCache.INSTANCE.getApplication("io.sermant.foo.FooTest")
                , invokers, invocation, queryMap, "io.sermant.foo.FooTest");
        Assert.assertEquals(2, targetInvokers.size());

        // Scenario 2: The downstream provider does not have instances that meet the requirements
        List<Object> invokers2 = new ArrayList<>();
        ApacheInvoker<Object> invoker3 = new ApacheInvoker<>("1.0.0", "az2");
        invokers2.add(invoker3);
        ApacheInvoker<Object> invoker4 = new ApacheInvoker<>("1.0.1", "az3");
        invokers2.add(invoker4);
        targetInvokers = (List<Object>) tagRouteHandler.handle(
                DubboCache.INSTANCE.getApplication("io.sermant.foo.FooTest")
                , invokers2, invocation, queryMap, "io.sermant.foo.FooTest");
        Assert.assertEquals(2, targetInvokers.size());

        ConfigCache.getLabel(RouterConstant.DUBBO_CACHE_NAME).resetRouteRule(Collections.emptyMap());
    }

    /**
     * The test rule rules are as follows:
     * given: Match to accurately match az1, set the trigger threshold of policy to 50, min Alliances to 4, and
     * downstream providers have az1 instances
     * when: Route to az1, min Alliances less than 5, az1 instance/ALL instance proportion greater than
     * triggerThreshold
     * then: Can match downstream provider instances of az1
     */
    @Test
    public void testGetTargetInvokerByTagRulesWithPolicySceneThree() {
        // initialize the routing rule
        RuleInitializationUtils.initAZTagMatchTriggerThresholdMinAllInstancesPolicyRule();
        // Scenario 1: The downstream provider has instances that meet the requirements
        List<Object> invokers = new ArrayList<>();
        ApacheInvoker<Object> invoker1 = new ApacheInvoker<>("1.0.0", "az1");
        invokers.add(invoker1);
        ApacheInvoker<Object> invoker2 = new ApacheInvoker<>("1.0.0", "az2");
        invokers.add(invoker2);
        ApacheInvoker<Object> invoker3 = new ApacheInvoker<>("1.0.1", "az1");
        invokers.add(invoker3);
        ApacheInvoker<Object> invoker4 = new ApacheInvoker<>("1.0.1", "az2");
        invokers.add(invoker4);
        ApacheInvoker<Object> invoker5 = new ApacheInvoker<>("1.0.2", "az1");
        invokers.add(invoker5);
        Invocation invocation = new ApacheInvocation();
        Map<String, String> queryMap = new HashMap<>();
        queryMap.put("zone", "az1");
        queryMap.put("interface", "io.sermant.foo.FooTest");
        Map<String, String> parameters = new HashMap<>();
        parameters.putIfAbsent(RouterConstant.META_ZONE_KEY, "az1");
        DubboCache.INSTANCE.setParameters(parameters);
        DubboCache.INSTANCE.putApplication("io.sermant.foo.FooTest", "foo");
        List<Object> targetInvokers = (List<Object>) tagRouteHandler.handle(
                DubboCache.INSTANCE.getApplication("io.sermant.foo.FooTest")
                , invokers, invocation, queryMap, "io.sermant.foo.FooTest");
        Assert.assertEquals(3, targetInvokers.size());
        ConfigCache.getLabel(RouterConstant.DUBBO_CACHE_NAME).resetRouteRule(Collections.emptyMap());
    }

    /**
     * The test rule rules are as follows:
     * given: Match to accurately match az1, set the trigger threshold of policy to 50, min Alliances to 4, and
     * downstream providers have az1 instances
     * when: Route to az1, min Alliances less than 5, az1 instance/ALL instance ratio less than triggerThreshold
     * then: Return all downstream provider instances
     */
    @Test
    public void testGetTargetInvokerByTagRulesWithPolicySceneFour() {
        // initialize the routing rule
        RuleInitializationUtils.initAZTagMatchTriggerThresholdMinAllInstancesPolicyRule();
        // Scenario 1: The downstream provider has instances that meet the requirements
        List<Object> invokers = new ArrayList<>();
        ApacheInvoker<Object> invoker1 = new ApacheInvoker<>("1.0.0", "az1");
        invokers.add(invoker1);
        ApacheInvoker<Object> invoker2 = new ApacheInvoker<>("1.0.0", "az2");
        invokers.add(invoker2);
        ApacheInvoker<Object> invoker3 = new ApacheInvoker<>("1.0.1", "az1");
        invokers.add(invoker3);
        ApacheInvoker<Object> invoker4 = new ApacheInvoker<>("1.0.1", "az2");
        invokers.add(invoker4);
        ApacheInvoker<Object> invoker5 = new ApacheInvoker<>("1.0.2", "az2");
        invokers.add(invoker5);
        Invocation invocation = new ApacheInvocation();
        Map<String, String> queryMap = new HashMap<>();
        queryMap.put("zone", "az1");
        queryMap.put("interface", "io.sermant.foo.FooTest");
        Map<String, String> parameters = new HashMap<>();
        parameters.putIfAbsent(RouterConstant.META_ZONE_KEY, "az1");
        DubboCache.INSTANCE.setParameters(parameters);
        DubboCache.INSTANCE.putApplication("io.sermant.foo.FooTest", "foo");
        List<Object> targetInvokers = (List<Object>) tagRouteHandler.handle(
                DubboCache.INSTANCE.getApplication("io.sermant.foo.FooTest")
                , invokers, invocation, queryMap, "io.sermant.foo.FooTest");
        Assert.assertEquals(5, targetInvokers.size());
        ConfigCache.getLabel(RouterConstant.DUBBO_CACHE_NAME).resetRouteRule(Collections.emptyMap());
    }

    /**
     * The test rule rules are as follows:
     * given: Match to accurately match az1, set the trigger threshold of policy to 50, min Alliances to 4, and
     * downstream providers have az1 instances
     * when: Route to az1, min Alliances greater than 3, az1 instance/ALL instance ratio less than or greater than
     * triggerThreshold
     * then: Return the provider instance of downstream az1
     */
    @Test
    public void testGetTargetInvokerByTagRulesWithPolicySceneFive() {
        // initialize the routing rule
        RuleInitializationUtils.initAZTagMatchTriggerThresholdMinAllInstancesPolicyRule();
        // Scenario 1: The proportion of az1 instances/ALL instances is greater than the triggerThreshold
        List<Object> invokers = new ArrayList<>();
        ApacheInvoker<Object> invoker1 = new ApacheInvoker<>("1.0.0", "az1");
        invokers.add(invoker1);
        ApacheInvoker<Object> invoker2 = new ApacheInvoker<>("1.0.0", "az2");
        invokers.add(invoker2);
        ApacheInvoker<Object> invoker3 = new ApacheInvoker<>("1.0.0", "az1");
        invokers.add(invoker3);
        Invocation invocation = new ApacheInvocation();
        Map<String, String> queryMap = new HashMap<>();
        queryMap.put("zone", "az1");
        queryMap.put("interface", "io.sermant.foo.FooTest");
        Map<String, String> parameters = new HashMap<>();
        parameters.putIfAbsent(RouterConstant.META_ZONE_KEY, "az1");
        DubboCache.INSTANCE.setParameters(parameters);
        DubboCache.INSTANCE.putApplication("io.sermant.foo.FooTest", "foo");
        List<Object> targetInvokers = (List<Object>) tagRouteHandler.handle(
                DubboCache.INSTANCE.getApplication("io.sermant.foo.FooTest")
                , invokers, invocation, queryMap, "io.sermant.foo.FooTest");
        Assert.assertEquals(2, targetInvokers.size());

        // Scenario 2: The proportion of az1 instances/ALL instances is less than the triggerThreshold
        List<Object> invokers2 = new ArrayList<>();
        ApacheInvoker<Object> invoker4 = new ApacheInvoker<>("1.0.0", "az1");
        invokers2.add(invoker4);
        ApacheInvoker<Object> invoker5 = new ApacheInvoker<>("1.0.0", "az2");
        invokers2.add(invoker5);
        ApacheInvoker<Object> invoker6 = new ApacheInvoker<>("1.0.1", "az2");
        invokers2.add(invoker6);
        targetInvokers = (List<Object>) tagRouteHandler.handle(
                DubboCache.INSTANCE.getApplication("io.sermant.foo.FooTest")
                , invokers2, invocation, queryMap, "io.sermant.foo.FooTest");
        Assert.assertEquals(1, targetInvokers.size());

        // If there is no match, all instances will be returned
        List<Object> invokers3 = new ArrayList<>();
        ApacheInvoker<Object> invoker7 = new ApacheInvoker<>("1.0.0", "az3");
        invokers3.add(invoker7);
        ApacheInvoker<Object> invoker8 = new ApacheInvoker<>("1.0.0", "az2");
        invokers3.add(invoker8);
        ApacheInvoker<Object> invoker9 = new ApacheInvoker<>("1.0.1", "az2");
        invokers3.add(invoker9);
        targetInvokers = (List<Object>) tagRouteHandler.handle(
                DubboCache.INSTANCE.getApplication("io.sermant.foo.FooTest")
                , invokers3, invocation, queryMap, "io.sermant.foo.FooTest");
        Assert.assertEquals(3, targetInvokers.size());

        ConfigCache.getLabel(RouterConstant.DUBBO_CACHE_NAME).resetRouteRule(Collections.emptyMap());
    }
}
