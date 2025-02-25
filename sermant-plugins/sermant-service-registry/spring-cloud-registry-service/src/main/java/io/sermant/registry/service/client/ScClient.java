/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at Copyright (C) 2022-2022 Huawei Technologies Co., Ltd. All rights
 * reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 */

package io.sermant.registry.service.client;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import io.sermant.core.common.LoggerFactory;
import io.sermant.core.plugin.common.PluginConstant;
import io.sermant.core.plugin.common.PluginSchemaValidator;
import io.sermant.core.plugin.config.PluginConfigManager;
import io.sermant.core.utils.JarFileUtils;
import io.sermant.core.utils.StringUtils;
import io.sermant.registry.config.ConfigConstants;
import io.sermant.registry.config.RegisterConfig;
import io.sermant.registry.config.RegisterServiceCommonConfig;
import io.sermant.registry.config.grace.GraceContext;
import io.sermant.registry.context.RegisterContext;
import io.sermant.registry.service.client.ScDiscovery.SubscriptionKey;
import io.sermant.registry.service.register.Register;
import io.sermant.registry.utils.HostUtils;

import org.apache.servicecomb.foundation.ssl.SSLCustom;
import org.apache.servicecomb.foundation.ssl.SSLOption;
import org.apache.servicecomb.http.client.common.HttpConfiguration;
import org.apache.servicecomb.http.client.common.HttpConfiguration.SSLProperties;
import org.apache.servicecomb.service.center.client.AddressManager;
import org.apache.servicecomb.service.center.client.RegistrationEvents.HeartBeatEvent;
import org.apache.servicecomb.service.center.client.RegistrationEvents.MicroserviceInstanceRegistrationEvent;
import org.apache.servicecomb.service.center.client.RegistrationEvents.MicroserviceRegistrationEvent;
import org.apache.servicecomb.service.center.client.ServiceCenterClient;
import org.apache.servicecomb.service.center.client.ServiceCenterOperation;
import org.apache.servicecomb.service.center.client.ServiceCenterRegistration;
import org.apache.servicecomb.service.center.client.exception.OperationException;
import org.apache.servicecomb.service.center.client.model.DataCenterInfo;
import org.apache.servicecomb.service.center.client.model.Framework;
import org.apache.servicecomb.service.center.client.model.HealthCheck;
import org.apache.servicecomb.service.center.client.model.HealthCheckMode;
import org.apache.servicecomb.service.center.client.model.Microservice;
import org.apache.servicecomb.service.center.client.model.MicroserviceInstance;
import org.apache.servicecomb.service.center.client.model.MicroserviceInstanceStatus;
import org.apache.servicecomb.service.center.client.model.MicroserviceInstancesResponse;
import org.apache.servicecomb.service.center.client.model.MicroserviceStatus;
import org.apache.servicecomb.service.center.client.model.MicroservicesResponse;
import org.apache.servicecomb.service.center.client.model.ServiceCenterConfiguration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.jar.JarFile;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Register a service instance based on a registration task
 *
 * @author zhouss
 * @since 2022-02-25
 */
public class ScClient {
    /**
     * Log
     */
    private static final Logger LOGGER = LoggerFactory.getLogger();

    /**
     * Event stack
     */
    private static final EventBus EVENT_BUS = new EventBus();

    /**
     * http URL prefix
     */
    private static final String HTTP_URL_PREFIX = "http://";

    /**
     * https URL prefix
     */
    private static final String HTTPS_URL_PREFIX = "https://";

    private static final int FLAG = -1;

    private static final int MAX_HOST_NAME_LENGTH = 64;

    private ServiceCenterConfiguration serviceCenterConfiguration;

    private ServiceCenterClient serviceCenterClient;

    private RegisterConfig registerConfig;

    private Microservice microservice;

    private MicroserviceInstance microserviceInstance;

    private ScDiscovery serviceCenterDiscovery;

    private ServiceCenterRegistration serviceCenterRegistration;

    private RegisterServiceCommonConfig commonConfig;

    /**
     * Initialize
     */
    public void init() {
        registerConfig = PluginConfigManager.getPluginConfig(RegisterConfig.class);
        commonConfig = PluginConfigManager.getPluginConfig(RegisterServiceCommonConfig.class);
        initScClientAndWatch();
        initServiceCenterConfiguration();
    }

    /**
     * Service instance registration
     */
    public void register() {
        startServiceCenterRegistration();
    }

    /**
     * Offline processing
     *
     * @throws OperationException An exception is thrown if the deletion fails
     */
    public void deRegister() {
        if (this.serviceCenterRegistration != null) {
            this.serviceCenterRegistration.stop();
        }
        if (serviceCenterDiscovery != null) {
            serviceCenterDiscovery.stop();
        }
        if (serviceCenterClient != null) {
            serviceCenterClient.deleteMicroserviceInstance(this.microservice.getServiceId(),
                    this.microserviceInstance.getInstanceId());
        }
    }

