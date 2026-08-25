package com.wcs.monitor.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wcs.monitor.entity.StorageLocation;
import com.wcs.monitor.mapper.StorageLocationMapper;
import com.wcs.monitor.service.StorageLocationService;
import org.springframework.stereotype.Service;

@Service
public class StorageLocationServiceImpl extends ServiceImpl<StorageLocationMapper, StorageLocation> implements StorageLocationService {
}
