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

package io.sermant.demo.grayscale.rocketmq.integration;

import io.sermant.demo.grayscale.rocketmq.integration.support.KieClient;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;

import com.alibaba.fastjson.JSON;

/**
 * test method class
 *
 * @author chengyouling
 * @since 2024-10-30
 */
public class GrayscaleRocketmqTest {
    private static final RestTemplate restTemplate = new RestTemplate();

    private static final KieClient kieClient = new KieClient(restTemplate);

    private static final String CONFIG_KEY = "grayscale.mq.config";

    private static final String CONSUMER_TYPE_PULL = "PULL";

    private static final String CONSUMER_TYPE_LITE_PULL = "LITE-PULL";

    private static final String CONSUMER_TYPE_PUSH = "PUSH";

    @Test
    @EnabledIfSystemProperty(named = "grayscale.rocketmq.integration.test.type", matches = "PLUGIN_ENABLED_FALSE")
    public void testPluginEnabledFalseConsumeMessage() throws InterruptedException {
        testPluginEnabledFalsePull();
        Thread.sleep(10000);
        testPluginEnabledFalseLitePull();
        Thread.sleep(10000);
        testPluginEnabledFalsePush();
    }

    private void testPluginEnabledFalsePull() throws InterruptedException {
        initAndProduceMessage(false, CONSUMER_TYPE_PULL);
        Thread.sleep(240000);
        String baseResult = getPullBaseResult();
        int baseBaseCount = parseBaseMessageCount(baseResult);
        int baseGrayCount = parseGrayMessageCount(baseResult);
        Assertions.assertTrue(baseBaseCount == 2 && baseGrayCount == 0);
        shutdownConsumer(false, CONSUMER_TYPE_PULL);
    }

    private void testPluginEnabledFalseLitePull() throws InterruptedException {
        initAndProduceMessage(false, CONSUMER_TYPE_LITE_PULL);
        Thread.sleep(240000);
        String baseResult = getLitePullBaseResult();
        int baseBaseCount = parseBaseMessageCount(baseResult);
        int baseGrayCount = parseGrayMessageCount(baseResult);
        Assertions.assertTrue(baseBaseCount == 2 && baseGrayCount == 0);
        shutdownConsumer(false, CONSUMER_TYPE_LITE_PULL);
    }

    private void testPluginEnabledFalsePush() throws InterruptedException {
        initAndProduceMessage(false, CONSUMER_TYPE_PUSH);
        Thread.sleep(240000);
        String baseResult = getPushBaseResult();
        int baseBaseCount = parseBaseMessageCount(baseResult);
        int baseGrayCount = parseGrayMessageCount(baseResult);
        Assertions.assertTrue(baseBaseCount == 2 && baseGrayCount == 0);
        shutdownConsumer(false, CONSUMER_TYPE_PUSH);
    }

    private int parseBaseMessageCount(String messageCount) {
        return (int) JSON.parseObject(messageCount).get("baseMessageCount");
    }

    private int parseGrayMessageCount(String messageCount) {
        return (int) JSON.parseObject(messageCount).get("grayMessageCount");
    }

    @Test
    @EnabledIfSystemProperty(named = "grayscale.rocketmq.integration.test.type", matches = "AUTO_ONLY_BASE")
    public void testAutoOnlyBaseConsumeMessage() throws InterruptedException {
        createGrayscaleConfig("AUTO", "");
        testAutoOnlyBasePull();
        Thread.sleep(15000);
        testAutoOnlyBaseLitePull();
        Thread.sleep(15000);
        testAutoOnlyBasePush();
    }

    private void testAutoOnlyBasePull() throws InterruptedException {
        initAndProduceMessage(false, CONSUMER_TYPE_PULL);
        Thread.sleep(240000);
        String baseResult = getPullBaseResult();
        int baseBaseCount = parseBaseMessageCount(baseResult);
        int baseGrayCount = parseGrayMessageCount(baseResult);
        Assertions.assertTrue(baseBaseCount == 1 && baseGrayCount == 1);
        shutdownConsumer(false, CONSUMER_TYPE_PULL);
    }

    private void testAutoOnlyBaseLitePull() throws InterruptedException {
        initAndProduceMessage(false, CONSUMER_TYPE_LITE_PULL);
        Thread.sleep(240000);
        String baseResult = getLitePullBaseResult();
        int baseBaseCount = parseBaseMessageCount(baseResult);
        int baseGrayCount = parseGrayMessageCount(baseResult);
        Assertions.assertTrue(baseBaseCount == 1 && baseGrayCount == 1);
        shutdownConsumer(false, CONSUMER_TYPE_LITE_PULL);
    }