    /**
     * Get ServiceComb status
     *
     * @return UP DOWN
     */
    public String getRegisterCenterStatus() {
        try {
            final MicroserviceInstancesResponse response = serviceCenterClient.getServiceCenterInstances();
            if (response == null || response.getInstances() == null) {
                return Register.DOWN;
            }
            return response.getInstances().isEmpty() ? Register.DOWN : Register.UP;
        } catch (OperationException exception) {
            LOGGER.warning("Service Center is not available!");
        }
        return Register.DOWN;
    }

    /**
     * Obtain the status of the currently registered instance
     *
     * @return Instance status
     */
    public String getInstanceStatus() {
        try {
            MicroserviceInstance instance = serviceCenterClient.getMicroserviceInstance(
                    microserviceInstance.getServiceId(), microserviceInstance.getInstanceId());
            final String status = instance.getStatus() != null ? instance.getStatus().name() : Register.UN_KNOWN;
            RegisterContext.INSTANCE.getClientInfo().setStatus(status);
            return status;
        } catch (OperationException ex) {
            LOGGER.severe(String.format(Locale.ENGLISH, "[RegistryPlugin] failed to get instance status, %s",
                    ex.getMessage()));
        }
        return Register.DOWN;
    }

    /**
     * Update the current instance status
     *
     * @param status Target status
     */
    public void updateInstanceStatus(String status) {
        try {
            serviceCenterClient.updateMicroserviceInstanceStatus(microserviceInstance.getServiceId(),
                    microserviceInstance.getInstanceId(), MicroserviceInstanceStatus.valueOf(status));
            RegisterContext.INSTANCE.getClientInfo().setStatus(status);
        } catch (OperationException ex) {
            LOGGER.severe(
                    String.format(Locale.ENGLISH, "[RegistryPlugin] Updated instance status to %s failed", status));
        }
    }

    /**
     * Query all instances
     *
     * @param serviceName Service name
     * @return List of instances
     */
    public List<MicroserviceInstance> queryInstancesByServiceId(String serviceName) {
        List<MicroserviceInstance> instances = null;
        try {
            if (registerConfig.isAllowCrossApp()) {
                instances = queryAllAppInstances(serviceName);
            } else {
                instances = getInstanceByCurApp(serviceName);
            }
        } catch (OperationException ex) {
            LOGGER.severe(String.format(Locale.ENGLISH,
                    "Queried service [%s] instance list from service center failed, reason [%s]", serviceName,
                    ex.getMessage()));
        }
        if (instances == null) {
            return Collections.emptyList();
        }
        if (registerConfig.isEnableZoneAware()) {
            return zoneAwareFilter(instances);
        }
        return instances;
    }

    private List<MicroserviceInstance> queryAllAppInstances(String serviceName) {
        final MicroservicesResponse response = serviceCenterClient.getMicroserviceList();
        if (response == null || response.getServices() == null) {
            return Collections.emptyList();
        }
        final List<Microservice> allServices = response.getServices();
        final List<String> allServiceIds = allServices.stream()
                .filter(service -> StringUtils.equals(service.getServiceName(), getRealServiceName(true, serviceName)))
                .map(Microservice::getServiceId).distinct().collect(Collectors.toList());
        List<MicroserviceInstance> microserviceInstances = new ArrayList<>();
        allServiceIds.forEach(serviceId -> {
            final MicroserviceInstancesResponse instanceResponse = serviceCenterClient.getMicroserviceInstanceList(
                    serviceId);
            if (instanceResponse != null && instanceResponse.getInstances() != null) {
                microserviceInstances.addAll(instanceResponse.getInstances());
            }
        });
        return microserviceInstances;
    }

    private String getRealServiceName(boolean isAllowCrossApp, String serviceName) {
        if (!isAllowCrossApp) {
            return serviceName;
        }
        String curServiceName = serviceName;
        final int index = serviceName.indexOf(ConfigConstants.APP_SERVICE_SEPARATOR);
        if (index != -1) {
            curServiceName = serviceName.substring(index + 1);
        }
        return curServiceName;
    }

    private List<MicroserviceInstance> getInstanceByCurApp(String serviceName) {
        final SubscriptionKey subscriptionKey = buildSubscriptionKey(serviceName);
        serviceCenterDiscovery.registerIfNotPresent(subscriptionKey);
        return serviceCenterDiscovery.getInstanceCache(subscriptionKey);
    }

