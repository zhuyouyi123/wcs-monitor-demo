package com.wcs.monitor.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wcs.monitor.entity.CommTestConfig;
import com.wcs.monitor.entity.DeviceCommBinding;
import com.wcs.monitor.entity.DeviceInfo;
import com.wcs.monitor.mapper.DeviceCommBindingMapper;
import com.wcs.monitor.service.CommTestConfigService;
import com.wcs.monitor.service.DeviceCommBindingService;
import com.wcs.monitor.service.DeviceInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DeviceCommBindingServiceImpl extends ServiceImpl<DeviceCommBindingMapper, DeviceCommBinding>
        implements DeviceCommBindingService {

    private final DeviceInfoService deviceInfoService;
    private final CommTestConfigService commTestConfigService;

    @Override
    public List<CommTestConfig> listBoundConfigs(Long deviceId) {
        List<Long> configIds = lambdaQuery()
                .eq(DeviceCommBinding::getDeviceId, deviceId)
                .orderByAsc(DeviceCommBinding::getId)
                .list()
                .stream()
                .map(DeviceCommBinding::getConfigId)
                .toList();
        if (configIds.isEmpty()) {
            return List.of();
        }
        return commTestConfigService.listByIds(configIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindConfigs(Long deviceId, List<Long> configIds) {
        DeviceInfo device = deviceInfoService.getById(deviceId);
        if (device == null) {
            throw new IllegalArgumentException("设备不存在: " + deviceId);
        }
        List<Long> ids = configIds == null ? List.of() : configIds.stream().distinct().toList();
        if (!ids.isEmpty()) {
            List<CommTestConfig> configs = commTestConfigService.listByIds(ids);
            if (configs.size() != ids.size()) {
                throw new IllegalArgumentException("存在无效的通信配置，请刷新后重试");
            }
            for (CommTestConfig config : configs) {
                if (config.getDeviceType() != device.getDeviceType()) {
                    throw new IllegalArgumentException(
                            "配置「" + config.getConfigName() + "」设备类型不匹配，仅可绑定同类型配置");
                }
            }
        }
        removeByDeviceId(deviceId);
        if (!ids.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            List<DeviceCommBinding> bindings = ids.stream().map(configId -> {
                DeviceCommBinding binding = new DeviceCommBinding();
                binding.setDeviceId(deviceId);
                binding.setConfigId(configId);
                binding.setCreateTime(now);
                return binding;
            }).toList();
            baseMapper.insertBatch(bindings);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeByDeviceId(Long deviceId) {
        remove(Wrappers.<DeviceCommBinding>lambdaQuery()
                .eq(DeviceCommBinding::getDeviceId, deviceId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeByConfigId(Long configId) {
        remove(Wrappers.<DeviceCommBinding>lambdaQuery()
                .eq(DeviceCommBinding::getConfigId, configId));
    }
}
