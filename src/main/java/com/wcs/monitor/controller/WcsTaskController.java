package com.wcs.monitor.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wcs.monitor.common.Result;
import com.wcs.monitor.entity.WcsTask;
import com.wcs.monitor.service.WcsTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class WcsTaskController {

    private final WcsTaskService wcsTaskService;

    @GetMapping
    public Result<List<WcsTask>> list() {
        return Result.ok(wcsTaskService.list());
    }

    @GetMapping("/page")
    public Result<IPage<WcsTask>> page(@RequestParam(defaultValue = "1") long current,
                                       @RequestParam(defaultValue = "10") long size) {
        return Result.ok(wcsTaskService.page(new Page<>(current, size)));
    }

    @GetMapping("/{id}")
    public Result<WcsTask> getById(@PathVariable Long id) {
        return Result.ok(wcsTaskService.getById(id));
    }

    @PostMapping
    public Result<Boolean> save(@RequestBody WcsTask entity) {
        return Result.ok(wcsTaskService.save(entity));
    }

    @PutMapping
    public Result<Boolean> update(@RequestBody WcsTask entity) {
        return Result.ok(wcsTaskService.updateById(entity));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(wcsTaskService.removeById(id));
    }
}