    private void testAutoOnlyBasePush() throws InterruptedException {
        initAndProduceMessage(false, CONSUMER_TYPE_PUSH);
        Thread.sleep(240000);
        String baseResult = getPushBaseResult();
        int baseBaseCount = parseBaseMessageCount(baseResult);
        int baseGrayCount = parseGrayMessageCount(baseResult);
        Assertions.assertTrue(baseBaseCount == 1 && baseGrayCount == 1);
        shutdownConsumer(false, CONSUMER_TYPE_PUSH);
    }

    @Test
    @EnabledIfSystemProperty(named = "grayscale.rocketmq.integration.test.type", matches = "AUTO_EXC_ONLY_BASE")
    public void testAutoExcOnlyBaseConsumeMessage() throws InterruptedException {
        createGrayscaleConfig("AUTO", "gray");
        testAutoExcOnlyBasePull();
        Thread.sleep(15000);
        testAutoExcOnlyBaseLitePull();
        Thread.sleep(15000);
        testAutoExcOnlyBasePush();
    }

    private void testAutoExcOnlyBasePull() throws InterruptedException {
        initAndProduceMessage(false, CONSUMER_TYPE_PULL);
        Thread.sleep(240000);
        String baseResult = getPullBaseResult();
        int baseBaseCount = parseBaseMessageCount(baseResult);
        int baseGrayCount = parseGrayMessageCount(baseResult);
        Assertions.assertTrue(baseBaseCount == 1 && baseGrayCount == 0);
        shutdownConsumer(false, CONSUMER_TYPE_PULL);
    }

    private void testAutoExcOnlyBaseLitePull() throws InterruptedException {
        initAndProduceMessage(false, CONSUMER_TYPE_LITE_PULL);
        Thread.sleep(240000);
        String baseResult = getLitePullBaseResult();
        int baseBaseCount = parseBaseMessageCount(baseResult);
        int baseGrayCount = parseGrayMessageCount(baseResult);
        Assertions.assertTrue(baseBaseCount == 1 && baseGrayCount == 0);
        shutdownConsumer(false, CONSUMER_TYPE_LITE_PULL);
    }

    private void testAutoExcOnlyBasePush() throws InterruptedException {
        initAndProduceMessage(false, CONSUMER_TYPE_PUSH);
        Thread.sleep(240000);
        String baseResult = getPushBaseResult();
        int baseBaseCount = parseBaseMessageCount(baseResult);
        int baseGrayCount = parseGrayMessageCount(baseResult);
        Assertions.assertTrue(baseBaseCount == 1 && baseGrayCount == 0);
        shutdownConsumer(false, CONSUMER_TYPE_PUSH);
    }

    @Test
    @EnabledIfSystemProperty(named = "grayscale.rocketmq.integration.test.type", matches = "AUTO_BASE_GRAY")
    public void testAutoBaseGrayConsumeMessage() throws InterruptedException {
        createGrayscaleConfig("AUTO", "");
        testAutoBaseGrayPull();
        Thread.sleep(15000);
        testAutoBaseGrayLitePull();
        Thread.sleep(15000);
        testAutoBaseGrayPush();
    }

    @Test
    @EnabledIfSystemProperty(named = "grayscale.rocketmq.integration.test.type", matches = "AUTO_BASE_GRAY_PULL")
    public void testAutoBaseGrayPull() throws InterruptedException {
        createGrayscaleConfig("AUTO", "");
        initAndProduceMessage(true, CONSUMER_TYPE_PULL);
        Thread.sleep(240000);
        String grayResult = getPullGrayResult();
        int grayGrayCount = parseGrayMessageCount(grayResult);
        String baseResult = getPullBaseResult();
        int baseBaseCount = parseBaseMessageCount(baseResult);
        Assertions.assertTrue(baseBaseCount == 1 && grayGrayCount == 1);
    }

    @Test
    @EnabledIfSystemProperty(named = "grayscale.rocketmq.integration.test.type", matches = "AUTO_BASE_GRAY_LITE_PULL")
    public void testAutoBaseGrayLitePull() throws InterruptedException {
        createGrayscaleConfig("AUTO", "");
        initAndProduceMessage(true, CONSUMER_TYPE_LITE_PULL);
        Thread.sleep(240000);
        String grayResult = getLitePullGrayResult();
        int grayBaseCount = parseBaseMessageCount(grayResult);
        int grayGrayCount = parseGrayMessageCount(grayResult);
        String baseResult = getLitePullBaseResult();
        int baseBaseCount = parseBaseMessageCount(baseResult);
        int baseGrayCount = parseGrayMessageCount(baseResult);
        Assertions.assertTrue(baseBaseCount == 1 && baseGrayCount == 0 && grayBaseCount == 0 && grayGrayCount == 1);
    }

