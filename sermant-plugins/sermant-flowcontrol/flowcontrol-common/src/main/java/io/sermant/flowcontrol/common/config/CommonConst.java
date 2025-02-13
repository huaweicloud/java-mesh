/*
 * Copyright (C) 2021-2022 Huawei Technologies Co., Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.sermant.flowcontrol.common.config;

/**
 * constant class
 *
 * @author zhouss
 * @since 2022-01-28
 */
public class CommonConst {
    /**
     * dubbo gets the downstream service name from the url
     *
     * @see org.apache.dubbo.common.URL#getParameter(String)
     * @see com.alibaba.dubbo.common.URL#getParameter(String)
     */
    public static final String DUBBO_REMOTE_APPLICATION = "remote.application";

    /**
     * dubbo gets the current service name from the url
     *
     * @see org.apache.dubbo.common.URL#getParameter(String)
     * @see com.alibaba.dubbo.common.URL#getParameter(String)
     */
    public static final String DUBBO_APPLICATION = "application";

    /**
     * dubbo interface
     */
    public static final String DUBBO_INTERFACE = "interface";

    /**
     * version key name obtained from url
     */
    public static final String URL_VERSION_KEY = "version";

    /**
     * The actual interface of the generalized interface can be obtained from the url
     */
    public static final String GENERIC_INTERFACE_KEY = "interface";

    /**
     * generalized method name
     */
    public static final String GENERIC_METHOD_NAME = "$invoke";

    /**
     * apache dubbo generalize the interface class name
     */
    public static final String APACHE_DUBBO_GENERIC_SERVICE_CLASS = "org.apache.dubbo.rpc.service.GenericService";

    /**
     * alibaba dubbo generalize the interface class name
     */
    public static final String ALIBABA_DUBBO_GENERIC_SERVICE_CLASS = "com.alibaba.dubbo.rpc.service.GenericService";

    /**
     * dubbo client
     */
    public static final String DUBBO_CONSUMER = "consumer";

    /**
     * dubbo server
     */
    public static final String DUBBO_PROVIDER = "provider";

    /**
     * distinguish dubbo callers: provider or consumer
     */
    public static final String DUBBO_SIDE = "side";

    /**
     * flow control configuration key
     */
    public static final String FLOW_RULE_CONFIG_KEY = "FlowRule";

    /**
     * circuit breaker configuration key
     */
    public static final String BREAKER_RULE_CONFIG_KEY = "DegradeRule";

    /**
     * isolation configuration key
     */
    public static final String BULK_RULE_CONFIG_KEY = "IsolateRule";

    /**
     * retry configuration key
     */
    public static final String RETRY_RULE_CONFIG_KEY = "RetryRule";

    /**
     * permission rule configuration key
     */
    public static final String AUTHORITY_RULE_CONFIG_KEY = "AuthorityRule";

    /**
     * system rule configuration key
     */
    public static final String SYSTEM_RULE_CONFIG_KEY = "SystemRule";

    /**
     * http too many request exception codes
     */
    public static final int TOO_MANY_REQUEST_CODE = 429;

    /**
     * instance isolation status code
     */
    public static final int INSTANCE_ISOLATION_REQUEST_CODE = 503;

    /**
     * service exception
     */
    public static final int INTERVAL_SERVER_ERROR = 500;

    /**
     * request normal response
     */
    public static final int HTTP_OK = 200;

    /**
     * empty string
     */
    public static final String EMPTY_STR = "";

    /**
     * name of the periodically executed thread pool
     */
    public static final String SENTINEL_SEND_CFC_TASK = "sentinel-send-cfc-task";

    /**
     * Thread pool startup delays the time of the first execution
     */
    public static final int INITIAL_DELAY = 5000;

    /**
     * Whether to back up rules to redis, default backup, console parameter transfer true is no backup
     */
    public static final String REDIS_RULE_STORE = "redis.rule.store";

    /**
     * retry times
     */
    public static final int RETRY_TIMES = 3;

    /**
     * retry interval
     */
    public static final int SLEEP_TIME = 1000;

    /**
     * flowcontrol rule
     */
    public static final String SENTINEL_RULE_FLOW = "flow";

