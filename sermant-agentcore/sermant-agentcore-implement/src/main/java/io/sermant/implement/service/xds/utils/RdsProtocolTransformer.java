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

package io.sermant.implement.service.xds.utils;

import com.github.udpa.udpa.type.v1.TypedStruct;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.ListValue;
import com.google.protobuf.Message;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;

import io.envoyproxy.envoy.config.route.v3.HeaderMatcher;
import io.envoyproxy.envoy.config.route.v3.RetryPolicy;
import io.envoyproxy.envoy.config.route.v3.Route;
import io.envoyproxy.envoy.config.route.v3.RouteAction;
import io.envoyproxy.envoy.config.route.v3.RouteConfiguration;
import io.envoyproxy.envoy.config.route.v3.RouteMatch;
import io.envoyproxy.envoy.config.route.v3.VirtualHost;
import io.envoyproxy.envoy.config.route.v3.WeightedCluster;
import io.envoyproxy.envoy.config.route.v3.WeightedCluster.ClusterWeight;
import io.envoyproxy.envoy.extensions.filters.common.fault.v3.FaultDelay;
import io.envoyproxy.envoy.extensions.filters.http.fault.v3.FaultAbort;
import io.envoyproxy.envoy.extensions.filters.http.fault.v3.HTTPFault;
import io.envoyproxy.envoy.type.matcher.v3.StringMatcher;
import io.envoyproxy.envoy.type.v3.FractionalPercent;
import io.sermant.core.common.CommonConstant;
import io.sermant.core.common.LoggerFactory;
import io.sermant.core.service.xds.entity.XdsAbort;
import io.sermant.core.service.xds.entity.XdsDelay;
import io.sermant.core.service.xds.entity.XdsHeader;
import io.sermant.core.service.xds.entity.XdsHeaderMatcher;
import io.sermant.core.service.xds.entity.XdsHeaderOption;
import io.sermant.core.service.xds.entity.XdsHttpFault;
import io.sermant.core.service.xds.entity.XdsPathMatcher;
import io.sermant.core.service.xds.entity.XdsRateLimit;
import io.sermant.core.service.xds.entity.XdsRetryPolicy;
import io.sermant.core.service.xds.entity.XdsRoute;
import io.sermant.core.service.xds.entity.XdsRouteAction;
import io.sermant.core.service.xds.entity.XdsRouteAction.XdsClusterWeight;
import io.sermant.core.service.xds.entity.XdsRouteAction.XdsWeightedClusters;
import io.sermant.core.service.xds.entity.XdsRouteConfiguration;
import io.sermant.core.service.xds.entity.XdsRouteMatch;
import io.sermant.core.service.xds.entity.XdsTokenBucket;
import io.sermant.core.service.xds.entity.XdsVirtualHost;
import io.sermant.core.service.xds.entity.match.ExactMatchStrategy;
import io.sermant.core.service.xds.entity.match.PrefixMatchStrategy;
import io.sermant.core.service.xds.entity.match.PresentMatchStrategy;
import io.sermant.core.service.xds.entity.match.RegexMatchStrategy;
import io.sermant.core.service.xds.entity.match.SuffixMatchStrategy;
import io.sermant.core.service.xds.entity.match.UnknownMatchStrategy;
import io.sermant.core.utils.StringUtils;
import io.sermant.implement.service.xds.constants.XdsFilterConstant;
import io.sermant.implement.service.xds.entity.DenominatorType;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Convert rds protocol data to Sermant data model
 *
 * @author daizhenyu
 * @since 2024-08-22
 **/
public class RdsProtocolTransformer {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    private static final String POINT_SEPARATOR = "\\.";

    private RdsProtocolTransformer() {
    }

    /**
     * get XdsRouteConfiguration list
     *
     * @param routeConfigurations RouteConfiguration list
     * @return XdsRouteConfiguration list
     */
    public static List<XdsRouteConfiguration> getRouteConfigurations(List<RouteConfiguration> routeConfigurations) {
        return routeConfigurations.stream()
                .filter(Objects::nonNull)
                .map(RdsProtocolTransformer::parseRouteConfiguration)
                .collect(Collectors.toList());
    }

