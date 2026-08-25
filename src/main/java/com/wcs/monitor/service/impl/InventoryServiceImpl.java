package com.wcs.monitor.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wcs.monitor.entity.Inventory;
import com.wcs.monitor.mapper.InventoryMapper;
import com.wcs.monitor.service.InventoryService;
import org.springframework.stereotype.Service;

@Service
public class InventoryServiceImpl extends ServiceImpl<InventoryMapper, Inventory> implements InventoryService {
}
