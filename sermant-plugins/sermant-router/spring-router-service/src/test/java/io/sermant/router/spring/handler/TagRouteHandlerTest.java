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

package io.sermant.router.spring.handler;

import io.sermant.core.config.ConfigManager;
import io.sermant.core.event.config.EventConfig;
import io.sermant.core.plugin.config.PluginConfigManager;
import io.sermant.router.common.config.RouterConfig;
import io.sermant.router.common.constants.RouterConstant;
import io.sermant.router.common.request.RequestData;
import io.sermant.router.config.cache.ConfigCache;
import io.sermant.router.spring.RuleInitializationUtils;
import io.sermant.router.spring.TestDefaultServiceInstance;
import io.sermant.router.common.cache.AppCache;

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
 * TagRouteHandler Unit tests
 *
 * @author lilai
 * @since 2023-02-27
 */
public class TagRouteHandlerTest {
    private static TagRouteHandler tagRouteHandler;

    private static MockedStatic<ConfigManager> mockConfigManager;

    private static MockedStatic<PluginConfigManager> mockPluginConfigManager;

    /**
     * Perform mock before the UT is executed
     */
    @BeforeClass
    public static void before() {
         EventConfig config = new EventConfig();
        config.setEnable(false);
        mockConfigManager = Mockito.mockStatic(ConfigManager.class);
        mockConfigManager.when(() -> ConfigManager.getConfig(EventConfig.class)).thenReturn(config);
        tagRouteHandler = new TagRouteHandler();
        mockPluginConfigManager = Mockito.mockStatic(PluginConfigManager.class);
        mockPluginConfigManager.when(() -> PluginConfigManager.getPluginConfig(RouterConfig.class))
                .thenReturn(Mockito.mock(RouterConfig.class));
    }

    /**
     * Resources are released after UT is executed
     */
    @AfterClass
    public static void after() {
        mockConfigManager.close();
        mockPluginConfigManager.close();
    }

    /**
     * Test the getTargetInstancesByRules method
     */
    @Test
    public void testGetTargetInstancesByTagRules() {
        RuleInitializationUtils.initTagMatchRule();
        List<Object> instances = new ArrayList<>();
        ServiceInstance instance1 = TestDefaultServiceInstance.getTestDefaultServiceInstance("1.0.0");
        instances.add(instance1);
        ServiceInstance instance2 = TestDefaultServiceInstance.getTestDefaultServiceInstance("1.0.1");
        instances.add(instance2);
        Map<String, String> metadata = new HashMap<>();
        metadata.put("group", "red");
        AppCache.INSTANCE.setMetadata(metadata);
        List<Object> targetInstances = tagRouteHandler.handle("foo", instances,
                new RequestData(null, null, null));
        Assert.assertEquals(1, targetInstances.size());
        Assert.assertEquals(instance2, targetInstances.get(0));
        ConfigCache.getLabel(RouterConstant.SPRING_CACHE_NAME).resetRouteRule(Collections.emptyMap());
    }

    /**
     * Test the getTargetInstancesByRules method
     */
    @Test
    public void testGetTargetInstancesByConsumerTagRules() {
        RuleInitializationUtils.initConsumerTagRules();
        List<Object> instances = new ArrayList<>();
        Map<String, String> metadata1 = new HashMap<>();
        metadata1.put("group", "red");
        Map<String, String> metadata2 = new HashMap<>();
        metadata2.put("group", "green");
        ServiceInstance instance1 = new TestDefaultServiceInstance(metadata1);
        instances.add(instance1);
        ServiceInstance instance2 = new TestDefaultServiceInstance(metadata2);
        instances.add(instance2);
        AppCache.INSTANCE.setMetadata(metadata1);
        List<Object> targetInstances = tagRouteHandler.handle("foo", instances,
                new RequestData(null, null, null));
        Assert.assertEquals(1, targetInstances.size());
        Assert.assertEquals(instance1, targetInstances.get(0));
        ConfigCache.getLabel(RouterConstant.SPRING_CACHE_NAME).resetRouteRule(Collections.emptyMap());
    }

    /**
     * Test the getTargetInstancesByRules method and configure both the service dimension rule and the global dimension
     * rule
     */
    @Test
    public void testGetTargetInstancesByTagRulesWithGlobalRules() {
        RuleInitializationUtils.initGlobalAndServiceTagMatchRules();
        List<Object> instances = new ArrayList<>();
        ServiceInstance instance1 = TestDefaultServiceInstance.getTestDefaultServiceInstance("1.0.0");
        instances.add(instance1);
        ServiceInstance instance2 = TestDefaultServiceInstance.getTestDefaultServiceInstance("1.0.1");
        instances.add(instance2);
        Map<String, String> metadata = new HashMap<>();
        metadata.put("group", "red");
        AppCache.INSTANCE.setMetadata(metadata);
        List<Object> targetInstances = tagRouteHandler.handle("foo", instances,
                new RequestData(null, null, null));
        Assert.assertEquals(1, targetInstances.size());
        Assert.assertEquals(instance2, targetInstances.get(0));
        ConfigCache.getLabel(RouterConstant.SPRING_CACHE_NAME).resetRouteRule(Collections.emptyMap());
        ConfigCache.getLabel(RouterConstant.SPRING_CACHE_NAME).resetGlobalRule(Collections.emptyList());
    }

