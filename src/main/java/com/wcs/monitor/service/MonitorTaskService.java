package com.wcs.monitor.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wcs.monitor.entity.MonitorTask;

public interface MonitorTaskService extends IService<MonitorTask> {

    void createTask(MonitorTask task);

    void updateTask(MonitorTask task);

    void deleteTask(Long id);

    void startTask(Long id);

    void stopTask(Long id);

    boolean hasRunningTask(Long deviceId);
}
