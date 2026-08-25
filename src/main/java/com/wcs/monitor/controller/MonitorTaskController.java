package com.wcs.monitor.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wcs.monitor.common.Result;
import com.wcs.monitor.entity.MonitorTask;
import com.wcs.monitor.entity.MonitorTaskData;
import com.wcs.monitor.mapper.MonitorTaskDataMapper;
import com.wcs.monitor.service.MonitorTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/monitor-tasks")
@RequiredArgsConstructor
public class MonitorTaskController {

    private final MonitorTaskService monitorTaskService;
    private final MonitorTaskDataMapper monitorTaskDataMapper;

    @GetMapping
    public Result<List<MonitorTask>> list(@RequestParam(required = false) String status) {
        LambdaQueryWrapper<MonitorTask> wrapper = new LambdaQueryWrapper<MonitorTask>()
                .orderByDesc(MonitorTask::getCreateTime);
        if (status != null && !status.isBlank()) {
            wrapper.eq(MonitorTask::getStatus, status);
        }
        return Result.ok(monitorTaskService.list(wrapper));
    }

    @PostMapping
    public Result<Boolean> create(@RequestBody MonitorTask task) {
        try {
            monitorTaskService.createTask(task);
            return Result.ok(true);
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody MonitorTask task) {
        task.setId(id);
        try {
            monitorTaskService.updateTask(task);
            return Result.ok(true);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        try {
            monitorTaskService.deleteTask(id);
            return Result.ok(true);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/{id}/start")
    public Result<Boolean> start(@PathVariable Long id) {
        try {
            monitorTaskService.startTask(id);
            return Result.ok(true);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/{id}/stop")
    public Result<Boolean> stop(@PathVariable Long id) {
        try {
            monitorTaskService.stopTask(id);
            return Result.ok(true);
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    /** 任务采集数据分页，按采集时间倒序 */
    @GetMapping("/{id}/data")
    public Result<IPage<MonitorTaskData>> data(@PathVariable Long id,
                                               @RequestParam(defaultValue = "1") long current,
                                               @RequestParam(defaultValue = "20") long size) {
        Page<MonitorTaskData> page = new Page<>(current, size);
        return Result.ok(monitorTaskDataMapper.selectPage(page,
                new LambdaQueryWrapper<MonitorTaskData>()
                        .eq(MonitorTaskData::getTaskId, id)
                        .orderByDesc(MonitorTaskData::getCollectTime)
                        .orderByDesc(MonitorTaskData::getId)));
    }
}
