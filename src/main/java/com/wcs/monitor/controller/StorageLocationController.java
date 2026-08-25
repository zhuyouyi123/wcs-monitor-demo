package com.wcs.monitor.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wcs.monitor.common.Result;
import com.wcs.monitor.entity.StorageLocation;
import com.wcs.monitor.service.StorageLocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class StorageLocationController {

    private final StorageLocationService storageLocationService;

    @GetMapping
    public Result<List<StorageLocation>> list() {
        return Result.ok(storageLocationService.list());
    }

    @GetMapping("/page")
    public Result<IPage<StorageLocation>> page(@RequestParam(defaultValue = "1") long current,
                                               @RequestParam(defaultValue = "10") long size) {
        return Result.ok(storageLocationService.page(new Page<>(current, size)));
    }

    @GetMapping("/{id}")
    public Result<StorageLocation> getById(@PathVariable Long id) {
        return Result.ok(storageLocationService.getById(id));
    }

    @PostMapping
    public Result<Boolean> save(@RequestBody StorageLocation entity) {
        return Result.ok(storageLocationService.save(entity));
    }

    @PutMapping
    public Result<Boolean> update(@RequestBody StorageLocation entity) {
        return Result.ok(storageLocationService.updateById(entity));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(storageLocationService.removeById(id));
    }
}