    /**
     * Test the getTargetInstancesByRules method and configure global dimension rules
     */
    @Test
    public void testGetTargetInstancesByGlobalRules() {
        RuleInitializationUtils.initGlobalTagMatchRules();
        List<Object> instances = new ArrayList<>();
        ServiceInstance instance1 = TestDefaultServiceInstance.getTestDefaultServiceInstance("1.0.0");
        instances.add(instance1);
        ServiceInstance instance2 = TestDefaultServiceInstance.getTestDefaultServiceInstance("1.0.1");
        instances.add(instance2);
        Map<String, String> metadata = new HashMap<>();
        metadata.put("group", "red");
        AppCache.INSTANCE.setMetadata(metadata);
        List<Object> targetInstances = tagRouteHandler.handle("foo", instances,
                new RequestData(null, null, null));
        Assert.assertEquals(1, targetInstances.size());
        Assert.assertEquals(instance2, targetInstances.get(0));
        ConfigCache.getLabel(RouterConstant.SPRING_CACHE_NAME).resetGlobalRule(Collections.emptyList());
    }

    /**
     * The following table describes the test rules：
     * given:match indicates an exact match for az 1, no policy is set,
     * and the downstream provider has an az 1 instance
     * when: Route to az1
     * then: There are instances that can be matched to the az1 downstream provider
     */
    @Test
    public void testGetTargetInstancesByTagRulesWithoutPolicySceneOne() {
        RuleInitializationUtils.initAZTagMatchRule();
        List<Object> instances = new ArrayList<>();
        ServiceInstance instance1 = TestDefaultServiceInstance.getTestDefaultServiceInstance("1.0.0", "az1");
        ServiceInstance instance2 = TestDefaultServiceInstance.getTestDefaultServiceInstance("1.0.0", "az2");
        instances.add(instance1);
        instances.add(instance2);
        Map<String, String> metadata = new HashMap<>();
        metadata.put("zone", "az1");
        AppCache.INSTANCE.setMetadata(metadata);
        List<Object> targetInstances = tagRouteHandler.handle("foo", instances,
                new RequestData(null, null, null));
        Assert.assertEquals(1, targetInstances.size());
        Assert.assertEquals(instance1, targetInstances.get(0));
        ConfigCache.getLabel(RouterConstant.SPRING_CACHE_NAME).resetRouteRule(Collections.emptyMap());
    }

    /**
     * The following table describes the test rules：
     * given：match indicates an exact match for az 1, no policy is set,
     * and the downstream provider does not have an az 1 instance
     * when：route to az1
     * then：If the downstream provider instance cannot be matched, all instances are returned
     */
    @Test
    public void testGetTargetInstancesByTagRulesWithoutPolicySceneTwo() {
        RuleInitializationUtils.initAZTagMatchRule();
        List<Object> instances = new ArrayList<>();
        ServiceInstance instance1 = TestDefaultServiceInstance.getTestDefaultServiceInstance("1.0.0", "az2");
        ServiceInstance instance2 = TestDefaultServiceInstance.getTestDefaultServiceInstance("1.0.0", "az3");
        instances.add(instance1);
        instances.add(instance2);
        Map<String, String> metadata = new HashMap<>();
        metadata.put("zone", "az1");
        AppCache.INSTANCE.setMetadata(metadata);
        List<Object> targetInstances = tagRouteHandler.handle("foo", instances,
                new RequestData(null, null, null));
        Assert.assertEquals(2, targetInstances.size());
        ConfigCache.getLabel(RouterConstant.SPRING_CACHE_NAME).resetRouteRule(Collections.emptyMap());
    }

    /**
     * The following table describes the test rules：
     * given：match is an exact match for az 1, the trigger threshold of the policy is set to 60,
     * and the downstream provider has an az 1 instance
     * when：route to az1
     * then：It can be matched to the az 1 downstream provider instance
     */
    @Test
    public void testGetTargetInstancesByTagRulesWithPolicySceneOne() {
        RuleInitializationUtils.initAZTagMatchTriggerThresholdPolicyRule();
        List<Object> instances = new ArrayList<>();
        ServiceInstance instance1 = TestDefaultServiceInstance.getTestDefaultServiceInstance("1.0.0", "az1");
        ServiceInstance instance2 = TestDefaultServiceInstance.getTestDefaultServiceInstance("1.0.0", "az2");
        ServiceInstance instance3 = TestDefaultServiceInstance.getTestDefaultServiceInstance("1.0.1", "az1");
        instances.add(instance1);
        instances.add(instance2);
        instances.add(instance3);
        Map<String, String> metadata = new HashMap<>();
        metadata.put("zone", "az1");
        AppCache.INSTANCE.setMetadata(metadata);
        List<Object> targetInstances = tagRouteHandler.handle("foo", instances,
                new RequestData(null, null, null));
        Assert.assertEquals(2, targetInstances.size());
        ConfigCache.getLabel(RouterConstant.SPRING_CACHE_NAME).resetRouteRule(Collections.emptyMap());
    }

