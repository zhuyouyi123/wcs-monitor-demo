package com.wcs.monitor.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wcs.monitor.common.Result;
import com.wcs.monitor.entity.OperationLog;
import com.wcs.monitor.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class OperationLogController {

    private final OperationLogService operationLogService;

    @GetMapping
    public Result<List<OperationLog>> list() {
        return Result.ok(operationLogService.list());
    }

    @GetMapping("/page")
    public Result<IPage<OperationLog>> page(@RequestParam(defaultValue = "1") long current,
                                            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(operationLogService.page(new Page<>(current, size)));
    }

    @GetMapping("/{id}")
    public Result<OperationLog> getById(@PathVariable Long id) {
        return Result.ok(operationLogService.getById(id));
    }

    @PostMapping
    public Result<Boolean> save(@RequestBody OperationLog entity) {
        return Result.ok(operationLogService.save(entity));
    }

    @PutMapping
    public Result<Boolean> update(@RequestBody OperationLog entity) {
        return Result.ok(operationLogService.updateById(entity));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(operationLogService.removeById(id));
    }
}
