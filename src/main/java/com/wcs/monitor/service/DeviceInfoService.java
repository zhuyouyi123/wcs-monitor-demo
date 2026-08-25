package com.wcs.monitor.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wcs.monitor.entity.DeviceInfo;

public interface DeviceInfoService extends IService<DeviceInfo> {

    boolean saveDevice(DeviceInfo device);

    boolean updateDevice(DeviceInfo device);
}