    /**
     * The following table describes the test rules：
     * given: match is an exact match for az 1, the trigger threshold of the policy is set to 60,
     * and the downstream provider has or does not have an az 1 instance
     * when: route to az1，However, the threshold rule is triggered: the number of available az 1 instances/ALL
     * instances is less than the trigger threshold
     * then: All instances are returned
     */
    @Test
    public void testGetTargetInstancesByTagRulesWithPolicySceneTwo() {
        RuleInitializationUtils.initAZTagMatchTriggerThresholdPolicyRule();
        // Scenario 1: The downstream provider has instances that meet the requirements
        List<Object> instances = new ArrayList<>();
        ServiceInstance instance1 = TestDefaultServiceInstance.getTestDefaultServiceInstance("1.0.0", "az1");
        ServiceInstance instance2 = TestDefaultServiceInstance.getTestDefaultServiceInstance("1.0.0", "az2");
        instances.add(instance1);
        instances.add(instance2);
        Map<String, String> metadata = new HashMap<>();
        metadata.put("zone", "az1");
        AppCache.INSTANCE.setMetadata(metadata);
        List<Object> targetInstances = tagRouteHandler.handle("foo", instances,
                new RequestData(null, null, null));
        Assert.assertEquals(2, targetInstances.size());

        // Scenario 2: The downstream provider does not have instances that meet the requirements
        List<Object> instances2 = new ArrayList<>();
        ServiceInstance instance3 = TestDefaultServiceInstance.getTestDefaultServiceInstance("1.0.0", "az3");
        ServiceInstance instance4 = TestDefaultServiceInstance.getTestDefaultServiceInstance("1.0.0", "az4");
        instances2.add(instance3);
        instances2.add(instance4);
        metadata.put("zone", "az1");
        AppCache.INSTANCE.setMetadata(metadata);
        targetInstances = tagRouteHandler.handle("foo", instances2,
                new RequestData(null, null, null));
        Assert.assertEquals(2, targetInstances.size());

        ConfigCache.getLabel(RouterConstant.SPRING_CACHE_NAME).resetRouteRule(Collections.emptyMap());
    }

    /**
     * The following table describes the test rules：
     * given: Match to accurately match az1, set the trigger threshold of policy to 50, min Alliances to 4,
     * and downstream providers have az1 instances
     * when: route to az1，MinAlliances less than 5,
     * az1 instance/ALL instance proportion greater than triggerThreshold
     * then: Can be matched to the downstream provider instance of az 1
     */
    @Test
    public void testGetTargetInstancesByTagRulesWithPolicySceneThree() {
        RuleInitializationUtils.initAZTagMatchTriggerThresholdMinAllInstancesPolicyRule();
        List<Object> instances = new ArrayList<>();
        ServiceInstance instance1 = TestDefaultServiceInstance.getTestDefaultServiceInstance("1.0.0", "az1");
        ServiceInstance instance2 = TestDefaultServiceInstance.getTestDefaultServiceInstance("1.0.0", "az2");
        ServiceInstance instance3 = TestDefaultServiceInstance.getTestDefaultServiceInstance("1.0.1", "az1");
        ServiceInstance instance4 = TestDefaultServiceInstance.getTestDefaultServiceInstance("1.0.1", "az2");
        ServiceInstance instance5 = TestDefaultServiceInstance.getTestDefaultServiceInstance("1.0.2", "az1");
        instances.add(instance1);
        instances.add(instance2);
        instances.add(instance3);
        instances.add(instance4);
        instances.add(instance5);
        Map<String, String> metadata = new HashMap<>();
        metadata.put("zone", "az1");
        AppCache.INSTANCE.setMetadata(metadata);
        List<Object> targetInstances = tagRouteHandler.handle("foo", instances,
                new RequestData(null, null, null));
        Assert.assertEquals(3, targetInstances.size());
        ConfigCache.getLabel(RouterConstant.SPRING_CACHE_NAME).resetRouteRule(Collections.emptyMap());
    }

