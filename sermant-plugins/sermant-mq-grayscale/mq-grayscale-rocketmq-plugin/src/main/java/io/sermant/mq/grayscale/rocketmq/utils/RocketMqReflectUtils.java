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

import io.sermant.core.utils.ReflectUtils;

import org.apache.rocketmq.client.impl.MQClientAPIImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 5.0.x/5.1.x path difference class reflect utils
 *
 * @author chengyouling
 * @since 2024-12-19
 */
public class RocketMqReflectUtils {
    private static final String GET_GROUP_LIST_METHOD = "getGroupList";

    private static final String GET_BROKER_DATAS_METHOD = "getBrokerDatas";

    private static final String GET_BROKER_ADDRS_METHOD = "getBrokerAddrs";

    private static final String GET_EXPRESSION_TYPE_METHOD = "getExpressionType";

    private static final String GET_TOPIC_METHOD = "getTopic";

    private static final String GET_SUBSTRING_METHOD = "getSubString";

    private static final String GET_TAGS_SET_METHOD = "getTagsSet";

    private static final String GET_CODE_SET_METHOD = "getCodeSet";

    private static final String GET_TOPIC_ROUTE_INFO_METHOD = "getTopicRouteInfoFromNameServer";

    private static final String QUERY_TOPIC_CONSUME_BY_WHO_METHOD = "queryTopicConsumeByWho";

    private RocketMqReflectUtils() {
    }

    /**
     * get groups
     *
     * @param groupList GroupList
     * @return groupList
     */
    public static Set<String> getGroupList(Object groupList) {
        Optional<Object> groupSetsOpt = ReflectUtils.invokeMethodWithNoneParameter(groupList, GET_GROUP_LIST_METHOD);
        return groupSetsOpt.map(groupSets -> (Set<String>) groupSets).orElseGet(HashSet::new);
    }

    /**
     * get brokerDatas
     *
     * @param topicRouteData topicRouteData
     * @return brokerDatas
     */
    public static List<Object> getBrokerDatas(Object topicRouteData) {
        Optional<Object> brokerDatasOpt = ReflectUtils.invokeMethodWithNoneParameter(topicRouteData,
                GET_BROKER_DATAS_METHOD);
        return brokerDatasOpt.map(brokerDatas -> (List<Object>) brokerDatas).orElseGet(ArrayList::new);
    }

    /**
     * get brokerAddrs
     *
     * @param brokerData brokerData
     * @return brokerAddrs
     */
    public static Map<Long, String> getBrokerAddrs(Object brokerData) {
        Optional<Object> brokerAddrsOpt = ReflectUtils.invokeMethodWithNoneParameter(brokerData,
                GET_BROKER_ADDRS_METHOD);
        return brokerAddrsOpt.map(brokerAddrs -> (Map<Long, String>) brokerAddrs).orElseGet(HashMap::new);
    }

    /**
     * get expressionType
     *
     * @param subscriptionData subscriptionData
     * @return expressionType
     */
    public static String getExpressionType(Object subscriptionData) {
        Optional<Object> expressionTypeOpt = ReflectUtils.invokeMethodWithNoneParameter(subscriptionData,
                GET_EXPRESSION_TYPE_METHOD);
        return expressionTypeOpt.map(expressionType -> (String) expressionType).orElse("");
    }

    /**
     * set SubscriptionDatae info
     *
     * @param subscriptionData subscriptionData
     * @param methodName methodName
     * @param paramsType paramsType
     * @param params params
     */
    public static void setSubscriptionData(Object subscriptionData, String methodName, Class<?>[] paramsType,
            Object[] params) {
        ReflectUtils.invokeMethod(subscriptionData, methodName, paramsType, params);
    }

    /**
     * get topic
     *
     * @param subscriptionData subscriptionData
     * @return topic
     */
    public static String getTopic(Object subscriptionData) {
        Optional<Object> topicOpt = ReflectUtils.invokeMethodWithNoneParameter(subscriptionData, GET_TOPIC_METHOD);
        return topicOpt.map(topic -> (String) topic).orElse("");
    }

    /**
     * get subString
     *
     * @param subscriptionData subscriptionData
     * @return subString
     */
    public static String getSubString(Object subscriptionData) {
        Optional<Object> substrOpt = ReflectUtils.invokeMethodWithNoneParameter(subscriptionData, GET_SUBSTRING_METHOD);
        return substrOpt.map(substr -> (String) substr).orElse("");
    }

    /**
     * get tags set
     *
     * @param subscriptionData subscriptionData
     * @return tagsSet
     */
    public static Set<String> getTagsSet(Object subscriptionData) {
        Optional<Object> tagsSetOpt = ReflectUtils.invokeMethodWithNoneParameter(subscriptionData, GET_TAGS_SET_METHOD);
        return tagsSetOpt.map(tagsSet -> (Set<String>) tagsSet).orElse(new HashSet<>());
    }

    /**
     * get code set
     *
     * @param subscriptionData subscriptionData
     * @return codeSet
     */
    public static Set<Integer> getCodeSet(Object subscriptionData) {
        Optional<Object> codeSetOpt = ReflectUtils.invokeMethodWithNoneParameter(subscriptionData, GET_CODE_SET_METHOD);
        return codeSetOpt.map(codeSet -> (Set<Integer>) codeSet).orElse(new HashSet<>());
    }

    /**
     * get topicRouteData
     *
     * @param mqClientApi mqClientApi
     * @param topic topic
     * @param timeout timeout
     * @param allowTopicNotExist allowTopicNotExist
     * @return topicRouteData
     */
    public static Object getTopicRouteInfoFromNameServer(MQClientAPIImpl mqClientApi, String topic, long timeout,
            boolean allowTopicNotExist) {
        Optional<Object> topicRouteData = ReflectUtils.invokeMethod(mqClientApi, GET_TOPIC_ROUTE_INFO_METHOD,
                new Class[]{String.class, long.class, boolean.class}, new Object[]{topic, timeout, allowTopicNotExist});
        return topicRouteData.orElseGet(Object::new);
    }

    /**
     * get groupList
     *
     * @param mqClientApi mqClientApi
     * @param brokerAddress brokerAddress
     * @param topic topic
     * @param timeout timeout
     * @return groupList
     */
    public static Object queryTopicConsumeByWho(MQClientAPIImpl mqClientApi, String brokerAddress, String topic,
            long timeout) {
        Optional<Object> groupList = ReflectUtils.invokeMethod(mqClientApi, QUERY_TOPIC_CONSUME_BY_WHO_METHOD,
                new Class[]{String.class, String.class, long.class}, new Object[]{brokerAddress, topic, timeout});
        return groupList.orElseGet(Object::new);
    }
}