    private static XdsRouteConfiguration parseRouteConfiguration(RouteConfiguration routeConfiguration) {
        XdsRouteConfiguration xdsRouteConfiguration = new XdsRouteConfiguration();
        xdsRouteConfiguration.setRouteConfigName(routeConfiguration.getName());
        Map<String, XdsVirtualHost> xdsVirtualHostMap = routeConfiguration.getVirtualHostsList().stream()
                .map(RdsProtocolTransformer::parseVirtualHost)
                .collect(Collectors.toMap(
                        xdsVirtualHost -> xdsVirtualHost.getName().split(POINT_SEPARATOR)[0],
                        xdsVirtualHost -> xdsVirtualHost
                ));
        xdsRouteConfiguration.setVirtualHosts(xdsVirtualHostMap);
        return xdsRouteConfiguration;
    }

    private static XdsVirtualHost parseVirtualHost(VirtualHost virtualHost) {
        XdsVirtualHost xdsVirtualHost = new XdsVirtualHost();
        List<String> domains = virtualHost.getDomainsList();
        List<XdsRoute> xdsRoutes =
                virtualHost.getRoutesList().stream()
                        .map(RdsProtocolTransformer::parseRoute)
                        .collect(Collectors.toList());
        xdsVirtualHost.setName(virtualHost.getName());
        xdsVirtualHost.setRoutes(xdsRoutes);
        xdsVirtualHost.setDomains(domains);
        return xdsVirtualHost;
    }

    private static XdsRoute parseRoute(Route route) {
        XdsRoute xdsRoute = new XdsRoute();
        xdsRoute.setName(route.getName());
        xdsRoute.setRouteMatch(parseRouteMatch(route.getMatch()));
        xdsRoute.setRouteAction(parseRouteAction(route.getRoute()));
        for (Map.Entry<String, Any> entry : route.getTypedPerFilterConfigMap().entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            if (StringUtils.equals(entry.getKey(), XdsFilterConstant.HTTP_FAULT_FILTER_NAME)) {
                Optional<XdsHttpFault> optional = unpackAndParseFilter(entry.getValue(), HTTPFault.class,
                        RdsProtocolTransformer::parseHttpFault);
                optional.ifPresent(xdsRoute::setHttpFault);
                continue;
            }
            if (StringUtils.equals(entry.getKey(), XdsFilterConstant.LOCAL_RATE_LIMIT_FILTER_FILTER_NAME)) {
                Optional<XdsRateLimit> optional = unpackAndParseFilter(entry.getValue(), TypedStruct.class,
                        RdsProtocolTransformer::parseRateLimit);
                optional.ifPresent(xdsRoute::setRateLimit);
            }
        }
        return xdsRoute;
    }

    private static XdsRouteMatch parseRouteMatch(RouteMatch routeMatch) {
        XdsRouteMatch xdsRouteMatch = new XdsRouteMatch();
        xdsRouteMatch.setPathMatcher(parsePathMatcher(routeMatch));
        xdsRouteMatch.setHeaderMatchers(parseHeaderMatchers(routeMatch.getHeadersList()));
        xdsRouteMatch.setCaseSensitive(routeMatch.getCaseSensitive().getValue());
        return xdsRouteMatch;
    }

    private static List<XdsHeaderMatcher> parseHeaderMatchers(List<HeaderMatcher> headerMatchers) {
        return headerMatchers.stream()
                .filter(Objects::nonNull)
                .map(RdsProtocolTransformer::parseHeaderMatcher)
                .collect(Collectors.toList());
    }

    private static XdsHeaderMatcher parseHeaderMatcher(HeaderMatcher headerMatcher) {
        if (headerMatcher.getPresentMatch()) {
            return new XdsHeaderMatcher(headerMatcher.getName(),
                    new PresentMatchStrategy());
        }
        if (!headerMatcher.hasStringMatch()) {
            LOGGER.log(Level.WARNING,
                    "The xDS route header matching strategy is unknown. Please check the route configuration.");
            return new XdsHeaderMatcher(headerMatcher.getName(),
                    new UnknownMatchStrategy());
        }
        StringMatcher stringMatch = headerMatcher.getStringMatch();
        switch (stringMatch.getMatchPatternCase()) {
            case EXACT:
                return new XdsHeaderMatcher(headerMatcher.getName(),
                        new ExactMatchStrategy(stringMatch.getExact()));
            case PREFIX:
                return new XdsHeaderMatcher(headerMatcher.getName(),
                        new PrefixMatchStrategy(stringMatch.getPrefix()));
            case SUFFIX:
                return new XdsHeaderMatcher(headerMatcher.getName(),
                        new SuffixMatchStrategy(stringMatch.getSuffix()));
            case SAFE_REGEX:
                return new XdsHeaderMatcher(headerMatcher.getName(),
                        new RegexMatchStrategy(stringMatch.getSafeRegex().getRegex()));
            default:
                LOGGER.log(Level.WARNING,
                        "The xDS route header matching strategy is unknown. Please check the route configuration.");
                return new XdsHeaderMatcher(headerMatcher.getName(),
                        new UnknownMatchStrategy());
        }
    }