    @Test
    @EnabledIfSystemProperty(named = "grayscale.rocketmq.integration.test.type", matches = "AUTO_BASE_GRAY_PUSH")
    public void testAutoBaseGrayPush() throws InterruptedException {
        createGrayscaleConfig("AUTO", "");
        initAndProduceMessage(true, CONSUMER_TYPE_PUSH);
        Thread.sleep(240000);
        String baseResult = getPushBaseResult();
        int baseBaseCount = parseBaseMessageCount(baseResult);
        int baseGrayCount = parseGrayMessageCount(baseResult);
        String grayResult = getPushGrayResult();
        int grayBaseCount = parseBaseMessageCount(grayResult);
        int grayGrayCount = parseGrayMessageCount(grayResult);
        Assertions.assertTrue(baseBaseCount == 1 && baseGrayCount == 0 && grayBaseCount == 0 && grayGrayCount == 1);
    }

    @Test
    @EnabledIfSystemProperty(named = "grayscale.rocketmq.integration.test.type", matches = "AUTO_EXC_BASE_GRAY")
    public void testAutoExcBaseGrayConsumeMessage() throws InterruptedException {
        createGrayscaleConfig("AUTO", "gray");
        testAutoExcBaseGrayPull();
        Thread.sleep(15000);
        testAutoExcBaseGrayLitePull();
        Thread.sleep(15000);
        testAutoExcBaseGrayPush();
    }

    private void testAutoExcBaseGrayPull() throws InterruptedException {
        initAndProduceMessage(true, CONSUMER_TYPE_PULL);
        Thread.sleep(240000);
        String grayResult = getPullGrayResult();
        int grayBaseCount = parseBaseMessageCount(grayResult);
        int grayGrayCount = parseGrayMessageCount(grayResult);
        String baseResult = getPullBaseResult();
        int baseBaseCount = parseBaseMessageCount(baseResult);
        int baseGrayCount = parseGrayMessageCount(baseResult);
        Assertions.assertTrue(baseBaseCount == 1 && baseGrayCount == 0 && grayBaseCount == 0 && grayGrayCount == 1);
        shutdownConsumer(true, CONSUMER_TYPE_PULL);
    }

    private void testAutoExcBaseGrayLitePull() throws InterruptedException {
        initAndProduceMessage(true, CONSUMER_TYPE_LITE_PULL);
        Thread.sleep(240000);
        String baseResult = getLitePullBaseResult();
        int baseBaseCount = parseBaseMessageCount(baseResult);
        int baseGrayCount = parseGrayMessageCount(baseResult);
        String grayResult = getLitePullGrayResult();
        int grayBaseCount = parseBaseMessageCount(grayResult);
        int grayGrayCount = parseGrayMessageCount(grayResult);
        Assertions.assertTrue(baseBaseCount == 1 && baseGrayCount == 0 && grayBaseCount == 0 && grayGrayCount == 1);
        shutdownConsumer(true, CONSUMER_TYPE_LITE_PULL);
    }

    private void testAutoExcBaseGrayPush() throws InterruptedException {
        initAndProduceMessage(true, CONSUMER_TYPE_PUSH);
        Thread.sleep(240000);
        String baseResult = getPushBaseResult();
        int baseBaseCount = parseBaseMessageCount(baseResult);
        int baseGrayCount = parseGrayMessageCount(baseResult);
        String grayResult = getPushGrayResult();
        int grayBaseCount = parseBaseMessageCount(grayResult);
        int grayGrayCount = parseGrayMessageCount(grayResult);
        Assertions.assertTrue(baseBaseCount == 1 && baseGrayCount == 0 && grayBaseCount == 0 && grayGrayCount == 1);
        shutdownConsumer(true, CONSUMER_TYPE_PUSH);
    }

    @Test
    @EnabledIfSystemProperty(named = "grayscale.rocketmq.integration.test.type", matches = "BASE_ONLY_BASE")
    public void testBaseOnlyBaseConsumeMessage() throws InterruptedException {
        createGrayscaleConfig("BASE", "");
        testBaseOnlyBasePull();
        Thread.sleep(15000);
        testBaseOnlyBaseLitePull();
        Thread.sleep(15000);
        testBaseOnlyBasePush();
    }