    private List<MicroserviceInstance> zoneAwareFilter(List<MicroserviceInstance> instances) {
        List<MicroserviceInstance> regionAndAzMatchList = new ArrayList<>();
        List<MicroserviceInstance> regionMatchList = new ArrayList<>();
        instances.forEach(instance -> {
            if (regionAndAzMatch(microserviceInstance, instance)) {
                // Matching region and zone
                regionAndAzMatchList.add(instance);
            } else if (regionMatch(microserviceInstance, instance)) {
                // Only the region is matched
                regionMatchList.add(instance);
            }
        });

        // Instances in matching regions are used first
        if (!regionAndAzMatchList.isEmpty()) {
            return regionAndAzMatchList;
        }
        if (!regionMatchList.isEmpty()) {
            return regionMatchList;
        }
        return instances;
    }

    private boolean regionAndAzMatch(MicroserviceInstance myself, MicroserviceInstance target) {
        if (myself.getDataCenterInfo() != null && target.getDataCenterInfo() != null) {
            return myself.getDataCenterInfo().getRegion().equals(target.getDataCenterInfo().getRegion())
                    && myself.getDataCenterInfo().getAvailableZone()
                    .equals(target.getDataCenterInfo().getAvailableZone());
        }
        return false;
    }

    private boolean regionMatch(MicroserviceInstance myself, MicroserviceInstance target) {
        if (target.getDataCenterInfo() != null) {
            return myself.getDataCenterInfo().getRegion().equals(target.getDataCenterInfo().getRegion());
        }
        return false;
    }

    private SubscriptionKey buildSubscriptionKey(String serviceId) {
        int index = serviceId.indexOf(ConfigConstants.APP_SERVICE_SEPARATOR);
        if (index == FLAG) {
            return new SubscriptionKey(microservice.getAppId(), serviceId);
        }
        return new SubscriptionKey(serviceId.substring(0, index), serviceId.substring(index + 1));
    }

    /**
     * Obtain the internal client
     *
     * @return ServiceCenterOperation
     */
    public ServiceCenterOperation getRawClient() {
        return serviceCenterClient;
    }

    /**
     * Heartbeat events
     *
     * @param event Heartbeat events
     */
    @Subscribe
    public void onHeartBeatEvent(HeartBeatEvent event) {
        if (event.isSuccess()) {
            RegisterContext.INSTANCE.getClientInfo().setStatus(Register.UP);
            LOGGER.fine("Service center post heartbeat success!");
        } else {
            RegisterContext.INSTANCE.getClientInfo().setStatus(Register.DOWN);
        }
    }

    /**
     * Register for the event
     *
     * @param event Register for the event
     */
    @Subscribe
    public void onMicroserviceRegistrationEvent(MicroserviceRegistrationEvent event) {
        if (event.isSuccess()) {
            initServiceCenterDiscovery();
            RegisterContext.INSTANCE.getClientInfo().setStatus(Register.UP);
        }
    }

    /**
     * Instance registration
     *
     * @param event event
     */
    @Subscribe
    public void onMicroserviceInstanceRegistrationEvent(MicroserviceInstanceRegistrationEvent event) {
        if (event.isSuccess()) {
            initServiceCenterDiscovery();
            GraceContext.INSTANCE.setSecondRegistryFinishTime(System.currentTimeMillis());
        }
    }

    private void initServiceCenterDiscovery() {
        if (serviceCenterDiscovery == null) {
            serviceCenterDiscovery = new ScDiscovery(serviceCenterClient, EVENT_BUS);
            serviceCenterDiscovery.updateMyselfServiceId(microservice.getServiceId());
            serviceCenterDiscovery.setPollInterval(registerConfig.getPullInterval());
            serviceCenterDiscovery.startDiscovery();
        } else {
            serviceCenterDiscovery.updateMyselfServiceId(microservice.getServiceId());
        }
    }

    private void startServiceCenterRegistration() {
        if (serviceCenterClient == null) {
            return;
        }
        serviceCenterRegistration = new ServiceCenterRegistration(serviceCenterClient, serviceCenterConfiguration,
                EVENT_BUS);
        EVENT_BUS.register(this);
        serviceCenterRegistration.setMicroservice(buildMicroService());
        buildMicroServiceInstance();
        serviceCenterRegistration.setHeartBeatInterval(microserviceInstance.getHealthCheck().getInterval());
        serviceCenterRegistration.setMicroserviceInstance(microserviceInstance);
        serviceCenterRegistration.startRegistration();
    }

    private void initServiceCenterConfiguration() {
        serviceCenterConfiguration = new ServiceCenterConfiguration();
        serviceCenterConfiguration.setIgnoreSwaggerDifferent(false);
    }

    private void initScClientAndWatch() {
        final AddressManager addressManager = createAddressManager(registerConfig.getProject(), getScUrls());
        final SSLProperties sslProperties = createSslProperties(registerConfig.isSslEnabled());
        serviceCenterClient = new ServiceCenterClient(addressManager, sslProperties,
                signRequest -> Collections.emptyMap(), "default", Collections.emptyMap());
    }

