package com.wcs.monitor.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wcs.monitor.entity.DeviceInfo;
import com.wcs.monitor.mapper.DeviceInfoMapper;
import com.wcs.monitor.service.DeviceInfoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeviceInfoServiceImpl extends ServiceImpl<DeviceInfoMapper, DeviceInfo> implements DeviceInfoService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveDevice(DeviceInfo device) {
        checkIpDuplicate(device, null);
        return save(device);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateDevice(DeviceInfo device) {
        DeviceInfo old = getById(device.getId());
        if (old == null) {
            throw new IllegalArgumentException("设备不存在: " + device.getId());
        }
        checkIpDuplicate(device, device.getId());
        return updateById(device);
    }

    private void checkIpDuplicate(DeviceInfo device, Long excludeId) {
        String ip = device.getIpAddress();
        if (ip == null || ip.isBlank()) {
            return;
        }
        boolean exists = lambdaQuery()
                .eq(DeviceInfo::getIpAddress, ip)
                .ne(excludeId != null, DeviceInfo::getId, excludeId)
                .exists();
        if (exists) {
            throw new IllegalArgumentException("IP 地址「" + ip + "」已被其他设备使用，堆垛机与输送线之间不可重复");
        }
    }
}
