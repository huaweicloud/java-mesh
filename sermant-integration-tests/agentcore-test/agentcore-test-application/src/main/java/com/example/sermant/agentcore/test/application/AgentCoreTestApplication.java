/*
 * Copyright (C) 2023-2025 Huawei Technologies Co., Ltd. All rights reserved.
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

package com.example.sermant.agentcore.test.application;

import com.example.sermant.agentcore.test.application.router.ControllerHandler;
import com.example.sermant.agentcore.test.application.router.RouterPath;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;

/**
 * 测试应用启动类
 *
 * @author tangle
 * @since 2023-09-08
 */
public class AgentCoreTestApplication {
    private static final int SERVER_PORT = 8915;

    /**
     * 启动main方法
     *
     * @param args
     * @throws IOException
     * @throws IllegalAccessException
     */
    public static void main(String[] args) throws IOException, IllegalAccessException {
        int actualPort;
        String port = System.getProperty("server.port");
        if (port != null && !"".equals(port)) {
            actualPort = Integer.parseInt(port);
        } else {
            actualPort = SERVER_PORT;
        }
        HttpServer server = HttpServer.create(new InetSocketAddress(actualPort), 0);

        // 添加URL路由
        for (Field field : RouterPath.class.getDeclaredFields()) {
            server.createContext(String.valueOf(field.get(null)), new ControllerHandler());
        }

        // 启动服务器
        server.start();
    }
}
