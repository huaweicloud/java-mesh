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

package io.sermant.tag.transmission.rpc.interceptors;

import io.sermant.core.plugin.agent.entity.ExecuteContext;
import io.sermant.core.utils.tag.TrafficUtils;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ClientHttpRequestTagTransmissionInterceptor
 *
 * @author chengyouling
 * @since 2024-12-30
 */
public class ClientHttpRequestTagTransmissionInterceptorTest extends BaseInterceptorTest {
    private final HttpRequest request;

    public ClientHttpRequestTagTransmissionInterceptorTest() throws IOException {
        request = new HttpComponentsClientHttpRequestFactory().createRequest(URI.create("http://127.0.0.1:8001"),
                HttpMethod.GET);
    }

    @Test
    public void testClient() {
        ClientHttpRequestTagTransmissionInterceptor interceptor = new ClientHttpRequestTagTransmissionInterceptor();
        ExecuteContext context;
        ExecuteContext resContext;
        Map<String, List<String>> addHeaders = new HashMap<>();
        Map<String, List<String>> tags = new HashMap<>();
        TrafficUtils.removeTrafficTag();

        // no headers no tags
        context = buildContext(addHeaders);
        TrafficUtils.updateTrafficTag(tags);
        resContext = interceptor.before(context);
        Assert.assertEquals(0, ((HttpRequest) resContext.getObject()).getHeaders().size());
        TrafficUtils.removeTrafficTag();

        // there are headers but no tags
        addHeaders.put("defaultKey", Collections.singletonList("defaultValue"));
        context = buildContext(addHeaders);
        TrafficUtils.updateTrafficTag(tags);
        resContext = interceptor.before(context);
        Assert.assertEquals(1, ((HttpRequest) resContext.getObject()).getHeaders().size());
        Assert.assertEquals("defaultValue",
                ((HttpRequest) resContext.getObject()).getHeaders().get("defaultKey").get(0));
        TrafficUtils.removeTrafficTag();

        // there are headers and tags
        List<String> ids = new ArrayList<>();
        ids.add("testId001");
        ids.add("testId002");
        tags.put("x_lane_canary", ids);
        context = buildContext(addHeaders);
        TrafficUtils.updateTrafficTag(tags);
        resContext = interceptor.before(context);
        Assert.assertEquals(2, ((HttpRequest) resContext.getObject()).getHeaders().size());
        Assert.assertEquals("testId001",
                ((HttpRequest) resContext.getObject()).getHeaders().get("x_lane_canary").get(0));
        TrafficUtils.removeTrafficTag();
    }

    private ExecuteContext buildContext(Map<String, List<String>> addHeaders) {
        for (Map.Entry<String, List<String>> entry : addHeaders.entrySet()) {
            for (String val : entry.getValue()) {
                request.getHeaders().put(entry.getKey(), Collections.singletonList(val));
            }
        }
        return ExecuteContext.forMemberMethod(request, null, null, null, null);
    }
}
