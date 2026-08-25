package com.wcs.monitor.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wcs.monitor.common.Result;
import com.wcs.monitor.dto.S7ReadResult;
import com.wcs.monitor.entity.CommTestConfig;
import com.wcs.monitor.entity.DeviceInfo;
import com.wcs.monitor.enums.DeviceType;
import com.wcs.monitor.enums.S7DataType;
import com.wcs.monitor.service.ConveyorNodeService;
import com.wcs.monitor.service.DeviceCommBindingService;
import com.wcs.monitor.service.DeviceConnectionManager;
import com.wcs.monitor.service.DeviceInfoService;
import com.wcs.monitor.util.S7DataUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceInfoController {

    private final DeviceInfoService deviceInfoService;
    private final DeviceConnectionManager deviceConnectionManager;
    private final DeviceCommBindingService deviceCommBindingService;
    private final ConveyorNodeService conveyorNodeService;
    private final com.wcs.monitor.service.MonitorTaskService monitorTaskService;

    @GetMapping
    public Result<List<DeviceInfo>> list(@RequestParam(required = false) DeviceType deviceType) {
        return Result.ok(deviceInfoService.list(Wrappers.<DeviceInfo>lambdaQuery()
                .eq(deviceType != null, DeviceInfo::getDeviceType, deviceType)));
    }

    @GetMapping("/page")
    public Result<IPage<DeviceInfo>> page(@RequestParam(defaultValue = "1") long current,
                                          @RequestParam(defaultValue = "10") long size,
                                          @RequestParam(required = false) DeviceType deviceType) {
        return Result.ok(deviceInfoService.page(new Page<>(current, size), Wrappers.<DeviceInfo>lambdaQuery()
                .eq(deviceType != null, DeviceInfo::getDeviceType, deviceType)
                .orderByAsc(DeviceInfo::getId)));
    }

    @GetMapping("/{id}")
    public Result<DeviceInfo> getById(@PathVariable Long id) {
        return Result.ok(deviceInfoService.getById(id));
    }

    @PostMapping
    public Result<Boolean> save(@RequestBody DeviceInfo entity) {
        try {
            return Result.ok(deviceInfoService.saveDevice(entity));
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PutMapping
    public Result<Boolean> update(@RequestBody DeviceInfo entity) {
        try {
            return Result.ok(deviceInfoService.updateDevice(entity));
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        // 有正在执行的监控任务时禁止删除
        if (monitorTaskService.hasRunningTask(id)) {
            return Result.fail("该设备存在正在执行的状态监控任务，请先停止对应任务后再删除");
        }
        deviceConnectionManager.disconnect(id, "设备删除，自动断开");
        deviceCommBindingService.removeByDeviceId(id);
        conveyorNodeService.removeByDeviceId(id);
        return Result.ok(deviceInfoService.removeById(id));
    }

    @GetMapping("/{id}/bindings")
    public Result<List<CommTestConfig>> getBindings(@PathVariable Long id) {
        return Result.ok(deviceCommBindingService.listBoundConfigs(id));
    }

    @PutMapping("/{id}/bindings")
    public Result<Boolean> updateBindings(@PathVariable Long id, @RequestBody List<Long> configIds) {
        try {
            deviceCommBindingService.bindConfigs(id, configIds);
            return Result.ok(true);
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/{id}/connect")
    public Result<Boolean> connect(@PathVariable Long id) {
        deviceConnectionManager.connect(id);
        return Result.ok(true);
    }

    @PostMapping("/{id}/disconnect")
    public Result<String> disconnect(@PathVariable Long id) {
        String reason = "用户手动断开";
        deviceConnectionManager.disconnect(id, reason);
        return Result.ok(reason);
    }

    @GetMapping("/{id}/s7/read")
    public Result<S7ReadResult> s7Read(@PathVariable Long id,
                                       @RequestParam(defaultValue = "1") int dbNumber,
                                       @RequestParam(defaultValue = "0") int start,
                                       @RequestParam(defaultValue = "4") int size,
                                       @RequestParam(defaultValue = "BYTE") S7DataType dataType) {
        if (dbNumber < 1 || dbNumber > 65535) {
            return Result.fail("DB 块编号必须在 1-65535 之间");
        }
        if (start < 0 || size < 1 || size > 512) {
            return Result.fail("读取长度必须在 1-512 字节之间");
        }
        try {
            byte[] data = deviceConnectionManager.readDB(id, dbNumber, start, size);
            S7ReadResult result = new S7ReadResult();
            result.setDbNumber(dbNumber);
            result.setStart(start);
            result.setSize(data.length);
            result.setDataType(dataType.getCode());
            result.setHex(S7DataUtil.toHex(data));
            result.setValues(S7DataUtil.decode(data, dataType));
            return Result.ok(result);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(e.getMessage());
        }
    }
}