    private static XdsPathMatcher parsePathMatcher(RouteMatch routeMatch) {
        boolean caseSensitive = routeMatch.getCaseSensitive().getValue();
        switch (routeMatch.getPathSpecifierCase()) {
            case PATH:
                String path = routeMatch.getPath();
                return new XdsPathMatcher(new ExactMatchStrategy(caseSensitive ? path : path.toLowerCase(Locale.ROOT)),
                        caseSensitive);
            case PREFIX:
                String prefix = routeMatch.getPrefix();
                return new XdsPathMatcher(
                        new PrefixMatchStrategy(caseSensitive ? prefix : prefix.toLowerCase(Locale.ROOT)),
                        caseSensitive);
            default:
                LOGGER.log(Level.WARNING,
                        "The xDS route path matching strategy is unknown. Please check the route configuration.");
                return new XdsPathMatcher(new UnknownMatchStrategy(), caseSensitive);
        }
    }

    private static XdsRouteAction parseRouteAction(RouteAction routeAction) {
        XdsRouteAction xdsRouteAction = new XdsRouteAction();
        switch (routeAction.getClusterSpecifierCase()) {
            case CLUSTER:
                xdsRouteAction.setCluster(routeAction.getCluster());
                break;
            case WEIGHTED_CLUSTERS:
                xdsRouteAction.setWeighted(true);
                xdsRouteAction.setWeightedClusters(parseWeightedClusters(routeAction.getWeightedClusters()));
                break;
            default:
                LOGGER.log(Level.WARNING,
                        "The xDS route action strategy is unknown. Please check the route configuration.");
        }
        xdsRouteAction.setRetryPolicy(parseRetryPolicy(routeAction.getRetryPolicy()));
        return xdsRouteAction;
    }

    private static XdsWeightedClusters parseWeightedClusters(WeightedCluster clusters) {
        XdsWeightedClusters xdsWeightedClusters = new XdsWeightedClusters();
        xdsWeightedClusters.setTotalWeight(clusters.getClustersList().stream()
                .filter(Objects::nonNull)
                .mapToInt(clusterWeight -> clusterWeight.getWeight().getValue())
                .sum()
        );
        xdsWeightedClusters.setClusters(clusters.getClustersList().stream()
                .filter(Objects::nonNull)
                .map(RdsProtocolTransformer::parseClusterWeight)
                .collect(Collectors.toList()));
        return xdsWeightedClusters;
    }

    private static XdsClusterWeight parseClusterWeight(ClusterWeight clusterWeight) {
        XdsClusterWeight xdsClusterWeight = new XdsClusterWeight();
        xdsClusterWeight.setWeight(clusterWeight.getWeight().getValue());
        xdsClusterWeight.setClusterName(clusterWeight.getName());
        return xdsClusterWeight;
    }

    private static XdsRetryPolicy parseRetryPolicy(RetryPolicy retryPolicy) {
        XdsRetryPolicy xdsRetryPolicy = new XdsRetryPolicy();
        if (!StringUtils.isEmpty(retryPolicy.getRetryOn())) {
            xdsRetryPolicy.setRetryConditions(Arrays.asList(retryPolicy.getRetryOn().split(CommonConstant.COMMA)));
        }
        xdsRetryPolicy.setMaxAttempts(retryPolicy.getNumRetries().getValue());
        xdsRetryPolicy.setPerTryTimeout(getDurationInMillis(retryPolicy.getPerTryTimeout()));
        return xdsRetryPolicy;
    }

    private static <T extends Message, R> Optional<R> unpackAndParseFilter(Any value, Class<T> clazz,
            Function<T, R> parser) {
        try {
            T filter = value.unpack(clazz);
            return Optional.of(parser.apply(filter));
        } catch (InvalidProtocolBufferException e) {
            LOGGER.log(Level.SEVERE, "Failed to unpack and parse filter of type: " + clazz.getName(), e);
        }
        return Optional.empty();
    }

