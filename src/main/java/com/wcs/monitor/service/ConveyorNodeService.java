package com.wcs.monitor.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wcs.monitor.entity.ConveyorNode;

public interface ConveyorNodeService extends IService<ConveyorNode> {

    boolean saveNode(ConveyorNode node);

    boolean updateNode(ConveyorNode node);

    void removeByDeviceId(Long deviceId);
}
