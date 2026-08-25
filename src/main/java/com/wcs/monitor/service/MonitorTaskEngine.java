package com.wcs.monitor.service;

import com.wcs.monitor.entity.CommTestConfig;
import com.wcs.monitor.entity.DeviceInfo;
import com.wcs.monitor.entity.MonitorTask;
import com.wcs.monitor.entity.MonitorTaskData;
import com.wcs.monitor.enums.MonitorTaskStatus;
import com.wcs.monitor.mapper.MonitorTaskDataMapper;
import com.wcs.monitor.mapper.MonitorTaskMapper;
import com.wcs.monitor.service.DeviceCommBindingService;
import com.wcs.monitor.service.DeviceConnectionManager;
import com.wcs.monitor.service.DeviceInfoService;
import com.wcs.monitor.service.SysDictItemService;
import com.wcs.monitor.util.S7DataUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 监控任务执行引擎：
 * 每个运行中的任务占用一个工作线程，按执行间隔周期性读取堆垛机绑定配置并落库；
 * 设备未连接时由 DeviceConnectionManager 按需自动建连。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MonitorTaskEngine {

    private final MonitorTaskMapper taskMapper;
    private final MonitorTaskDataMapper dataMapper;
    private final DeviceInfoService deviceInfoService;
    private final DeviceCommBindingService deviceCommBindingService;
    private final DeviceConnectionManager connectionManager;
    private final SysDictItemService sysDictItemService;

    private final Map<Long, Future<?>> futures = new ConcurrentHashMap<>();

    private final ExecutorService pool = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.setName("monitor-task-" + t.getId());
        return t;
    });

    /** 服务重启后把遗留的“运行中”任务复位为已停止 */
    @PostConstruct
    public void recoverOnStartup() {
        List<MonitorTask> leftovers = taskMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MonitorTask>()
                        .eq(MonitorTask::getStatus, MonitorTaskStatus.RUNNING));
        for (MonitorTask task : leftovers) {
            task.setStatus(MonitorTaskStatus.STOPPED);
            taskMapper.updateById(task);
            log.warn("监控任务[{}]因服务重启被复位为已停止", task.getTaskName());
        }
    }

    public boolean isRunning(Long taskId) {
        Future<?> future = futures.get(taskId);
        return future != null && !future.isDone();
    }

    /** 启动任务：状态置为运行中并清零已执行次数 */
    public synchronized void start(MonitorTask task) {
        Long taskId = task.getId();
        if (isRunning(taskId)) {
            throw new IllegalStateException("任务已在运行中");
        }
        task.setStatus(MonitorTaskStatus.RUNNING);
        task.setExecutedCount(0);
        taskMapper.updateById(task);
        futures.put(taskId, pool.submit(() -> runTask(taskId)));
        log.info("监控任务[{}]已启动，间隔 {} 秒，{}",
                task.getTaskName(), task.getIntervalSeconds(),
                task.getExecCount() == null || task.getExecCount() <= 0 ? "持续执行" : "共执行 " + task.getExecCount() + " 次");
    }

    /** 停止任务：先落库再取消线程 */
    public synchronized void stop(Long taskId) {
        MonitorTask task = taskMapper.selectById(taskId);
        Future<?> future = futures.remove(taskId);
        if (future != null) {
            future.cancel(true);
        }
        if (task != null && task.getStatus() == MonitorTaskStatus.RUNNING) {
            task.setStatus(MonitorTaskStatus.STOPPED);
            taskMapper.updateById(task);
            log.info("监控任务[{}]已手动停止", task.getTaskName());
        }
    }

    private void runTask(Long taskId) {
        boolean cancelled = false;
        try {
            while (!Thread.currentThread().isInterrupted()) {
                MonitorTask current = taskMapper.selectById(taskId);
                if (current == null || current.getStatus() != MonitorTaskStatus.RUNNING) {
                    break;
                }
                Integer execCount = current.getExecCount();
                if (execCount != null && execCount > 0
                        && current.getExecutedCount() != null && current.getExecutedCount() >= execCount) {
                    break;
                }
                try {
                    executeOnce(current);
                } catch (TaskFatalException e) {
                    log.warn("监控任务[{}]终止，原因：{}", current.getTaskName(), e.getMessage());
                    markStopped(taskId);
                    return;
                } catch (Exception e) {
                    // 停止任务会打断进行中的读取，属正常停止路径，不按失败记录
                    if (Thread.currentThread().isInterrupted() || hasCause(e, InterruptedException.class)) {
                        Thread.currentThread().interrupt();
                        cancelled = true;
                        break;
                    }
                    log.warn("监控任务[{}]本轮执行失败，原因：{}", current.getTaskName(), e.getMessage());
                }
                long intervalMs = Math.max(1, current.getIntervalSeconds() == null ? 5 : current.getIntervalSeconds()) * 1000L;
                Thread.sleep(intervalMs);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            cancelled = true;
        } finally {
            finishTask(taskId, cancelled);
        }
    }

    /** 单次采集：读取该堆垛机的全部绑定配置并逐条落库 */
    private void executeOnce(MonitorTask task) {
        DeviceInfo device = deviceInfoService.getById(task.getDeviceId());
        if (device == null) {
            throw new TaskFatalException("绑定的堆垛机已被删除");
        }
        List<CommTestConfig> configs = deviceCommBindingService.listBoundConfigs(task.getDeviceId());
        if (configs.isEmpty()) {
            log.warn("设备[{}]未绑定通信测试配置，跳过本次采集", device.getDeviceCode());
            return;
        }
        // readDB 内部会在未连接时按需建立连接；连接失败会抛出异常并在外层记录原因
        // 每轮采集前先删除该设备已有的采集数据，表中始终只保留最新一轮结果
        dataMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MonitorTaskData>()
                .eq(MonitorTaskData::getDeviceId, task.getDeviceId()));
        LocalDateTime now = LocalDateTime.now();
        for (CommTestConfig config : configs) {
            byte[] raw = connectionManager.readDB(task.getDeviceId(),
                    config.getDbNumber(), config.getStartOffset(), config.getReadLength());
            List<String> decoded = S7DataUtil.decode(raw, config.getDataType());
            String value = String.join(",", decoded);
            Map<String, SysDictItemService.DictValueInfo> dict = sysDictItemService.valueInfoMap(config.getDictKey());
            MonitorTaskData record = new MonitorTaskData();
            record.setTaskId(task.getId());
            record.setDeviceId(task.getDeviceId());
            record.setConfigId(config.getId());
            record.setConfigName(config.getConfigName());
            record.setDbNumber(config.getDbNumber());
            record.setStartOffset(config.getStartOffset());
            record.setDataType(config.getDataType().getCode());
            record.setRawValue(value);
            // 配置关联了字典时，把采集值转换为对应含义并带上颜色；无字典则不转换
            if (!dict.isEmpty()) {
                java.util.StringJoiner converted = new java.util.StringJoiner(",");
                String color = null;
                for (String v : decoded) {
                    SysDictItemService.DictValueInfo info = dict.get(v);
                    converted.add(info != null ? info.getLabel() : v);
                    if (color == null && info != null && info.getColor() != null) {
                        color = info.getColor();
                    }
                }
                record.setDictLabel(converted.toString());
                record.setDictColor(color);
            }
            record.setCollectTime(now);
            dataMapper.insert(record);
        }
        MonitorTask update = new MonitorTask();
        update.setId(task.getId());
        update.setExecutedCount((task.getExecutedCount() == null ? 0 : task.getExecutedCount()) + 1);
        update.setLastRunTime(now);
        taskMapper.updateById(update);
        log.info("监控任务[{}]第 {} 次采集完成，写入 {} 条数据", task.getTaskName(), update.getExecutedCount(), configs.size());
    }

    private void finishTask(Long taskId, boolean cancelled) {
        futures.remove(taskId);
        MonitorTask current = taskMapper.selectById(taskId);
        if (current == null || current.getStatus() != MonitorTaskStatus.RUNNING) {
            return;
        }
        current.setStatus(cancelled ? MonitorTaskStatus.STOPPED : MonitorTaskStatus.FINISHED);
        taskMapper.updateById(current);
        log.info("监控任务[{}]执行结束，状态：{}", current.getTaskName(), current.getStatus().getLabel());
    }

    private void markStopped(Long taskId) {
        MonitorTask current = taskMapper.selectById(taskId);
        if (current != null && current.getStatus() == MonitorTaskStatus.RUNNING) {
            current.setStatus(MonitorTaskStatus.STOPPED);
            taskMapper.updateById(current);
        }
        futures.remove(taskId);
    }

    @PreDestroy
    public void shutdown() {
        futures.keySet().forEach(this::stop);
        pool.shutdownNow();
    }

    /** 判断异常链中是否包含指定类型的异常 */
    private static boolean hasCause(Throwable e, Class<? extends Throwable> type) {
        Throwable t = e;
        while (t != null) {
            if (type.isInstance(t)) {
                return true;
            }
            t = t.getCause() == t ? null : t.getCause();
        }
        return false;
    }

    /** 导致任务无法继续的致命错误（如设备被删除） */
    private static class TaskFatalException extends RuntimeException {

        TaskFatalException(String message) {
            super(message);
        }
    }
}