    private void testBaseOnlyBasePull() throws InterruptedException {
        initAndProduceMessage(false, CONSUMER_TYPE_PULL);
        Thread.sleep(240000);
        String baseResult = getPullBaseResult();
        int baseBaseCount = parseBaseMessageCount(baseResult);
        int baseGrayCount = parseGrayMessageCount(baseResult);
        Assertions.assertTrue( baseBaseCount == 1 && baseGrayCount == 1);
        shutdownConsumer(false, CONSUMER_TYPE_PULL);
    }

    private void testBaseOnlyBaseLitePull() throws InterruptedException {
        initAndProduceMessage(false, CONSUMER_TYPE_LITE_PULL);
        Thread.sleep(240000);
        String baseResult = getLitePullBaseResult();
        int baseBaseCount = parseBaseMessageCount(baseResult);
        int baseGrayCount = parseGrayMessageCount(baseResult);
        Assertions.assertTrue( baseBaseCount == 1 && baseGrayCount == 1);
        shutdownConsumer(false, CONSUMER_TYPE_LITE_PULL);
    }

    private void testBaseOnlyBasePush() throws InterruptedException {
        initAndProduceMessage(false, CONSUMER_TYPE_PUSH);
        Thread.sleep(240000);
        String baseResult = getPushBaseResult();
        int baseBaseCount = parseBaseMessageCount(baseResult);
        int baseGrayCount = parseGrayMessageCount(baseResult);
        Assertions.assertTrue( baseBaseCount == 1 && baseGrayCount == 1);
        shutdownConsumer(false, CONSUMER_TYPE_PUSH);
    }

    @Test
    @EnabledIfSystemProperty(named = "grayscale.rocketmq.integration.test.type", matches = "BASE_EXC_ONLY_BASE")
    public void testBaseExcOnlyBaseConsumeMessage() throws InterruptedException {
        createGrayscaleConfig("BASE", "gray");
        testBaseExcOnlyBasePull();
        Thread.sleep(15000);
        testBaseExcOnlyBaseLitePull();
        Thread.sleep(15000);
        testBaseExcOnlyBasePush();
    }

    private void testBaseExcOnlyBasePull() throws InterruptedException {
        initAndProduceMessage(false, CONSUMER_TYPE_PULL);
        Thread.sleep(240000);
        String baseResult = getPullBaseResult();
        int baseBaseCount = parseBaseMessageCount(baseResult);
        int baseGrayCount = parseGrayMessageCount(baseResult);
        Assertions.assertTrue( baseBaseCount == 1 && baseGrayCount == 0);
        shutdownConsumer(false, CONSUMER_TYPE_PULL);
    }

    private void testBaseExcOnlyBaseLitePull() throws InterruptedException {
        initAndProduceMessage(false, CONSUMER_TYPE_LITE_PULL);
        Thread.sleep(240000);
        String baseResult = getLitePullBaseResult();
        int baseBaseCount = parseBaseMessageCount(baseResult);
        int baseGrayCount = parseGrayMessageCount(baseResult);
        Assertions.assertTrue( baseBaseCount == 1 && baseGrayCount == 0);
        shutdownConsumer(false, CONSUMER_TYPE_LITE_PULL);
    }

    private void testBaseExcOnlyBasePush() throws InterruptedException {
        initAndProduceMessage(false, CONSUMER_TYPE_PUSH);
        Thread.sleep(240000);
        String baseResult = getPushBaseResult();
        int baseBaseCount = parseBaseMessageCount(baseResult);
        int baseGrayCount = parseGrayMessageCount(baseResult);
        Assertions.assertTrue( baseBaseCount == 1 && baseGrayCount == 0);
        shutdownConsumer(false, CONSUMER_TYPE_PUSH);
    }

    @Test
    @EnabledIfSystemProperty(named = "grayscale.rocketmq.integration.test.type", matches = "BASE_BASE_GRAY")
    public void testBaseBaseGrayConsumeMessage() throws InterruptedException {
        createGrayscaleConfig("BASE", "");
        testBaseBaseGrayPull();
        Thread.sleep(15000);
        testBaseBaseGrayLitePull();
        Thread.sleep(15000);
        testBaseBaseGrayPush();
    }