    private List<String> buildEndpoints() {
        return Collections.singletonList(String.format(Locale.ENGLISH, "rest://%s:%d", HostUtils.getMachineIp(),
                RegisterContext.INSTANCE.getClientInfo().getPort()));
    }

    private void buildMicroServiceInstance() {
        microserviceInstance = new MicroserviceInstance();
        microserviceInstance.setStatus(MicroserviceInstanceStatus.UP);
        String hostName = RegisterContext.INSTANCE.getClientInfo().getHost();
        microserviceInstance.setHostName(
                hostName != null && hostName.length() > MAX_HOST_NAME_LENGTH ? hostName.substring(0,
                        MAX_HOST_NAME_LENGTH) : hostName);
        microserviceInstance.setEndpoints(buildEndpoints());
        HealthCheck healthCheck = new HealthCheck();
        healthCheck.setMode(HealthCheckMode.push);
        healthCheck.setInterval(registerConfig.getHeartbeatInterval());
        healthCheck.setTimes(registerConfig.getHeartbeatRetryTimes());
        microserviceInstance.setHealthCheck(healthCheck);
        final String currentTimeMillis = String.valueOf(System.currentTimeMillis());
        microserviceInstance.setTimestamp(currentTimeMillis);
        microserviceInstance.setModTimestamp(currentTimeMillis);
        microserviceInstance.setProperties(RegisterContext.INSTANCE.getClientInfo().getMeta());
        if (registerConfig.isEnableZoneAware()) {
            // Set up data center information
            fillDataCenterInfo();
        }
    }

    private void fillDataCenterInfo() {
        final DataCenterInfo dataCenterInfo = new DataCenterInfo();
        dataCenterInfo.setName(registerConfig.getDataCenterName());
        dataCenterInfo.setAvailableZone(registerConfig.getDataCenterAvailableZone());
        dataCenterInfo.setRegion(registerConfig.getDataCenterRegion());
        microserviceInstance.setDataCenterInfo(dataCenterInfo);
    }

    private String getVersion() {
        try (JarFile jarFile = new JarFile(getClass().getProtectionDomain().getCodeSource().getLocation().getPath())) {
            final Object pluginName = JarFileUtils.getManifestAttr(jarFile, PluginConstant.PLUGIN_NAME_KEY);
            if (pluginName instanceof String) {
                return PluginSchemaValidator.getPluginVersionMap().get(pluginName);
            }
        } catch (IOException e) {
            LOGGER.warning("Cannot not get the version.");
        }
        return "";
    }

    private Microservice buildMicroService() {
        microservice = new Microservice();
        if (registerConfig.isAllowCrossApp()) {
            microservice.setAlias(registerConfig.getApplication() + ConfigConstants.APP_SERVICE_SEPARATOR
                    + RegisterContext.INSTANCE.getClientInfo().getServiceId());
        }
        microservice.setAppId(registerConfig.getApplication());
        microservice.setEnvironment(registerConfig.getEnvironment());

        // agent相关信息
        final Framework framework = new Framework();
        framework.setName(ConfigConstants.COMMON_FRAMEWORK);
        framework.setVersion(getVersion());
        microservice.setFramework(framework);
        microservice.setVersion(registerConfig.getVersion());
        microservice.setServiceName(RegisterContext.INSTANCE.getClientInfo().getServiceId());
        microservice.setStatus(MicroserviceStatus.UP);
        microservice.setProperties(registerConfig.getParametersMap());
        return microservice;
    }

    private List<String> getScUrls() {
        final List<String> urlList = commonConfig.getAddressList();
        if (urlList == null || urlList.isEmpty()) {
            throw new IllegalArgumentException("Kie url must not be empty!");
        }
        Iterator<String> it = urlList.iterator();
        while (it.hasNext()) {
            String url = it.next();
            if (!isUrlValid(url)) {
                LOGGER.warning(String.format(Locale.ENGLISH, "Invalid url : %s", url));
                it.remove();
            }
        }
        return urlList;
    }

    private boolean isUrlValid(String url) {
        if (url == null || url.length() == 0) {
            return false;
        }
        final String trimUrl = url.trim();
        return trimUrl.startsWith(HTTP_URL_PREFIX) || trimUrl.startsWith(HTTPS_URL_PREFIX);
    }

    private HttpConfiguration.SSLProperties createSslProperties(boolean isEnabled) {
        final HttpConfiguration.SSLProperties sslProperties = new HttpConfiguration.SSLProperties();
        sslProperties.setSslOption(SSLOption.DEFAULT_OPTION);
        sslProperties.setSslCustom(SSLCustom.defaultSSLCustom());
        sslProperties.setEnabled(isEnabled);
        return sslProperties;
    }

    private AddressManager createAddressManager(String project, List<String> scUrls) {
        return new AddressManager(project, scUrls, EVENT_BUS);
    }
}