    /**
     * The following table describes the test rules：
     * given：Match to accurately match az1, set the trigger threshold of policy to 50, minAlliances to 4,
     * and downstream providers have az1 instances
     * when：Route to az1, minAlliances less than 5, az1 instance/ALL instance ratio less than triggerThreshold
     * then：Returns all downstream provider instances
     */
    @Test
    public void testGetTargetInstancesByTagRulesWithPolicySceneFour() {
        RuleInitializationUtils.initAZTagMatchTriggerThresholdMinAllInstancesPolicyRule();
        List<Object> instances = new ArrayList<>();
        ServiceInstance instance1 = TestDefaultServiceInstance.getTestDefaultServiceInstance("1.0.0", "az1");
        ServiceInstance instance2 = TestDefaultServiceInstance.getTestDefaultServiceInstance("1.0.0", "az2");
        ServiceInstance instance3 = TestDefaultServiceInstance.getTestDefaultServiceInstance("1.0.1", "az1");
        ServiceInstance instance4 = TestDefaultServiceInstance.getTestDefaultServiceInstance("1.0.1", "az2");
        ServiceInstance instance5 = TestDefaultServiceInstance.getTestDefaultServiceInstance("1.0.2", "az2");
        instances.add(instance1);
        instances.add(instance2);
        instances.add(instance3);
        instances.add(instance4);
        instances.add(instance5);
        Map<String, String> metadata = new HashMap<>();
        metadata.put("zone", "az1");
        AppCache.INSTANCE.setMetadata(metadata);
        List<Object> targetInstances = tagRouteHandler.handle("foo", instances,
                new RequestData(null, null, null));
        Assert.assertEquals(5, targetInstances.size());
        ConfigCache.getLabel(RouterConstant.SPRING_CACHE_NAME).resetRouteRule(Collections.emptyMap());
    }

    /**
     * The following table describes the test rules：
     * given：Match to accurately match az1, set the trigger threshold of policy to 50, minAlliances to 4,
     * and downstream providers have az1 instances
     * when：Route to az1, minAlliances greater than 3, az1 instance/ALL instance ratio less than or greater than
     * triggerThreshold
     * then：Returns the provider instance of the downstream AZ 1
     */
    @Test
    public void testGetTargetInstancesByTagRulesWithPolicySceneFive() {
        RuleInitializationUtils.initAZTagMatchTriggerThresholdMinAllInstancesPolicyRule();
        // Scenario 1: The proportion of az1 instances/ALL instances is greater than the triggerThreshold
        List<Object> instances = new ArrayList<>();
        ServiceInstance instance1 = TestDefaultServiceInstance.getTestDefaultServiceInstance("1.0.0", "az1");
        ServiceInstance instance2 = TestDefaultServiceInstance.getTestDefaultServiceInstance("1.0.0", "az2");
        ServiceInstance instance3 = TestDefaultServiceInstance.getTestDefaultServiceInstance("1.0.1", "az1");
        instances.add(instance1);
        instances.add(instance2);
        instances.add(instance3);
        Map<String, String> metadata = new HashMap<>();
        metadata.put("zone", "az1");
        AppCache.INSTANCE.setMetadata(metadata);
        List<Object> targetInstances = tagRouteHandler.handle("foo", instances,
                new RequestData(null, null, null));
        Assert.assertEquals(2, targetInstances.size());

        // Scenario 2: The proportion of az1 instances/ALL instances is less than the triggerThreshold
        List<Object> instances2 = new ArrayList<>();
        ServiceInstance instance4 = TestDefaultServiceInstance.getTestDefaultServiceInstance("1.0.0", "az1");
        ServiceInstance instance5 = TestDefaultServiceInstance.getTestDefaultServiceInstance("1.0.0", "az2");
        ServiceInstance instance6 = TestDefaultServiceInstance.getTestDefaultServiceInstance("1.0.1", "az2");
        instances2.add(instance4);
        instances2.add(instance5);
        instances2.add(instance6);
        targetInstances = tagRouteHandler.handle("foo", instances2,
                new RequestData(null, null, null));
        Assert.assertEquals(1, targetInstances.size());

        // There is no match on all instances returned
        List<Object> instances3 = new ArrayList<>();
        ServiceInstance instance7 = TestDefaultServiceInstance.getTestDefaultServiceInstance("1.0.0", "az3");
        ServiceInstance instance8 = TestDefaultServiceInstance.getTestDefaultServiceInstance("1.0.0", "az2");
        ServiceInstance instance9 = TestDefaultServiceInstance.getTestDefaultServiceInstance("1.0.1", "az2");
        instances3.add(instance7);
        instances3.add(instance8);
        instances3.add(instance9);
        targetInstances = tagRouteHandler.handle("foo", instances3,
                new RequestData(null, null, null));
        Assert.assertEquals(3, targetInstances.size());

        ConfigCache.getLabel(RouterConstant.SPRING_CACHE_NAME).resetRouteRule(Collections.emptyMap());
    }
}