    private void testBaseBaseGrayPull() throws InterruptedException {
        initAndProduceMessage(true, CONSUMER_TYPE_PULL);
        Thread.sleep(240000);
        String baseResult = getPullBaseResult();
        int baseBaseCount = parseBaseMessageCount(baseResult);
        int baseGrayCount = parseGrayMessageCount(baseResult);
        String grayResult = getPullGrayResult();
        int grayGrayCount = parseGrayMessageCount(grayResult);
        int grayBaseCount = parseBaseMessageCount(grayResult);
        Assertions.assertTrue(baseBaseCount == 1 && baseGrayCount == 1 && grayGrayCount == 1 && grayBaseCount == 0);
        shutdownConsumer(true, CONSUMER_TYPE_PULL);
    }

    private void testBaseBaseGrayLitePull() throws InterruptedException {
        initAndProduceMessage(true, CONSUMER_TYPE_LITE_PULL);
        Thread.sleep(240000);
        String baseResult = getLitePullBaseResult();
        int baseBaseCount = parseBaseMessageCount(baseResult);
        int baseGrayCount = parseGrayMessageCount(baseResult);
        String grayResult = getLitePullGrayResult();
        int grayGrayCount = parseGrayMessageCount(grayResult);
        int grayBaseCount = parseBaseMessageCount(grayResult);
        Assertions.assertTrue(baseBaseCount == 1 && baseGrayCount == 1 && grayGrayCount == 1 && grayBaseCount == 0);
        shutdownConsumer(true, CONSUMER_TYPE_LITE_PULL);
    }

    private void testBaseBaseGrayPush() throws InterruptedException {
        initAndProduceMessage(true, CONSUMER_TYPE_PUSH);
        Thread.sleep(240000);
        String baseResult = getPushBaseResult();
        int baseBaseCount = parseBaseMessageCount(baseResult);
        int baseGrayCount = parseGrayMessageCount(baseResult);
        String grayResult = getPushGrayResult();
        int grayGrayCount = parseGrayMessageCount(grayResult);
        int grayBaseCount = parseBaseMessageCount(grayResult);
        Assertions.assertTrue(baseBaseCount == 1 && baseGrayCount == 1 && grayGrayCount == 1 && grayBaseCount == 0);
        shutdownConsumer(true, CONSUMER_TYPE_PUSH);
    }

    @Test
    @EnabledIfSystemProperty(named = "grayscale.rocketmq.integration.test.type", matches = "BASE_EXC_BASE_GRAY")
    public void testBaseExcBaseGrayConsumeMessage() throws InterruptedException {
        createGrayscaleConfig("BASE", "gray");
        testBaseExcBaseGrayPull();
        Thread.sleep(15000);
        testBaseExcBaseGrayLitePull();
        Thread.sleep(15000);
        testBaseExcBaseGrayPush();
    }

    private void testBaseExcBaseGrayPull() throws InterruptedException {
        initAndProduceMessage(true, CONSUMER_TYPE_PULL);
        Thread.sleep(240000);
        String baseResult = getPullBaseResult();
        int baseBaseCount = parseBaseMessageCount(baseResult);
        int baseGrayCount = parseGrayMessageCount(baseResult);
        String grayResult = getPullGrayResult();
        int grayBaseCount = parseBaseMessageCount(grayResult);
        int grayGrayCount = parseGrayMessageCount(grayResult);
        Assertions.assertTrue(baseBaseCount == 1 && baseGrayCount == 0 && grayBaseCount == 0 && grayGrayCount == 1);
        shutdownConsumer(true, CONSUMER_TYPE_PULL);
    }

    private void testBaseExcBaseGrayLitePull() throws InterruptedException {
        initAndProduceMessage(true, CONSUMER_TYPE_LITE_PULL);
        Thread.sleep(240000);
        String baseResult = getLitePullBaseResult();
        int baseBaseCount = parseBaseMessageCount(baseResult);
        int baseGrayCount = parseGrayMessageCount(baseResult);
        String grayResult = getLitePullGrayResult();
        int grayBaseCount = parseBaseMessageCount(grayResult);
        int grayGrayCount = parseGrayMessageCount(grayResult);
        Assertions.assertTrue(baseBaseCount == 1 && baseGrayCount == 0 && grayBaseCount == 0 && grayGrayCount == 1);
        shutdownConsumer(true, CONSUMER_TYPE_LITE_PULL);
    }

