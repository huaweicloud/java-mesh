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

package io.sermant.demo.grayscale.rocketmq.integration.support;

import com.alibaba.fastjson.JSONObject;

import io.sermant.demo.grayscale.rocketmq.integration.support.entity.KieConfigEntity;
import io.sermant.demo.grayscale.rocketmq.integration.support.entity.KieResponse;
import io.sermant.demo.grayscale.rocketmq.integration.support.utils.LabelGroupUtils;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Kie response client
 *
 * @author chengyouling
 * @since 2024-10-30
 */
public class KieClient {
    private final RestTemplate restTemplate;

    private final String url;

    private Map<String, String> labels;

    /**
     * 构造函数
     *
     * @param restTemplate 请求器
     */
    public KieClient(RestTemplate restTemplate) {
        this(restTemplate, null);
    }

    /**
     * 构造函数
     *
     * @param restTemplate 请求器
     * @param url 地址
     */
    public KieClient(RestTemplate restTemplate, String url) {
        this.restTemplate = restTemplate;
        this.url = url == null ? "http://127.0.0.1:30110/v1/default/kie/kv" : url;
        this.labels = new HashMap<>();
        labels.put("app", "default");
        labels.put("environment", "development");
    }

    public void updateServiceNameLabels(String serviceName) {
        labels.put("service", serviceName);
    }

    /**
     * 发布配置
     *
     * @param key 键
     * @param value 值
     * @return 是否发布成功
     */
    public boolean publishConfig(String key, String value) {
        final KieConfigEntity configEntity = queryTargetKeyId(key);
        if (configEntity != null) {
            // 更新操作
            return updateKey(configEntity, value);
        } else {
            // 新增操作
            return addKey(key, value);
        }
    }

    /**
     * 更新配置
     *
     * @param configEntity 查询的config信息
     * @param value 新的配置内容
     * @return 是否更新成功
     */
    public boolean updateKey(KieConfigEntity configEntity, String value) {
        HttpHeaders headers = new HttpHeaders();
        MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
        headers.setContentType(type);
        headers.add("Accept", MediaType.APPLICATION_JSON.toString());

        HttpEntity<String> entity = new HttpEntity<>(JSONObject.toJSONString(new KieRequest(configEntity.getKey(),
                value, labels)),
                headers);
        String address = this.url + "/" + configEntity.getId();
        restTemplate.put(address, entity);
        return true;
    }

    /**
     * 根据key删除配置
     *
     * @param key 键
     * @return 是否删除成功
     */
    public boolean deleteKey(String key) {
        final KieConfigEntity configEntity = this.queryTargetKeyId(key);
        if (configEntity == null) {
            return false;
        }
        String address = this.url + "/" + configEntity.getId();
        restTemplate.delete(address);
        return true;
    }

    /**
     * 添加新配置
     *
     * @param key 键
     * @param value 值
     * @return 是否添加成功
     */
    public boolean addKey(String key, String value) {
        HttpHeaders headers = new HttpHeaders();
        MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
        headers.setContentType(type);
        headers.add("Accept", MediaType.APPLICATION_JSON.toString());

        HttpEntity<String> entity = new HttpEntity<>(JSONObject.toJSONString(new KieRequest(key, value, labels)), headers);

        try {
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, entity, String.class);
            return responseEntity.getStatusCode() == HttpStatus.OK;
        } catch (HttpClientErrorException ex) {
            // ignored
        }
        return false;
    }

    private KieConfigEntity queryTargetKeyId(String key) {
        final List<KieConfigEntity> query = query(null);
        return query.stream().filter(kieConfigEntity -> kieConfigEntity.getKey().equals(key))
                .findFirst().orElse(null);
    }

    /**
     * 根据标签查询kie配置
     *
     * @param labels 新增标签
     * @return kie响应
     */
    public List<KieConfigEntity> query(Map<String, String> labels) {
        Map<String, String> curLabels = labels;
        if (labels == null) {
            curLabels = this.labels;
        }
        if (curLabels == null) {
            curLabels = new HashMap<>();
            curLabels.put("app", "default");
            curLabels.put("environment", "development");
        }
        final String labelGroup = LabelGroupUtils.createLabelGroup(curLabels);
        final String labelCondition = LabelGroupUtils.getLabelCondition(labelGroup);
        String address = this.url + "?" + labelCondition + "&match=exact&revision=";
        final ResponseEntity<String> configs = restTemplate.getForEntity(address, String.class);
        JSONObject result = JSONObject.parseObject(configs.getBody());
        if (configs.getStatusCode().value() == 200) {
            final KieResponse kieResponse = result.toJavaObject(KieResponse.class);
            return kieResponse.getData();
        }
        return Collections.emptyList();
    }

    /**
     * kie请求
     *
     * @since 2022-07-12
     */
    static class KieRequest implements Serializable {
        private String key;
        private String value;
        private Map<String, String> labels;
        private String status = "enabled";

        public KieRequest(String key, String value, Map<String, String> labels) {
            this.key = key;
            this.value = value;
            this.labels = labels;
            if (this.labels == null) {
                this.labels = new HashMap<>();
                this.labels.put("app", "default");
                this.labels.put("environment", "development");
            }
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public Map<String, String> getLabels() {
            return labels;
        }

        public void setLabels(Map<String, String> labels) {
            this.labels = labels;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}
