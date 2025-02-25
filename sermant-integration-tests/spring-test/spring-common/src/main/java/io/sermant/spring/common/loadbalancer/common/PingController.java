/*
 * Copyright (C) 2022-2022 Huawei Technologies Co., Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.sermant.spring.common.loadbalancer.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

/**
 * ping, 测试服务联通性
 *
 * @author zhouss
 * @since 2022-08-16
 */
@Controller
@ResponseBody
@RequestMapping("/lb")
public class PingController {
    @Autowired(required = false)
    private RestTemplate restTemplate;

    /**
     * ping
     *
     * @return ok
     * @throws IllegalStateException 注入失败抛出
     */
    @RequestMapping("/ping")
    public String ping() {
        if (restTemplate == null) {
            throw new IllegalStateException("wrong");
        }
        return restTemplate.getForObject("http://rest-provider/lb/ping", String.class);
    }
}