    /**
     * downgrade rule
     */
    public static final String SENTINEL_RULE_DEGRADE = "degrade";

    /**
     * authorization rule
     */
    public static final String SENTINEL_RULE_AUTHORITY = "authority";

    /**
     * system rule
     */
    public static final String SENTINEL_RULE_SYSTEM = "system";

    /**
     * slash symbol
     */
    public static final String SLASH_SIGN = "/";

    /**
     * sentinel configuration parameter, default interval for sending heartbeat packets，unitMillisecond
     */
    public static final long FLOW_CONTROL_HEARTBEAT_INTERVAL = 10000L;

    /**
     * sentinel configuration parameter, default interval for sending flow control information，unitMillisecond
     */
    public static final long FLOW_CONTROL_METRIC_INTERVAL = 1000L;

    /**
     * sentinel configuration parameter, Specifies the period during
     * which flow control information data is initially loaded after startup
     */
    public static final long METRIC_INITIAL_DURATION = 60000L;

    /**
     * sentinel configuration parameter,the default number of data loaded at the end time of querying flow control
     * information is not provided
     */
    public static final long METRIC_MAX_LINE = 12000L;

    /**
     * sentinel configuration parameter, when querying flow control data, sleep for a period of time
     * and wait until the flow limiting data is written into the file
     */
    public static final long METRIC_SLEEP_TIME = 2000L;

    /**
     * kafka configuration parameter, Control the maximum size of requests sent by the producer. The default is 1 M
     * （This parameter is related to the message.max.bytes parameter of the Kafka host）
     */
    public static final long KAFKA_MAX_REQUEST_SIZE = 1048576L;

    /**
     * kafka configuration parameter, producer memory buffer size 32m
     */
    public static final long KAFKA_BUFFER_MEMORY = 33554432L;

    /**
     * kafka configuration parameter, The maximum time the client will wait for a response to the request
     */
    public static final long KAFKA_REQUEST_TIMEOUT_MS = 10000L;

    /**
     * kafka configuration parameter, Maximum blocking time, beyond which an exception is thrown
     */
    public static final long KAFKA_MAX_BLOCK_MS = 60000L;

    /**
     * default indicator sending interval
     */
    public static final long DEFAULT_METRIC_SEND_INTERVAL_MS = 1000L;

    /**
     * second to millisecond conversion unit
     */
    public static final long S_MS_UNIT = 1000L;

    /**
     * a unit of seconds to minutes
     */
    public static final long S_M_UNIT = 60L;

    /**
     * percentage
     */
    public static final double PERCENT = 100.0d;

    /**
     * traffic limiting request number conversion
     */
    public static final double RATE_DIV_POINT = 1000.0d;

    /**
     * record the request time key
     */
    public static final String REQUEST_START_TIME = "requestStartTime";

    /**
     * the connect for request address
     */
    public static final String CONNECT = ":";

    /**
     * point
     */
    public static final String ESCAPED_POINT = "\\.";

    /**
     * Default response status code
     */
    public static final int DEFAULT_RESPONSE_CODE = -1;

    /**
     * the key of Scenario information for flow control
     */
    public static final String SCENARIO_INFO = "flowControlScenario";

    /**
     * the key of request-information
     */
    public static final String REQUEST_INFO = "REQUEST_INFO";

    /**
     * the default contentType
     */
    public static final String DEFAULT_CONTENT_TYPE = "text/plain";

    /**
     * Minimum response code for a successful request
     */
    public static final int MIN_SUCCESS_STATUS_CODE = 200;

    /**
     * Maximum response code for a successful request
     */
    public static final int MAX_SUCCESS_STATUS_CODE = 399;

    /**
     * Retry condition based on the result
     */
    public static final int RETRY_CONDITION_BY_RESULT = 0;

    /**
     * Retry condition based on the exception
     */
    public static final int RETRY_CONDITION_BY_EXCEPTION = 1;

    /**
     * Retry condition based on both the status code and the exception
     */
    public static final int RETRY_CONDITION_BY_STATUS_AND_EXCEPTION = 2;

    private CommonConst() {
    }
}
