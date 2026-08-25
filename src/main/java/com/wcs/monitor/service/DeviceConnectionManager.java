package com.wcs.monitor.service;

import com.github.s7connector.api.DaveArea;
import com.github.s7connector.api.S7Connector;
import com.github.s7connector.api.factory.S7ConnectorFactory;
import com.wcs.monitor.entity.DeviceInfo;
import com.wcs.monitor.enums.ConnectStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 按需连接管理：
 * 1. 只有真正需要通信（如 S7 读取）时才建立连接；
 * 2. 连接成功后若连续一段时间没有通信，自动断开并落库；
 * 3. 所有断开均记录原因。
 */
@Slf4j
@Service
public class DeviceConnectionManager {

    private static final int CONNECT_TIMEOUT_MS = 3000;
    private static final long REAP_INTERVAL_MS = 5_000L;
    private static final long DEFAULT_IDLE_TIMEOUT_MS = 60_000L;
    private static final int RACK = 0;
    private static final int SLOT = 0;

    private final DeviceInfoService deviceInfoService;
    private final SysConfigService sysConfigService;

    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "device-connect");
        t.setDaemon(true);
        return t;
    });

    private final ScheduledExecutorService reaper = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "device-idle-reaper");
        t.setDaemon(true);
        return t;
    });

    private final Map<Long, ManagedConnection> connections = new ConcurrentHashMap<>();

    public DeviceConnectionManager(DeviceInfoService deviceInfoService, SysConfigService sysConfigService) {
        this.deviceInfoService = deviceInfoService;
        this.sysConfigService = sysConfigService;
    }

    @PostConstruct
    public void startReaper() {
        reaper.scheduleWithFixedDelay(this::reapIdleConnections,
                REAP_INTERVAL_MS, REAP_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * 手动预热连接：立即建立连接（单次尝试，不再无限重试），
     * 后续通信复用该连接；若失败状态置为连接失败。
     */
    public void connect(Long id) {
        DeviceInfo device = requireDevice(id);
        if (connections.containsKey(id)) {
            touch(id);
            log.info("设备[{}]已存在有效连接，忽略重复连接请求", device.getDeviceCode());
            return;
        }
        updateStatus(id, ConnectStatus.CONNECTING);
        executor.submit(() -> {
            try {
                openConnection(device);
                updateStatus(id, ConnectStatus.CONNECTED);
                log.info("设备[{}]连接成功", device.getDeviceCode());
            } catch (Exception e) {
                updateStatus(id, ConnectStatus.CONNECT_FAILED);
                log.warn("设备[{}]连接失败，原因：{}", device.getDeviceCode(), e.getMessage());
            }
        });
    }

    /** 手动断开 */
    public void disconnect(Long id, String reason) {
        String safeReason = reason == null || reason.isBlank() ? "未指定原因" : reason;
        ManagedConnection mc = connections.remove(id);
        if (mc != null) {
            closeQuietly(mc);
            log.info("设备[{}]断开连接，原因：{}", mc.deviceCode, safeReason);
        }
        if (!updateStatus(id, ConnectStatus.DISCONNECTED)) {
            throw new IllegalArgumentException("设备不存在: " + id);
        }
    }

    /**
     * 通信入口：确保连接可用（不可用则现场建立），并刷新最后使用时间。
     * 连接即在此刻按需创建，无需提前手动连接。
     */
    public byte[] readDB(Long deviceId, int dbNumber, int start, int size) {
        ManagedConnection mc = ensureConnected(deviceId);
        mc.lastUsedAt = System.currentTimeMillis();
        synchronized (mc.ioLock) {
            try {
                return mc.connector.read(DaveArea.DB, dbNumber, size, start);
            } catch (Exception e) {
                // 读取被线程中断（如监控任务停止）属正常停止路径：关闭连接但不标记设备故障
                if (isInterruption(e)) {
                    Thread.currentThread().interrupt();
                    String reason = "通信线程被中断（任务停止）";
                    log.info("设备[{}]断开连接，原因：{}", mc.deviceCode, reason);
                    closeAndRemove(deviceId, mc, reason);
                    updateStatus(deviceId, ConnectStatus.DISCONNECTED);
                    throw new IllegalStateException("S7 读取被中断");
                }
                String reason = "通信异常: " + e.getMessage();
                log.warn("设备[{}]断开连接，原因：{}（读取 DB{} 偏移{} 失败）",
                        mc.deviceCode, reason, dbNumber, start);
                closeAndRemove(deviceId, mc, reason);
                updateStatus(deviceId, ConnectStatus.CONNECT_FAILED);
                throw new IllegalStateException("S7 读取失败: " + e.getMessage());
            }
        }
    }

    /** 判断异常链中是否包含线程中断 */
    private static boolean isInterruption(Throwable e) {
        Throwable t = e;
        while (t != null) {
            if (t instanceof InterruptedException || t instanceof java.io.InterruptedIOException) {
                return true;
            }
            t = t.getCause() == t ? null : t.getCause();
        }
        return Thread.currentThread().isInterrupted();
    }

    private ManagedConnection ensureConnected(Long deviceId) {
        ManagedConnection mc = connections.get(deviceId);
        if (mc != null && isAlive(mc)) {
            return mc;
        }
        if (mc != null) {
            closeAndRemove(deviceId, mc, "连接已失效，重建前清理旧连接");
        }
        DeviceInfo device = requireDevice(deviceId);
        updateStatus(deviceId, ConnectStatus.CONNECTING);
        long begin = System.currentTimeMillis();
        try {
            mc = openConnection(device);
        } catch (Exception e) {
            updateStatus(deviceId, ConnectStatus.CONNECT_FAILED);
            throw new IllegalStateException(
                    "设备[" + device.getDeviceCode() + "]建立连接失败: " + e.getMessage());
        }
        updateStatus(deviceId, ConnectStatus.CONNECTED);
        log.info("设备[{}]按需建立连接成功，耗时 {}ms",
                device.getDeviceCode(), System.currentTimeMillis() - begin);
        return mc;
    }

    private ManagedConnection openConnection(DeviceInfo device) throws Exception {
        log.info("设备[{}]开始建立连接 {}:{}", device.getDeviceCode(), device.getIpAddress(), device.getPort());
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(device.getIpAddress(),
                device.getPort() == null ? 102 : device.getPort()), CONNECT_TIMEOUT_MS);
        try {
            S7Connector connector = S7ConnectorFactory.buildTCPConnector()
                    .withHost(device.getIpAddress())
                    .withPort(102)
                    .withRack(RACK)
                    .withSlot(SLOT)
                    .withTimeout(CONNECT_TIMEOUT_MS)
                    .build();
            ManagedConnection mc = new ManagedConnection(
                    device.getDeviceCode(), socket, connector, System.currentTimeMillis());
            ManagedConnection old = connections.put(device.getId(), mc);
            if (old != null) {
                closeQuietly(old);
            }
            return mc;
        } catch (Exception e) {
            closeQuietly(socket);
            throw e;
        }
    }

    /** 扫描空闲连接，超过配置时长未使用则自动断开 */
    private void reapIdleConnections() {
        long timeoutMs = idleTimeoutMs();
        long now = System.currentTimeMillis();
        connections.forEach((deviceId, mc) -> {
            long idleMs = now - mc.lastUsedAt;
            if (idleMs >= timeoutMs) {
                long idleSec = idleMs / 1000;
                closeAndRemove(deviceId, mc, "空闲超过 " + idleSec + " 秒，自动断开");
                updateStatus(deviceId, ConnectStatus.DISCONNECTED);
            }
        });
    }

    private long idleTimeoutMs() {
        try {
            String v = sysConfigService.getAll().get("connIdleTimeout");
            if (v != null && !v.isBlank()) {
                long sec = Long.parseLong(v.trim());
                if (sec >= 5) {
                    return sec * 1000L;
                }
            }
        } catch (Exception ignored) {
            // 配置缺失或非法时使用默认值
        }
        return DEFAULT_IDLE_TIMEOUT_MS;
    }

    private void touch(Long deviceId) {
        ManagedConnection mc = connections.get(deviceId);
        if (mc != null) {
            mc.lastUsedAt = System.currentTimeMillis();
        }
    }

    private void closeAndRemove(Long deviceId, ManagedConnection mc, String reason) {
        if (connections.remove(deviceId, mc)) {
            closeQuietly(mc);
            log.info("设备[{}]断开连接，原因：{}", mc.deviceCode, reason);
        }
    }

    private boolean isAlive(ManagedConnection mc) {
        return mc.connector != null
                && mc.socket != null
                && mc.socket.isConnected()
                && !mc.socket.isClosed();
    }

    private boolean updateStatus(Long id, ConnectStatus status) {
        DeviceInfo entity = new DeviceInfo();
        entity.setId(id);
        entity.setStatus(status);
        return deviceInfoService.updateById(entity);
    }

    private DeviceInfo requireDevice(Long id) {
        DeviceInfo device = deviceInfoService.getById(id);
        if (device == null) {
            throw new IllegalArgumentException("设备不存在: " + id);
        }
        return device;
    }

    private void closeQuietly(Socket socket) {
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                log.debug("关闭连接异常: {}", e.getMessage());
            }
        }
    }

    private void closeQuietly(ManagedConnection mc) {
        if (mc.connector != null) {
            try {
                mc.connector.close();
            } catch (Exception e) {
                log.debug("设备[{}]关闭 S7 连接异常: {}", mc.deviceCode, e.getMessage());
            }
        }
        if (mc.socket != null && !mc.socket.isClosed()) {
            try {
                mc.socket.close();
            } catch (IOException e) {
                log.debug("设备[{}]关闭连接异常: {}", mc.deviceCode, e.getMessage());
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        connections.forEach((id, mc) -> {
            closeAndRemove(id, mc, "服务关闭");
            updateStatus(id, ConnectStatus.DISCONNECTED);
        });
        executor.shutdownNow();
        reaper.shutdownNow();
    }

    private static class ManagedConnection {

        private final String deviceCode;
        private final Socket socket;
        private final S7Connector connector;
        private final Object ioLock = new Object();
        private volatile long lastUsedAt;

        ManagedConnection(String deviceCode, Socket socket, S7Connector connector, long lastUsedAt) {
            this.deviceCode = deviceCode;
            this.socket = socket;
            this.connector = connector;
            this.lastUsedAt = lastUsedAt;
        }
    }
}
