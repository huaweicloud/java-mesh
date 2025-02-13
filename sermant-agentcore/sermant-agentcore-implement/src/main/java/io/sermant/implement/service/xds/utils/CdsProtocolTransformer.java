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

import io.envoyproxy.envoy.config.cluster.v3.CircuitBreakers;
import io.envoyproxy.envoy.config.cluster.v3.Cluster;
import io.envoyproxy.envoy.config.cluster.v3.Cluster.LbPolicy;
import io.envoyproxy.envoy.config.cluster.v3.OutlierDetection;
import io.sermant.core.service.xds.entity.XdsCluster;
import io.sermant.core.service.xds.entity.XdsInstanceCircuitBreakers;
import io.sermant.core.service.xds.entity.XdsLbPolicy;
import io.sermant.core.service.xds.entity.XdsRequestCircuitBreakers;
import io.sermant.core.service.xds.entity.XdsServiceCluster;
import io.sermant.core.utils.CollectionUtils;
import io.sermant.core.utils.StringUtils;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Convert cds protocol data to Sermant data model
 *
 * @author daizhenyu
 * @since 2024-08-22
 **/
public class CdsProtocolTransformer {
    private static final int CLUSTER_SUBSET_INDEX = 2;

    private static final String VERTICAL_LINE_SEPARATOR = "\\|";

    private static final Map<LbPolicy, XdsLbPolicy> LB_POLICY_MAPPING = new HashMap<>();

    static {
        LB_POLICY_MAPPING.put(LbPolicy.RANDOM, XdsLbPolicy.RANDOM);
        LB_POLICY_MAPPING.put(LbPolicy.ROUND_ROBIN, XdsLbPolicy.ROUND_ROBIN);
        LB_POLICY_MAPPING.put(LbPolicy.LEAST_REQUEST, XdsLbPolicy.LEAST_REQUEST);
        LB_POLICY_MAPPING.put(LbPolicy.RING_HASH, XdsLbPolicy.RING_HASH);
        LB_POLICY_MAPPING.put(LbPolicy.MAGLEV, XdsLbPolicy.MAGLEV);
    }

    private CdsProtocolTransformer() {
    }

    /**
     * get the mapping between service name of k8s and cluster of istio
     *
     * @param clusters clusters
     * @return XdsServiceCluster map
     */
    public static Map<String, XdsServiceCluster> getServiceClusters(List<Cluster> clusters) {
        Map<String, Set<XdsCluster>> xdsClusters = clusters.stream()
                .filter(Objects::nonNull)
                .map(CdsProtocolTransformer::parseCluster)
                .filter(xdsCluster -> !StringUtils.isEmpty(xdsCluster.getServiceName()))
                .collect(Collectors.groupingBy(
                        XdsCluster::getServiceName,
                        Collectors.toSet()
                ));
        Map<String, XdsServiceCluster> xdsServiceClusterMap = new HashMap<>();
        for (Entry<String, Set<XdsCluster>> clusterEntry : xdsClusters.entrySet()) {
            XdsServiceCluster serviceCluster = new XdsServiceCluster();
            serviceCluster.setBaseClusterName(getServiceBaseClusterName(clusterEntry.getValue()));
            Map<String, XdsCluster> clusterMap = clusterEntry.getValue().stream()
                    .collect(Collectors.toMap(
                            XdsCluster::getClusterName,
                            xdsCluster -> xdsCluster
                    ));
            serviceCluster.setClusters(clusterMap);
            xdsServiceClusterMap.put(clusterEntry.getKey(), serviceCluster);
        }
        return xdsServiceClusterMap;
    }

    private static XdsCluster parseCluster(Cluster cluster) {
        XdsCluster xdsCluster = new XdsCluster();
        Optional<String> serviceNameFromCluster = XdsCommonUtils.getServiceNameFromCluster(cluster.getName());
        if (!serviceNameFromCluster.isPresent()) {
            return xdsCluster;
        }
        xdsCluster.setClusterName(cluster.getName());
        xdsCluster.setServiceName(serviceNameFromCluster.get());
        xdsCluster.setLocalityLb(cluster.getCommonLbConfig().hasLocalityWeightedLbConfig());
        xdsCluster.setLbPolicy(parseClusterLbPolicy(cluster.getLbPolicy()));
        xdsCluster.setRequestCircuitBreakers(parseRequestCircuitBreakers(cluster.getCircuitBreakers()));
        xdsCluster.setInstanceCircuitBreakers(parseInstanceCircuitBreakers(cluster));
        return xdsCluster;
    }

    private static String getServiceBaseClusterName(Set<XdsCluster> xdsClusters) {
        for (XdsCluster cluster : xdsClusters) {
            String clusterName = cluster.getClusterName();
            String[] splitCluster = clusterName.split(VERTICAL_LINE_SEPARATOR);
            if (splitCluster[CLUSTER_SUBSET_INDEX].equals(StringUtils.EMPTY)) {
                return clusterName;
            }
        }
        return StringUtils.EMPTY;
    }

    private static XdsLbPolicy parseClusterLbPolicy(LbPolicy lbPolicy) {
        return LB_POLICY_MAPPING.getOrDefault(lbPolicy, XdsLbPolicy.UNRECOGNIZED);
    }

    private static XdsRequestCircuitBreakers parseRequestCircuitBreakers(CircuitBreakers circuitBreakers) {
        XdsRequestCircuitBreakers requestCircuitBreakers = new XdsRequestCircuitBreakers();
        if (!CollectionUtils.isEmpty(circuitBreakers.getThresholdsList())) {
            requestCircuitBreakers.setMaxRequests(circuitBreakers.getThresholds(0).getMaxRequests().getValue());
        }
        return requestCircuitBreakers;
    }

    private static XdsInstanceCircuitBreakers parseInstanceCircuitBreakers(Cluster cluster) {
        OutlierDetection outlierDetection = cluster.getOutlierDetection();
        XdsInstanceCircuitBreakers xdsInstanceCircuitBreakers = new XdsInstanceCircuitBreakers();
        xdsInstanceCircuitBreakers.setSplitExternalLocalOriginErrors(outlierDetection
                .getSplitExternalLocalOriginErrors());
        xdsInstanceCircuitBreakers.setConsecutiveLocalOriginFailure(outlierDetection.getConsecutiveLocalOriginFailure()
                .getValue());
        xdsInstanceCircuitBreakers.setConsecutiveGatewayFailure(outlierDetection.getConsecutiveGatewayFailure()
                .getValue());
        xdsInstanceCircuitBreakers.setConsecutive5xxFailure(outlierDetection.getConsecutive5Xx().getValue());
        xdsInstanceCircuitBreakers.setInterval(getDurationInMillis(outlierDetection.getInterval()));
        xdsInstanceCircuitBreakers.setBaseEjectionTime(getDurationInMillis(outlierDetection.getBaseEjectionTime()));
        xdsInstanceCircuitBreakers.setMaxEjectionPercent(outlierDetection.getMaxEjectionPercent().getValue());
        xdsInstanceCircuitBreakers.setFailurePercentageMinimumHosts(outlierDetection.getFailurePercentageMinimumHosts()
                .getValue());
        xdsInstanceCircuitBreakers.setMinHealthPercent(cluster.getCommonLbConfig().
                getHealthyPanicThreshold().getValue());
        return xdsInstanceCircuitBreakers;
    }

    private static long getDurationInMillis(com.google.protobuf.Duration duration) {
        return Duration.ofSeconds(duration.getSeconds()).toMillis() + Duration.ofNanos(duration.getNanos()).toMillis();
    }
}