    private static XdsHttpFault parseHttpFault(HTTPFault httpFault) {
        XdsHttpFault xdsHttpFault = new XdsHttpFault();
        xdsHttpFault.setAbort(parseAbort(httpFault.getAbort()));
        xdsHttpFault.setDelay(parseDelay(httpFault.getDelay()));
        return xdsHttpFault;
    }

    private static XdsAbort parseAbort(FaultAbort faultAbort) {
        XdsAbort xdsAbort = new XdsAbort();
        io.sermant.core.service.xds.entity.FractionalPercent fractionalPercent =
                new io.sermant.core.service.xds.entity.FractionalPercent();
        fractionalPercent.setNumerator(faultAbort.getPercentage().getNumerator());
        fractionalPercent.setDenominator(DenominatorType.getValueByName(faultAbort.getPercentage()
                .getDenominator().name()));
        xdsAbort.setPercentage(fractionalPercent);
        xdsAbort.setHttpStatus(faultAbort.getHttpStatus());
        return xdsAbort;
    }

    private static XdsDelay parseDelay(FaultDelay faultDelay) {
        XdsDelay xdsDelay = new XdsDelay();
        xdsDelay.setFixedDelay(getDurationInMillis(faultDelay.getFixedDelay()));
        io.sermant.core.service.xds.entity.FractionalPercent fractionalPercent =
                new io.sermant.core.service.xds.entity.FractionalPercent();
        fractionalPercent.setNumerator(faultDelay.getPercentage().getNumerator());
        fractionalPercent.setDenominator(DenominatorType.getValueByName(faultDelay.getPercentage()
                .getDenominator().name()));
        xdsDelay.setPercentage(fractionalPercent);
        return xdsDelay;
    }

    /**
     * Parse Rate limiting configuration information
     * The original message format of LocalRateLimit can be found in the link:
     * <a href="https://istio.io/latest/docs/tasks/policy-enforcement/rate-limit/#local-rate-limit">LocalRateLimit</a>
     * The format of some of the obtained LocalRateLimit messages is as follows:
     * fields {
     *   key: "filter_enabled"
     *   value {
     *     struct_value {
     *       fields {
     *         key: "default_value"
     *         value {
     *           struct_value {
     *             fields {
     *               key: "denominator"
     *               value {
     *                 string_value: "HUNDRED"
     *               }
     *             }
     *             fields {
     *               key: "numerator"
     *               value {
     *                 number_value: 100.0
     *               }
     *             }
     *           }
     *         }
     *       }
     *     }
     *   }
     * }
     *
     * @param typedStruct Serial protocol buffer message for rate limiting configuration
     * @return Rate limiting configuration information
     */
    private static XdsRateLimit parseRateLimit(TypedStruct typedStruct) {
        XdsRateLimit xdsRateLimit = new XdsRateLimit();
        Struct struct = typedStruct.getValue();
        if (struct.containsFields(XdsFilterConstant.TOKEN_BUCKET)) {
            Optional<XdsTokenBucket> optionalTokenBucket =
                    parseTokenBucket(struct.getFieldsOrThrow(XdsFilterConstant.TOKEN_BUCKET).getStructValue());
            optionalTokenBucket.ifPresent(xdsRateLimit::setTokenBucket);
        }
        if (struct.containsFields(XdsFilterConstant.FILTER_ENFORCED)) {
            Optional<io.sermant.core.service.xds.entity.FractionalPercent> optionalXdsFractionalPercent =
                    parseRuntimeFractionalPercent(struct.getFieldsOrThrow(XdsFilterConstant.FILTER_ENFORCED)
                            .getStructValue());
            optionalXdsFractionalPercent.ifPresent(xdsRateLimit::setPercent);
        }
        if (struct.containsFields(XdsFilterConstant.FILTER_ENABLED)
                && (xdsRateLimit.getPercent() == null || xdsRateLimit.getPercent().getNumerator() == 0)) {
            Optional<io.sermant.core.service.xds.entity.FractionalPercent> optionalXdsFractionalPercent =
                    parseRuntimeFractionalPercent(struct.getFieldsOrThrow(XdsFilterConstant.FILTER_ENABLED)
                            .getStructValue());
            optionalXdsFractionalPercent.ifPresent(xdsRateLimit::setPercent);
        }
        List<XdsHeaderOption> responseHeaders = parseHeaderValueOptions(struct);
        xdsRateLimit.setResponseHeaderOption(responseHeaders);
        return xdsRateLimit;
    }

