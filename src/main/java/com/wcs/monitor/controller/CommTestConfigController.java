package com.wcs.monitor.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wcs.monitor.common.Result;
import com.wcs.monitor.entity.CommTestConfig;
import com.wcs.monitor.enums.DeviceType;
import com.wcs.monitor.service.CommTestConfigService;
import com.wcs.monitor.service.DeviceCommBindingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/test-configs")
@RequiredArgsConstructor
public class CommTestConfigController {

    private final CommTestConfigService commTestConfigService;
    private final DeviceCommBindingService deviceCommBindingService;

    @GetMapping
    public Result<List<CommTestConfig>> list(@RequestParam(required = false) DeviceType deviceType) {
        return Result.ok(commTestConfigService.list(Wrappers.<CommTestConfig>lambdaQuery()
                .eq(deviceType != null, CommTestConfig::getDeviceType, deviceType)
                .orderByAsc(CommTestConfig::getId)));
    }

    @GetMapping("/page")
    public Result<IPage<CommTestConfig>> page(@RequestParam(defaultValue = "1") long current,
                                              @RequestParam(defaultValue = "10") long size,
                                              @RequestParam(required = false) DeviceType deviceType) {
        return Result.ok(commTestConfigService.page(new Page<>(current, size), Wrappers.<CommTestConfig>lambdaQuery()
                .eq(deviceType != null, CommTestConfig::getDeviceType, deviceType)
                .orderByAsc(CommTestConfig::getId)));
    }

    @PostMapping
    public Result<Boolean> save(@RequestBody CommTestConfig config) {
        try {
            return Result.ok(commTestConfigService.saveConfig(config));
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PutMapping
    public Result<Boolean> update(@RequestBody CommTestConfig config) {
        try {
            return Result.ok(commTestConfigService.updateConfig(config));
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        deviceCommBindingService.removeByConfigId(id);
        return Result.ok(commTestConfigService.removeById(id));
    }
}
