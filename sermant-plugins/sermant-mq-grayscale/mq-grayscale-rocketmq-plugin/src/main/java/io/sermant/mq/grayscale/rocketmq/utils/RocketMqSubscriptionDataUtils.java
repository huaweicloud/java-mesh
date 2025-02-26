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

package io.sermant.mq.grayscale.rocketmq.utils;

import io.sermant.core.common.LoggerFactory;
import io.sermant.mq.grayscale.config.ConsumeModeEnum;
import io.sermant.mq.grayscale.config.GrayTagItem;
import io.sermant.mq.grayscale.config.MqGrayConfigCache;
import io.sermant.mq.grayscale.config.MqGrayscaleConfig;
import io.sermant.mq.grayscale.config.rocketmq.RocketMqConfigUtils;
import io.sermant.mq.grayscale.rocketmq.config.RocketMqConsumerClientConfig;

import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.impl.consumer.RebalanceImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * TAG/SQL92 query message statement builder util
 *
 * @author chengyouling
 * @since 2024-06-03
 */
public class RocketMqSubscriptionDataUtils {
    /**
     * tag consume message type
     */
    public static final String EXPRESSION_TYPE_TAG = "TAG";

    /**
     * sql92 consume message type
     */
    public static final String EXPRESSION_TYPE_SQL92 = "SQL92";

    /**
     * afa symbol
     */
    public static final String AFA_SYMBOL = "@";

    /**
     * select all message sql92 expression
     */
    public static final String SELECT_ALL_MESSAGE_SQL = "(_message_tag_ is null) or (_message_tag_ is not null)";

    private static final Pattern PATTERN = Pattern.compile(" and | or ", Pattern.CASE_INSENSITIVE);

    private static final Logger LOGGER = LoggerFactory.getLogger();

    /**
     * auto find gray consumer group correspondents grayscale config at auto model
     * key: namesrvAddr@topic@consumerGroup
     * value: grayTagItems
     */
    private static final Map<String, List<GrayTagItem>> AUTO_CHECK_GRAY_TAGS = new ConcurrentHashMap<>();

    private static final String RETYPE = "%RETRY%";

    private static final String RIGHT_BRACKET = ")";

    private static final String LEFT_BRACKET = "(";

    private static final String AND_SPLICE_STR = " and ";

    private RocketMqSubscriptionDataUtils() {
    }

    /**
     * build sql expression by tag expression tags
     *
     * @param tagsSet tagsSet
     * @return sql92 statement
     */
    public static String buildSql92ExpressionByTags(Set<String> tagsSet) {
        return tagsSet != null && !tagsSet.isEmpty() ? buildTagsExpression(tagsSet) : "";
    }

    private static String buildTagsExpression(Set<String> tagsSet) {
        return "(TAGS is not null and TAGS in " + getStrForSets(tagsSet) + RIGHT_BRACKET;
    }

    private static String getStrForSets(Set<String> tags) {
        StringBuilder builder = new StringBuilder(LEFT_BRACKET);
        for (String tag : tags) {
            builder.append("'").append(tag).append("'");
            builder.append(",");
        }
        builder.deleteCharAt(builder.length() - 1);
        builder.append(RIGHT_BRACKET);
        return builder.toString();
    }

    /**
     * add current gray tags to sql92 expression
     *
     * @param originSubData originSubData
     * @param subscribeScope subscribeScope
     * @return sql expression
     */
    public static String addGrayTagsToSql92Expression(String originSubData, String subscribeScope) {
        String originSubDataBak = originSubData;
        if (!StringUtils.isBlank(originSubDataBak)) {
            originSubDataBak = rebuildWithoutGrayTagSubData(originSubDataBak);
        }
        String sql92Expression = buildSql92Expression(subscribeScope, StringUtils.isBlank(originSubDataBak));
        if (StringUtils.isBlank(sql92Expression)) {
            return originSubDataBak;
        }
        return StringUtils.isBlank(originSubDataBak)
                ? sql92Expression : originSubDataBak + AND_SPLICE_STR + sql92Expression;
    }

