package com.wcs.monitor.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wcs.monitor.common.Result;
import com.wcs.monitor.entity.AlarmLog;
import com.wcs.monitor.service.AlarmLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alarms")
@RequiredArgsConstructor
public class AlarmLogController {

    private final AlarmLogService alarmLogService;

    @GetMapping
    public Result<List<AlarmLog>> list() {
        return Result.ok(alarmLogService.list());
    }

    @GetMapping("/page")
    public Result<IPage<AlarmLog>> page(@RequestParam(defaultValue = "1") long current,
                                        @RequestParam(defaultValue = "10") long size) {
        return Result.ok(alarmLogService.page(new Page<>(current, size)));
    }

    @GetMapping("/{id}")
    public Result<AlarmLog> getById(@PathVariable Long id) {
        return Result.ok(alarmLogService.getById(id));
    }

    @PostMapping
    public Result<Boolean> save(@RequestBody AlarmLog entity) {
        return Result.ok(alarmLogService.save(entity));
    }

    @PutMapping
    public Result<Boolean> update(@RequestBody AlarmLog entity) {
        return Result.ok(alarmLogService.updateById(entity));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(alarmLogService.removeById(id));
    }
}