    private static List<XdsHeaderOption> parseHeaderValueOptions(Struct struct) {
        if (!struct.containsFields(XdsFilterConstant.RESPONSE_HEADERS_TO_ADD)) {
            return Collections.emptyList();
        }
        List<XdsHeaderOption> responseHeaders = new ArrayList<>();
        ListValue headers = struct.getFieldsOrThrow(XdsFilterConstant.RESPONSE_HEADERS_TO_ADD).getListValue();
        for (Value value : headers.getValuesList()) {
            Map<String, Value> headersMap = value.getStructValue().getFieldsMap();
            if (headersMap.get(XdsFilterConstant.HEADER) == null) {
                continue;
            }
            Struct headerStruct = headersMap.get(XdsFilterConstant.HEADER).getStructValue();
            XdsHeaderOption responseHeader = new XdsHeaderOption();
            if (headersMap.get(XdsFilterConstant.APPEND) != null) {
                responseHeader.setEnabledAppend(headersMap.get(XdsFilterConstant.APPEND).getBoolValue());
            }
            if (!headerStruct.containsFields(XdsFilterConstant.HEADER_KEY)) {
                continue;
            }
            XdsHeader xdsHeader = new XdsHeader();
            xdsHeader.setKey(headerStruct.getFieldsOrThrow(XdsFilterConstant.HEADER_KEY).getStringValue());
            if (headerStruct.containsFields(XdsFilterConstant.HEADER_VALUE)) {
                xdsHeader.setValue(headerStruct.getFieldsOrThrow(XdsFilterConstant.HEADER_VALUE).getStringValue());
            }
            responseHeader.setHeader(xdsHeader);
            responseHeaders.add(responseHeader);
        }
        return responseHeaders;
    }

    private static Optional<io.sermant.core.service.xds.entity.FractionalPercent> parseRuntimeFractionalPercent(
            Struct filterEnabledStruct) {
        Map<String, Value> valueMap = filterEnabledStruct.getFieldsMap();
        Value defaultValue = valueMap.get(XdsFilterConstant.DEFAULT_VALUE);
        if (defaultValue == null) {
            return Optional.empty();
        }
        Struct defaultValueStruct = defaultValue.getStructValue();
        Map<String, Value> defaultValueMap = defaultValueStruct.getFieldsMap();
        if (defaultValueMap.get(XdsFilterConstant.DENOMINATOR) == null
                || defaultValueMap.get(XdsFilterConstant.NUMERATOR) == null) {
            return Optional.empty();
        }
        io.sermant.core.service.xds.entity.FractionalPercent fractionalPercent =
                new io.sermant.core.service.xds.entity.FractionalPercent();
        fractionalPercent.setNumerator((int) defaultValueMap.get(XdsFilterConstant.NUMERATOR).getNumberValue());
        FractionalPercent.DenominatorType type = FractionalPercent.DenominatorType.valueOf(
                defaultValueMap.get(XdsFilterConstant.DENOMINATOR).getStringValue());
        fractionalPercent.setDenominator(DenominatorType.getValueByName(type.name()));
        return Optional.of(fractionalPercent);
    }

    private static Optional<XdsTokenBucket> parseTokenBucket(Struct tokenBucketStruct) {
        Map<String, Value> fieldMap = tokenBucketStruct.getFieldsMap();
        if (fieldMap.get(XdsFilterConstant.TOKENS_PER_FILL) == null
                || fieldMap.get(XdsFilterConstant.FILL_INTERVAL) == null
                || fieldMap.get(XdsFilterConstant.MAX_TOKENS) == null) {
            return Optional.empty();
        }
        double tokensPerFill = fieldMap.get(XdsFilterConstant.TOKENS_PER_FILL).getNumberValue();
        String timeStr = XdsFilterConstant.PARSE_TIME_PREFIX + fieldMap.get(XdsFilterConstant.FILL_INTERVAL)
                .getStringValue();
        long fillInterval = Duration.parse(timeStr).toMillis();
        XdsTokenBucket xdsTokenBucket = new XdsTokenBucket();
        xdsTokenBucket.setFillInterval(fillInterval);
        xdsTokenBucket.setMaxTokens((int) fieldMap.get(XdsFilterConstant.MAX_TOKENS).getNumberValue());
        xdsTokenBucket.setTokensPerFill((int) tokensPerFill);
        return Optional.of(xdsTokenBucket);
    }

    private static long getDurationInMillis(com.google.protobuf.Duration duration) {
        return Duration.ofSeconds(duration.getSeconds()).toMillis() + Duration.ofNanos(duration.getNanos()).toMillis();
    }
}
