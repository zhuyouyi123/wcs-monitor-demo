package com.wcs.monitor.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wcs.monitor.entity.CommTestConfig;
import com.wcs.monitor.entity.DeviceCommBinding;

import java.util.List;

public interface DeviceCommBindingService extends IService<DeviceCommBinding> {

    List<CommTestConfig> listBoundConfigs(Long deviceId);

    void bindConfigs(Long deviceId, List<Long> configIds);

    void removeByDeviceId(Long deviceId);

    void removeByConfigId(Long configId);
}
