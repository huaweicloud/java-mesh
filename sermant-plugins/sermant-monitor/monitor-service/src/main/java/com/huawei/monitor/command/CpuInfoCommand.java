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

package com.huawei.monitor.command;

import com.huaweicloud.sermant.core.utils.StringUtils;

import java.io.InputStream;
import java.util.List;

/**
 * CPU信息命令
 *
 * @author zhp
 * @version 1.0.0
 * @since 2022-08-02
 */
public class CpuInfoCommand extends CommonMonitorCommand<CpuInfoCommand.CpuInfoStat> {

    private static final String COMMAND = "lscpu";

    private static final String CPU_NUM_PRE = "Socket(s)";

    private static final String CPU_CORE_PRE = "Core(s) per socket";

    private static final String SEPARATOR = ":";

    private static final String MATCHES = "^[0-9]*$";

    private static final int LIMIT_SIZE = 2;

    @Override
    public String getCommand() {
        return COMMAND;
    }

    /**
     * 重构泛PaaS类：com.huawei.sermant.plugin.collection.util.CpuParser parse方法
     *
     * @param inputStream 外部进程输出流
     * @return 解析后的结果
     */
    @Override
    public CpuInfoStat parseResult(InputStream inputStream) {
        final List<String> lines = readLines(inputStream);
        int core;
        int cpuNum = 0;
        int cpuCores = 0;
        for (String line : lines) {
            if (StringUtils.isBlank(line)) {
                continue;
            }
            String[] cpuInfo = line.split(SEPARATOR);
            if (cpuInfo.length < LIMIT_SIZE || StringUtils.isBlank(cpuInfo[0]) || StringUtils.isBlank(cpuInfo[1])) {
                continue;
            }
            cpuInfo[1] = cpuInfo[1].trim();
            if (cpuInfo[0].startsWith(CPU_NUM_PRE) && cpuInfo[1].matches(MATCHES)) {
                cpuNum = Integer.parseInt(cpuInfo[1]);
            } else if (cpuInfo[0].startsWith(CPU_CORE_PRE) && cpuInfo[1].matches(MATCHES)) {
                cpuCores = Integer.parseInt(cpuInfo[1]);
            }
        }
        core = cpuNum * cpuCores;
        return new CpuInfoStat(core);
    }

    /**
     * CPU信息
     *
     * @since 2022-08-02
     */
    public static class CpuInfoStat {

        /**
         * 核心数
         */
        private final int totalCores;

        /**
         * 构造方法
         *
         * @param totalCores 核心数
         */
        public CpuInfoStat(int totalCores) {
            this.totalCores = totalCores;
        }

        public int getTotalCores() {
            return totalCores;
        }
    }
}
