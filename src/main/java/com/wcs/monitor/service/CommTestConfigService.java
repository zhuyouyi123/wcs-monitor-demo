package com.wcs.monitor.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wcs.monitor.entity.CommTestConfig;

public interface CommTestConfigService extends IService<CommTestConfig> {

    boolean saveConfig(CommTestConfig config);

    boolean updateConfig(CommTestConfig config);
}
