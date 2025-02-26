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

import io.sermant.core.common.LoggerFactory;
import io.sermant.core.plugin.agent.entity.ExecuteContext;
import io.sermant.core.utils.StringUtils;
import io.sermant.mq.grayscale.rocketmq.service.RocketMqConsumerGroupAutoCheck;
import io.sermant.mq.grayscale.rocketmq.utils.RocketMqGrayscaleConfigUtils;
import io.sermant.mq.grayscale.rocketmq.utils.RocketMqReflectUtils;
import io.sermant.mq.grayscale.rocketmq.utils.RocketMqSubscriptionDataUtils;

import org.apache.rocketmq.client.impl.consumer.RebalanceImpl;
import org.apache.rocketmq.client.impl.factory.MQClientInstance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

/**
 * TAG/SQL92 query message statement interceptor
 *
 * @author chengyouling
 * @since 2024-05-27
 **/
public class RocketMqSchedulerRebuildSubscriptionInterceptor extends RocketMqAbstractInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    private static final String RETRY_TOPIC_FLAG = "%RETRY%";

    @Override
    public ExecuteContext doAfter(ExecuteContext context) throws Exception {
        ConcurrentMap<String, Object> subscriptionInner = (ConcurrentMap<String, Object>) context.getResult();
        RebalanceImpl balance = (RebalanceImpl) context.getObject();
        if (balance.getConsumerGroup() == null) {
            return context;
        }
        List<Object> retryTopicSubscriptions = new ArrayList<>();
        List<Object> originTopicSubscriptions = new ArrayList<>();
        buildTopicSubscriptions(subscriptionInner, retryTopicSubscriptions, originTopicSubscriptions);
        Object changedOriginSubscription = null;
        for (Object subscriptionData : originTopicSubscriptions) {
            if (RocketMqSubscriptionDataUtils
                    .isExpressionTypeInaccurate(RocketMqReflectUtils.getExpressionType(subscriptionData))) {
                continue;
            }
            String topic = RocketMqReflectUtils.getTopic(subscriptionData);
            if (!RocketMqSubscriptionDataUtils.getGrayTagChangeFlag(topic, balance)) {
                continue;
            }
            buildSql92SubscriptionData(subscriptionData, balance, topic);

            // sql92 expression is associated only with the consumer group. Therefore,
            // using any of the changed subscription build retry-topic sql92 expression.
            changedOriginSubscription = subscriptionData;
        }
        if (changedOriginSubscription != null) {
            // update %RETRY%+GROUP substring with sql92
            updateRetrySubscriptionData(changedOriginSubscription, retryTopicSubscriptions);
        }
        return context;
    }

    private void buildTopicSubscriptions(ConcurrentMap<String, Object> subscriptionInner,
            List<Object> retryTopicSubscriptions, List<Object> originTopicSubscriptions) {
        // If one consumer subscribe many topic when create, subscriptionInner data structure is:
        // many origin topic subscriptionData, as topic1:subscriptionData1 topic2:subscriptionData2
        // Currently, there is only one retry topic subscriptionData, because the retry topic is associated with
        // consumer group, so that one consumer group has only one rebalancing task, still using array storage
        // for convenience
        for (Object subscriptionData : subscriptionInner.values()) {
            String tempTopic = RocketMqReflectUtils.getTopic(subscriptionData);
            if (tempTopic.contains(RETRY_TOPIC_FLAG)) {
                retryTopicSubscriptions.add(subscriptionData);
            } else {
                originTopicSubscriptions.add(subscriptionData);
            }
        }
    }

    private void updateRetrySubscriptionData(Object subscriptionData, Collection<Object> retryTopicSubscriptions) {
        for (Object subData : retryTopicSubscriptions) {
            RocketMqReflectUtils.getTagsSet(subData).clear();
            RocketMqReflectUtils.getCodeSet(subData).clear();
            RocketMqReflectUtils.setSubscriptionData(subData, "setExpressionType",
                new Class[]{String.class}, new Object[]{"SQL92"});
            RocketMqReflectUtils.setSubscriptionData(subData, "setSubVersion",
                new Class[]{long.class}, new Object[]{System.currentTimeMillis()});
            String originSubData = RocketMqReflectUtils.getSubString(subData);
            String sqlSubstr = RocketMqReflectUtils.getSubString(subscriptionData);
            RocketMqReflectUtils.setSubscriptionData(subData, "setSubString",
                new Class[]{String.class}, new Object[]{sqlSubstr});
            String originTopic = RocketMqReflectUtils.getTopic(subscriptionData);
            String retryTopic = RocketMqReflectUtils.getTopic(subData);
            LOGGER.warning(String.format(Locale.ENGLISH, "update retry topic [%s] SQL92 expression, "
                    + "originTopic: [%s], originSubStr: [%s], newSubStr: [%s]", retryTopic, originTopic,
                    originSubData, sqlSubstr));
        }
    }

    private void buildSql92SubscriptionData(Object subscriptionData, RebalanceImpl balance, String topic) {
        String consumerGroup = balance.getConsumerGroup();
        MQClientInstance instance = balance.getmQClientFactory();
        if (StringUtils.isEmpty(RocketMqGrayscaleConfigUtils.getGrayGroupTag())) {
            RocketMqConsumerGroupAutoCheck.setMqClientInstance(topic, consumerGroup, instance);
            RocketMqConsumerGroupAutoCheck.syncUpdateCacheGrayTags();
            RocketMqConsumerGroupAutoCheck.startSchedulerCheckGroupTask();
        }
        String namesrvAddr = balance.getmQClientFactory().getClientConfig().getNamesrvAddr();
        resetsSql92SubscriptionData(topic, consumerGroup, subscriptionData, namesrvAddr);

        // update change flag when finished build substr
        RocketMqSubscriptionDataUtils.resetTagChangeMap(namesrvAddr, topic, consumerGroup, false);
    }

    private void resetsSql92SubscriptionData(String topic, String consumerGroup, Object subscriptionData,
            String namesrvAddr) {
        String subscribeScope = RocketMqSubscriptionDataUtils.buildSubscribeScope(topic, consumerGroup,
                namesrvAddr);
        RocketMqSubscriptionDataUtils.resetsSql92SubscriptionData(subscriptionData, subscribeScope);
    }
}
