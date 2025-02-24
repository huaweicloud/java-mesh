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

package io.sermant.mq.grayscale.rocketmq.interceptor;

import io.sermant.core.plugin.agent.entity.ExecuteContext;
import io.sermant.mq.grayscale.rocketmq.RocketMqTestAbstract;
import io.sermant.mq.grayscale.rocketmq.utils.RocketMqSubscriptionDataUtils;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.impl.consumer.DefaultMQPushConsumerImpl;
import org.apache.rocketmq.client.impl.consumer.RebalanceImpl;
import org.apache.rocketmq.common.protocol.heartbeat.SubscriptionData;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * RocketMqSchedulerRebuildSubscriptionInterceptor test
 *
 * @author chengyouling
 * @since 2024-09-10
 **/
public class RocketMqSchedulerRebuildSubscriptionInterceptorTest extends RocketMqTestAbstract {
    @Test
    public void testDoAfter() throws Exception {
        RocketMqSubscriptionDataUtils.resetTagChangeMap("127.0.0.1:9876", "TOPIC_TEST",
                "consumerGroup", true);
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("consumerGroup");
        DefaultMQPushConsumerImpl pushConsumer = new DefaultMQPushConsumerImpl(consumer, null);
        consumer.setNamesrvAddr("127.0.0.1:9876");
        RebalanceImpl rebalanced = getPushConsumerRebalanced(pushConsumer);

        ExecuteContext context = ExecuteContext.forMemberMethod(rebalanced, null, null, null, null);
        SubscriptionData subscriptionData = new SubscriptionData();
        subscriptionData.setTopic("TOPIC_TEST");
        SubscriptionData retrySubscriptionData = new SubscriptionData();
        retrySubscriptionData.setTopic("%RETRY%consumerGroup");
        ConcurrentMap<String, SubscriptionData> map = new ConcurrentHashMap<>();
        map.put("test", subscriptionData);
        map.put("testRetry", retrySubscriptionData);
        context.afterMethod(map, null);
        RocketMqSchedulerRebuildSubscriptionInterceptor interceptor
                = new RocketMqSchedulerRebuildSubscriptionInterceptor();
        interceptor.doAfter(context);
        Assert.assertEquals("(x_lane_canary in ('gray'))", subscriptionData.getSubString());
        Assert.assertEquals("(x_lane_canary in ('gray'))", retrySubscriptionData.getSubString());
    }
}
