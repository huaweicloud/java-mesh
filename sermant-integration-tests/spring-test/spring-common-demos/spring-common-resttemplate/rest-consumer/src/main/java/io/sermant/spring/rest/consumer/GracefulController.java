/*
 * Copyright (C) 2022-2022 Huawei Technologies Co., Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sermant.spring.rest.consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

/**
 * 优雅上下线测试
 *
 * @author zhouss
 * @since 2022-11-14
 */
@Controller
@ResponseBody
@RequestMapping("graceful")
public class GracefulController {
    @Autowired
    @Qualifier("gracefulRestTemplate")
    private RestTemplate restTemplate;

    @Value("${down.serviceName}")
    private String downServiceName;

    /**
     * 测试优雅上下线
     *
     * @return port
     */
    @RequestMapping("/testGraceful")
    public String testGraceful() {
        return restTemplate.getForObject("http://" + downServiceName + "/testGraceful", String.class);
    }
}
