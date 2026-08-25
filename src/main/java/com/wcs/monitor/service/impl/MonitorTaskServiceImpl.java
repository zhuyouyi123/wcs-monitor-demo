package com.wcs.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wcs.monitor.entity.CommTestConfig;
import com.wcs.monitor.entity.DeviceInfo;
import com.wcs.monitor.entity.MonitorTask;
import com.wcs.monitor.entity.MonitorTaskData;
import com.wcs.monitor.enums.DeviceType;
import com.wcs.monitor.enums.MonitorTaskStatus;
import com.wcs.monitor.mapper.MonitorTaskDataMapper;
import com.wcs.monitor.mapper.MonitorTaskMapper;
import com.wcs.monitor.service.DeviceCommBindingService;
import com.wcs.monitor.service.DeviceInfoService;
import com.wcs.monitor.service.MonitorTaskEngine;
import com.wcs.monitor.service.MonitorTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MonitorTaskServiceImpl extends ServiceImpl<MonitorTaskMapper, MonitorTask> implements MonitorTaskService {

    private final DeviceInfoService deviceInfoService;
    private final DeviceCommBindingService deviceCommBindingService;
    private final MonitorTaskDataMapper monitorTaskDataMapper;
    private final MonitorTaskEngine monitorTaskEngine;

    @Override
    public void createTask(MonitorTask task) {
        validate(task);
        task.setId(null);
        task.setTaskNo("MT" + System.currentTimeMillis());
        task.setTaskType(MonitorTask.TYPE_STACKER_MONITOR);
        task.setStatus(MonitorTaskStatus.STOPPED);
        task.setExecutedCount(0);
        save(task);
    }

    @Override
    public void updateTask(MonitorTask task) {
        MonitorTask db = getById(task.getId());
        if (db == null) {
            throw new IllegalArgumentException("任务不存在");
        }
        if (db.getStatus() == MonitorTaskStatus.RUNNING) {
            boolean execChanged = notEquals(task.getDeviceId(), db.getDeviceId())
                    || notEquals(task.getExecCount(), db.getExecCount())
                    || notEquals(task.getIntervalSeconds(), db.getIntervalSeconds());
            if (execChanged) {
                throw new IllegalStateException("运行中的任务不允许修改执行配置，请先停止任务");
            }
            // 运行中仅允许改名称与备注
            MonitorTask update = new MonitorTask();
            update.setId(db.getId());
            update.setTaskName(task.getTaskName() == null ? db.getTaskName() : task.getTaskName().trim());
            update.setRemark(task.getRemark());
            updateById(update);
            return;
        }
        validate(task);
        task.setStatus(db.getStatus());
        task.setExecutedCount(db.getExecutedCount());
        updateById(task);
    }

    @Override
    public void deleteTask(Long id) {
        MonitorTask db = getById(id);
        if (db == null) {
            throw new IllegalArgumentException("任务不存在");
        }
        if (db.getStatus() == MonitorTaskStatus.RUNNING) {
            throw new IllegalStateException("任务正在运行，请先停止后再删除");
        }
        removeById(id);
        monitorTaskDataMapper.delete(new LambdaQueryWrapper<MonitorTaskData>()
                .eq(MonitorTaskData::getTaskId, id));
    }

    @Override
    public void startTask(Long id) {
        MonitorTask db = getById(id);
        if (db == null) {
            throw new IllegalArgumentException("任务不存在");
        }
        if (db.getStatus() == MonitorTaskStatus.RUNNING) {
            throw new IllegalStateException("任务已在运行中");
        }
        List<CommTestConfig> configs = deviceCommBindingService.listBoundConfigs(db.getDeviceId());
        if (configs.isEmpty()) {
            throw new IllegalStateException("该堆垛机未绑定通信测试配置，请先在设备管理中完成绑定配置");
        }
        monitorTaskEngine.start(db);
    }

    @Override
    public void stopTask(Long id) {
        if (getById(id) == null) {
            throw new IllegalArgumentException("任务不存在");
        }
        monitorTaskEngine.stop(id);
    }

    @Override
    public boolean hasRunningTask(Long deviceId) {
        return lambdaQuery()
                .eq(MonitorTask::getDeviceId, deviceId)
                .eq(MonitorTask::getStatus, MonitorTaskStatus.RUNNING)
                .exists();
    }

    private void validate(MonitorTask task) {
        if (task.getTaskName() == null || task.getTaskName().isBlank()) {
            throw new IllegalArgumentException("任务名称不能为空");
        }
        if (task.getDeviceId() == null) {
            throw new IllegalArgumentException("请选择要监控的堆垛机");
        }
        DeviceInfo device = deviceInfoService.getById(task.getDeviceId());
        if (device == null) {
            throw new IllegalArgumentException("绑定的堆垛机不存在");
        }
        if (device.getDeviceType() != DeviceType.STACKER) {
            throw new IllegalArgumentException("状态监控任务只能绑定堆垛机类型的设备");
        }
        if (task.getIntervalSeconds() == null) {
            task.setIntervalSeconds(5);
        } else if (task.getIntervalSeconds() < 1) {
            throw new IllegalArgumentException("执行间隔不能小于 1 秒");
        }
        if (task.getExecCount() != null && task.getExecCount() < 0) {
            throw new IllegalArgumentException("执行次数不能为负数（0 表示持续执行）");
        }
        task.setTaskName(task.getTaskName().trim());
    }

    private boolean notEquals(Object a, Object b) {
        return a == null ? b != null : !a.equals(b);
    }
}
