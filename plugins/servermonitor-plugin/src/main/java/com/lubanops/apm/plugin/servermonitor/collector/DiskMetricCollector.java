/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2021-2021. All rights reserved.
 */

package com.lubanops.apm.plugin.servermonitor.collector;

import com.lubanops.apm.plugin.servermonitor.command.Command;
import com.lubanops.apm.plugin.servermonitor.command.CommandExecutor;
import com.lubanops.apm.plugin.servermonitor.command.DiskCommand;
import com.lubanops.apm.plugin.servermonitor.entity.DiskMetric;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.lubanops.apm.plugin.servermonitor.common.CalculateUtil.getPercentage;

/**
 * Linux disk指标{@link DiskMetric}采集器，通过执行两次{@link DiskCommand}命令
 * 获取两次{@link DiskCommand.DiskStats}结果，来计算每秒的磁盘读写字节数、和两次执
 * 行间隔时间段内IO时间所占的百分比。
 *
 * <p>每调用一次{@link #getDiskMetrics()}方法会触发一次{@link DiskCommand}命令
 * 的执行，然后将本次执行的{@link DiskCommand.DiskStats}结果与上次执行的结果进行计
 * 算得到{@link DiskMetric}，并缓存本次执行结果用于下次计算。</p>
 *
 * <p>重构泛PaaS：com.huawei.apm.plugin.collection.disk.ServerDiskProvider。
 * </p>
 */
public class DiskMetricCollector {

    /**
     * 扇区的字节大小
     */
    private static final int BYTES_PER_SECTOR = 512;

    private static final int IO_SPENT_SCALE = 2;

    /**
     * 采集周期毫秒，用于计算IO占比
     */
    private final long collectCycleMills;

    /**
     * 计算每秒读写字节数的因子
     */
    private final long multiPlyFactor;

    /**
     * key: diskName
     * value: DiskStats
     */
    private final Map<String, DiskCommand.DiskStats> lastDiskStats = new HashMap<String, DiskCommand.DiskStats>();

    /**
     * 指标零值缓存，新设备或存量设备第一次的指标使用零值
     * key: diskName
     * value: DiskMetric
     */
    private final Map<String, DiskMetric> emptyResults = new HashMap<String, DiskMetric>();

    public DiskMetricCollector(long collectCycle) {
        this.collectCycleMills = TimeUnit.MILLISECONDS.convert(collectCycle, TimeUnit.SECONDS);
        this.multiPlyFactor = BYTES_PER_SECTOR / collectCycle;
    }

    /**
     * 获取disk指标{@link DiskMetric}
     *
     * @return {@link DiskMetric}
     */
    public List<DiskMetric> getDiskMetrics() {
        final List<DiskCommand.DiskStats> currentDiskStats = CommandExecutor.execute(Command.DISK);
        if (currentDiskStats != null && !currentDiskStats.isEmpty()) {
            return buildResults(currentDiskStats);
        } else {
            return emptyResults();
        }
    }

    private List<DiskMetric> buildResults(List<DiskCommand.DiskStats> currentDiskStats) {
        final List<DiskMetric> diskMetrics = new LinkedList<DiskMetric>();
        for (final DiskCommand.DiskStats currentDiskStat : currentDiskStats) {
            final String deviceName = currentDiskStat.getDeviceName();
            // 根据设备名从last disk中检索并移除disk状态（判断是否存在）
            final DiskCommand.DiskStats lastDiskStat = lastDiskStats.remove(deviceName);
            DiskMetric diskMetric;
            if (lastDiskStat == null) {
                // 如果上次采集的数据中不包含当前disk，则其为新添加的disk，缓存新disk的“零值”
                diskMetric = new DiskMetric(deviceName);
                emptyResults.put(deviceName, diskMetric);
            } else {
                long readBytesPerSec = (currentDiskStat.getSectorsRead() - lastDiskStat.getSectorsRead()) * multiPlyFactor;
                long writeBytesPerSec = (currentDiskStat.getSectorsWritten() - lastDiskStat.getSectorsWritten()) * multiPlyFactor;
                double ioSpentPercentage = getPercentage(
                    currentDiskStat.getIoSpentMillis() - lastDiskStat.getIoSpentMillis(),
                    collectCycleMills, IO_SPENT_SCALE).doubleValue();
                diskMetric = new DiskMetric(deviceName, readBytesPerSec, writeBytesPerSec, ioSpentPercentage);
            }
            diskMetrics.add(diskMetric);
        }

        // last disk中剩下的disk表示，之前存在过，但在本次采集中已不存在的disk，即被移除的disk
        for (String deviceName : lastDiskStats.keySet()) {
            diskMetrics.add(emptyResults.get(deviceName));
        }

        // 更新last disk状态
        for (DiskCommand.DiskStats diskStat : currentDiskStats) {
            lastDiskStats.put(diskStat.getDeviceName(), diskStat);
        }
        return diskMetrics;
    }

    private List<DiskMetric> emptyResults() {
        return Collections.unmodifiableList(new ArrayList<DiskMetric>(emptyResults.values()));
    }
}
