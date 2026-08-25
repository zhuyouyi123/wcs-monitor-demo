package com.wcs.monitor.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wcs.monitor.entity.ConveyorNode;
import com.wcs.monitor.entity.DeviceInfo;
import com.wcs.monitor.enums.DeviceType;
import com.wcs.monitor.mapper.ConveyorNodeMapper;
import com.wcs.monitor.service.ConveyorNodeService;
import com.wcs.monitor.service.DeviceInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConveyorNodeServiceImpl extends ServiceImpl<ConveyorNodeMapper, ConveyorNode>
        implements ConveyorNodeService {

    private final DeviceInfoService deviceInfoService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveNode(ConveyorNode node) {
        validate(node);
        checkCodeDuplicate(node.getNodeCode(), null);
        node.setId(null);
        return save(node);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateNode(ConveyorNode node) {
        if (getById(node.getId()) == null) {
            throw new IllegalArgumentException("节点不存在: " + node.getId());
        }
        validate(node);
        checkCodeDuplicate(node.getNodeCode(), node.getId());
        return updateById(node);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeByDeviceId(Long deviceId) {
        remove(Wrappers.<ConveyorNode>lambdaQuery()
                .eq(ConveyorNode::getDeviceId, deviceId));
    }

    private void validate(ConveyorNode node) {
        if (node.getNodeCode() == null || node.getNodeCode().isBlank()) {
            throw new IllegalArgumentException("节点编码不能为空");
        }
        if (node.getNodeName() == null || node.getNodeName().isBlank()) {
            throw new IllegalArgumentException("节点名称不能为空");
        }
        if (node.getDeviceId() == null) {
            throw new IllegalArgumentException("请选择所属输送线");
        }
        DeviceInfo device = deviceInfoService.getById(node.getDeviceId());
        if (device == null) {
            throw new IllegalArgumentException("所选输送线不存在，请刷新后重试");
        }
        if (device.getDeviceType() != DeviceType.CONVEYOR) {
            throw new IllegalArgumentException("所选设备不是输送线，仅可关联输送线设备");
        }
    }

    private void checkCodeDuplicate(String code, Long excludeId) {
        boolean exists = lambdaQuery()
                .eq(ConveyorNode::getNodeCode, code)
                .ne(excludeId != null, ConveyorNode::getId, excludeId)
                .exists();
        if (exists) {
            throw new IllegalArgumentException("节点编码「" + code + "」已存在");
        }
    }
}