    private void testBaseExcBaseGrayPush() throws InterruptedException {
        initAndProduceMessage(true, CONSUMER_TYPE_PUSH);
        Thread.sleep(240000);
        String baseResult = getPushBaseResult();
        int baseBaseCount = parseBaseMessageCount(baseResult);
        int baseGrayCount = parseGrayMessageCount(baseResult);
        String grayResult = getPushGrayResult();
        int grayBaseCount = parseBaseMessageCount(grayResult);
        int grayGrayCount = parseGrayMessageCount(grayResult);
        Assertions.assertTrue(baseBaseCount == 1 && baseGrayCount == 0 && grayBaseCount == 0 && grayGrayCount == 1);
        shutdownConsumer(true, CONSUMER_TYPE_PUSH);
    }

    private void initAndProduceMessage(boolean isGrayInstanceInit, String consumerType)
            throws InterruptedException {
        clearCacheCount(isGrayInstanceInit);
        initProducer();
        produceMessage(consumerType);
        if (isGrayInstanceInit) {
            // Trigger start gray consumer.
            restTemplate.getForObject("http://127.0.0.1:9010/initConsumer?consumerType={1}", String.class,
                    consumerType);
            Thread.sleep(150000);
        }

        // Trigger start base consumer.
        restTemplate.getForObject("http://127.0.0.1:9000/initConsumer?consumerType={1}", String.class, consumerType);
    }

    private void shutdownConsumer(boolean isGrayInstanceInit, String consumerType) {
        if (isGrayInstanceInit) {
            restTemplate.getForObject("http://127.0.0.1:9010/shutdownConsumer?consumerType={1}", String.class,
                    consumerType);
        }
        restTemplate.getForObject("http://127.0.0.1:9000/shutdownConsumer?consumerType={1}", String.class,
                consumerType);
    }

    private String getPullBaseResult() {
        return restTemplate.getForObject("http://127.0.0.1:9000/getPullConsumerMessageCount", String.class);
    }

    private String getPushBaseResult() {
        return restTemplate.getForObject("http://127.0.0.1:9000/getPushConsumerMessageCount", String.class);
    }

    private String getLitePullBaseResult() {
        return restTemplate.getForObject("http://127.0.0.1:9000/getLitePullConsumerMessageCount", String.class);
    }

    private String getPullGrayResult() {
        return restTemplate.getForObject("http://127.0.0.1:9010/getPullConsumerMessageCount", String.class);
    }

    private String getPushGrayResult() {
        return restTemplate.getForObject("http://127.0.0.1:9010/getPushConsumerMessageCount", String.class);
    }

    private String getLitePullGrayResult() {
        return restTemplate.getForObject("http://127.0.0.1:9010/getLitePullConsumerMessageCount", String.class);
    }

    private void initProducer() {
        restTemplate.getForObject("http://127.0.0.1:9030/initProducer", String.class);
        restTemplate.getForObject("http://127.0.0.1:9040/initProducer", String.class);
    }

    private void produceMessage(String consumerType) {
        String path = consumerType.equals(CONSUMER_TYPE_PULL) ? "producePullMessage" :
                consumerType.equals(CONSUMER_TYPE_LITE_PULL) ? "produceLitePullMessage" : "producePushMessage";
        restTemplate.getForObject("http://127.0.0.1:9030/" + path + "?message={1}", String.class, "message");
        restTemplate.getForObject("http://127.0.0.1:9040/" + path + "?message={1}", String.class, "gray-message");
    }

    private void clearCacheCount(boolean isGrayInstanceInit) {
        if (isGrayInstanceInit) {
            restTemplate.getForObject("http://127.0.0.1:9010/clearMessageCount", String.class);
        }
        restTemplate.getForObject("http://127.0.0.1:9000/clearMessageCount", String.class);
    }

    private void createGrayscaleConfig(String consumeMode, String excludeTag) {
        String testModel = System.getProperty("grayscale.rocketmq.integration.test.type");
        kieClient.updateServiceNameLabels(testModel);
        String CONTENT = "enabled: true\n"
                + "grayscale:\n"
                + "  - consumerGroupTag: gray\n"
                + "    serviceMeta:\n"
                + "      version: 1.0.1\n"
                + "    trafficTag:\n"
                + "      x_lane_canary: gray\n"
                + "base:\n"
                + "  autoCheckDelayTime: 5\n"
                + "  consumeMode: " + consumeMode + "\n"
                + "  excludeGroupTags: [\""+ excludeTag + "\"]\n";
        Assertions.assertTrue(kieClient.publishConfig(CONFIG_KEY, CONTENT));
    }

    @AfterEach
    public void deleteGrayscaleConfig() {
        try {
            kieClient.deleteKey(CONFIG_KEY);
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