    private static String buildSql92Expression(String subscribeScope, boolean isOriginSubEmpty) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.isEmpty(RocketMqGrayscaleConfigUtils.getGrayGroupTag())) {
            // base model return without exclude group message
            if (RocketMqGrayscaleConfigUtils.getConsumeType() == ConsumeModeEnum.BASE) {
                List<GrayTagItem> items = RocketMqGrayscaleConfigUtils.getGrayTagItemByExcludeGroupTags();
                if (!items.isEmpty()) {
                    sb.append(buildBaseConsumerSql92Expression(items, isOriginSubEmpty));
                }
                return sb.toString();
            }

            // auto model return without exclude group and current consume message gray group message
            sb.append(buildBaseConsumerSql92Expression(getAutoTypeGrayTagItems(subscribeScope), isOriginSubEmpty));
        } else {
            MqGrayscaleConfig mqGrayscaleConfig = MqGrayConfigCache.getCacheConfig();
            Optional<GrayTagItem> grayTagItem
                    = mqGrayscaleConfig.getGrayTagByGroupTag(RocketMqGrayscaleConfigUtils.getGrayGroupTag());
            if (grayTagItem.isPresent()) {
                sb.append(buildGrayConsumerSql92Expression(grayTagItem.get(), isOriginSubEmpty));
            } else {
                LOGGER.warning(String.format(Locale.ENGLISH, "current gray group [%s] had not set grayscale, set it "
                        + "and restart service to valid.", RocketMqGrayscaleConfigUtils.getGrayGroupTag()));
            }
        }
        return sb.toString();
    }

    private static List<GrayTagItem> getAutoTypeGrayTagItems(String subscribeScope) {
        List<GrayTagItem> excludeItems = RocketMqGrayscaleConfigUtils.getGrayTagItemByExcludeGroupTags();
        List<GrayTagItem> autoDiscoveryGrayTags = AUTO_CHECK_GRAY_TAGS.get(subscribeScope);
        if (autoDiscoveryGrayTags != null && !autoDiscoveryGrayTags.isEmpty()) {
            excludeItems.addAll(autoDiscoveryGrayTags);
        }
        return excludeItems;
    }

    private static String buildGrayConsumerSql92Expression(GrayTagItem item, boolean isOriginSubEmpty) {
        Map<String, List<String>> trafficTagMap = new HashMap<>();
        for (Map.Entry<String, String> entry : item.getTrafficTag().entrySet()) {
            buildTrafficTagMap(trafficTagMap, entry);
        }
        StringBuilder builder = new StringBuilder();
        if (trafficTagMap.size() > 1 || (!trafficTagMap.isEmpty() && !isOriginSubEmpty)) {
            builder.append(LEFT_BRACKET);
        }
        for (Map.Entry<String, List<String>> envEntry : trafficTagMap.entrySet()) {
            if (builder.length() > 1) {
                builder.append(" or ");
            }
            builder.append(LEFT_BRACKET)
                    .append(envEntry.getKey())
                    .append(" in ")
                    .append(getStrForSets(new HashSet<>(envEntry.getValue())))
                    .append(RIGHT_BRACKET);
        }
        if (trafficTagMap.size() > 1 || (!trafficTagMap.isEmpty() && !isOriginSubEmpty)) {
            builder.append(RIGHT_BRACKET);
        }
        return builder.toString();
    }

    private static String buildBaseConsumerSql92Expression(List<GrayTagItem> items, boolean isOriginSubEmpty) {
        Map<String, List<String>> trafficTagMap = new HashMap<>();
        for (GrayTagItem item : items) {
            for (Map.Entry<String, String> entry : item.getTrafficTag().entrySet()) {
                buildTrafficTagMap(trafficTagMap, entry);
            }
        }
        StringBuilder builder = new StringBuilder();
        if (trafficTagMap.size() > 1 || (!trafficTagMap.isEmpty() && !isOriginSubEmpty)) {
            builder.append(LEFT_BRACKET);
        }
        for (Map.Entry<String, List<String>> envEntry : trafficTagMap.entrySet()) {
            if (builder.length() > 1) {
                builder.append(AND_SPLICE_STR);
            }
            if (trafficTagMap.size() > 1) {
                builder.append(LEFT_BRACKET);
            }
            builder.append(LEFT_BRACKET)
                    .append(envEntry.getKey())
                    .append(" not in ")
                    .append(getStrForSets(new HashSet<>(envEntry.getValue())))
                    .append(RIGHT_BRACKET)
                    .append(" or ")
                    .append(LEFT_BRACKET)
                    .append(envEntry.getKey())
                    .append(" is null")
                    .append(RIGHT_BRACKET);
            if (trafficTagMap.size() > 1) {
                builder.append(RIGHT_BRACKET);
            }
        }
        if (trafficTagMap.size() > 1 || (!trafficTagMap.isEmpty() && !isOriginSubEmpty)) {
            builder.append(RIGHT_BRACKET);
        }
        return builder.toString();
    }

    private static void buildTrafficTagMap(Map<String, List<String>> trafficTagMap, Map.Entry<String, String> entry) {
        trafficTagMap.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).add(entry.getValue());
    }

    private static String rebuildWithoutGrayTagSubData(String originSubData) {
        if (StringUtils.isBlank(originSubData)) {
            return originSubData;
        }
        String[] originConditions = PATTERN.split(originSubData);
        List<String> refactorConditions = new ArrayList<>();
        for (String condition : originConditions) {
            if (!containsGrayTags(condition) && !condition.contains("_message_tag_")) {
                refactorConditions.add(condition);
            }
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < refactorConditions.size(); i++) {
            sb.append(refactorConditions.get(i));
            if (i != refactorConditions.size() - 1) {
                sb.append(AND_SPLICE_STR);
            }
        }
        return sb.toString();
    }

    private static boolean containsGrayTags(String condition) {
        for (String key : RocketMqConfigUtils.getGrayTagsSet()) {
            if (condition.contains(key)) {
                return true;
            }
        }
        return false;
    }

    /**
     * reset subString with gray tags
     *
     * @param subscriptionData subscriptionData
     * @param subscribeScope subscribeScope
     */
    public static void resetsSql92SubscriptionData(Object subscriptionData, String subscribeScope) {
        String originSubData;
        if (EXPRESSION_TYPE_TAG.equals(RocketMqReflectUtils.getExpressionType(subscriptionData))) {
            originSubData = buildSql92ExpressionByTags(RocketMqReflectUtils.getTagsSet(subscriptionData));
        } else {
            originSubData = RocketMqReflectUtils.getSubString(subscriptionData);
        }
        String newSubStr = addGrayTagsToSql92Expression(originSubData, subscribeScope);
        if (StringUtils.isEmpty(newSubStr)) {
            newSubStr = SELECT_ALL_MESSAGE_SQL;
        }
        if (EXPRESSION_TYPE_TAG.equals(RocketMqReflectUtils.getExpressionType(subscriptionData))) {
            RocketMqReflectUtils.setSubscriptionData(subscriptionData, "setExpressionType",
                    new Class[]{String.class}, new Object[]{"SQL92"});
            RocketMqReflectUtils.getTagsSet(subscriptionData).clear();
            RocketMqReflectUtils.getCodeSet(subscriptionData).clear();
        }
        RocketMqReflectUtils.setSubscriptionData(subscriptionData, "setSubString",
                new Class[]{String.class}, new Object[]{newSubStr});
        RocketMqReflectUtils.setSubscriptionData(subscriptionData, "setSubVersion",
                new Class[]{long.class}, new Object[]{System.currentTimeMillis()});
        LOGGER.warning(String.format(Locale.ENGLISH, "update [key: %s] SQL92 subscriptionData, originSubStr: "
                + "[%s], newSubStr: [%s]", subscribeScope, originSubData, newSubStr));
    }

    /**
     * reset grayTagItems for address@topic@group
     *
     * @param grayTagItems grayTagItems
     * @param clientConfig clientConfig
     */
    public static void resetAutoCheckGrayTagItems(List<GrayTagItem> grayTagItems,
            RocketMqConsumerClientConfig clientConfig) {
        String subscribeScope = buildSubscribeScope(clientConfig.getTopic(), clientConfig.getConsumerGroup(),
                clientConfig.getAddress());
        AUTO_CHECK_GRAY_TAGS.remove(subscribeScope);
        RocketMqConfigUtils.setBaseGroupTagChangeMap(buildSubscribeScope(clientConfig.getTopic(),
                clientConfig.getConsumerGroup(), clientConfig.getAddress()), true);
        if (!grayTagItems.isEmpty()) {
            AUTO_CHECK_GRAY_TAGS.put(subscribeScope, grayTagItems);
        }
    }

    /**
     * using namesrvAddr/topic/consumerGroup build subscribe scope
     *
     * @param topic topic
     * @param consumerGroup consumerGroup
     * @param namesrvAddr namesrvAddr
     * @return subscribeScope
     */
    public static String buildSubscribeScope(String topic, String consumerGroup, String namesrvAddr) {
        String topicTemp = topic.contains(RETYPE) ? StringUtils.substringAfterLast(topic, RETYPE) : topic;
        String consumerGroupTemp = consumerGroup.contains(RETYPE)
                ? StringUtils.substringAfterLast(consumerGroup, RETYPE) : consumerGroup;
        return namesrvAddr + AFA_SYMBOL + topicTemp + AFA_SYMBOL + consumerGroupTemp;
    }

    /**
     * get gray tag change flag
     *
     * @param topic topic
     * @param rebalance rebalance
     * @return changeFlag
     */
    public static boolean getGrayTagChangeFlag(String topic, RebalanceImpl rebalance) {
        String subscribeScope = buildSubscribeScope(topic, rebalance.getConsumerGroup(),
                rebalance.getmQClientFactory().getClientConfig().getNamesrvAddr());
        if (StringUtils.isEmpty(RocketMqGrayscaleConfigUtils.getGrayGroupTag())) {
            return RocketMqConfigUtils.getBaseGroupTagChangeMap(subscribeScope);
        }
        return RocketMqConfigUtils.getGrayGroupTagChangeMap(subscribeScope);
    }

    /**
     * reset gray tag change flag
     *
     * @param namesrvAddr namesrvAddr
     * @param topic topic
     * @param consumerGroup consumerGroup
     * @param flag flag
     */
    public static void resetTagChangeMap(String namesrvAddr, String topic, String consumerGroup, boolean flag) {
        String subscribeScope = buildSubscribeScope(topic, consumerGroup, namesrvAddr);
        if (StringUtils.isEmpty(RocketMqGrayscaleConfigUtils.getGrayGroupTag())) {
            RocketMqConfigUtils.setBaseGroupTagChangeMap(subscribeScope, flag);
        } else {
            RocketMqConfigUtils.setGrayGroupTagChangeMap(subscribeScope, flag);
        }
    }

    /**
     * check expressionType is inaccurate
     *
     * @param expressionType expressionType
     * @return is inaccurate
     */
    public static boolean isExpressionTypeInaccurate(String expressionType) {
        if (!EXPRESSION_TYPE_SQL92.equals(expressionType) && !EXPRESSION_TYPE_TAG.equals(expressionType)) {
            LOGGER.warning(String.format(Locale.ENGLISH, "can not process expressionType: %s", expressionType));
            return true;
        }
        return false;
    }
}
