package com.wcs.monitor.service;

import com.github.s7connector.api.DaveArea;
import com.github.s7connector.api.S7Connector;
import com.github.s7connector.api.factory.S7ConnectorFactory;
import com.wcs.monitor.entity.DeviceInfo;
import com.wcs.monitor.enums.ConnectStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Slf4j
@Service
public class DeviceConnectionManager {

    private static final int CONNECT_TIMEOUT_MS = 3000;
    private static final long RETRY_INTERVAL_MS = 10_000L;
    private static final long MONITOR_INTERVAL_MS = 5_000L;
    private static final int S7_PORT = 102;
    private static final int RACK = 0;
    private static final int SLOT = 0;

    private final DeviceInfoService deviceInfoService;
    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "device-connection");
        t.setDaemon(true);
        return t;
    });
    private final Map<Long, ConnectionHandle> connections = new ConcurrentHashMap<>();

    public DeviceConnectionManager(DeviceInfoService deviceInfoService) {
        this.deviceInfoService = deviceInfoService;
    }

    public void connect(Long id) {
        DeviceInfo device = requireDevice(id);
        if (connections.containsKey(id)) {
            log.info("设备[{}]连接任务执行中，忽略重复连接请求", device.getDeviceCode());
            return;
        }
        updateStatus(id, ConnectStatus.CONNECTING);
        ConnectionWorker worker = new ConnectionWorker(id, device.getDeviceCode(), device.getIpAddress(), device.getPort());
        Future<?> future = executor.submit(worker);
        connections.put(id, new ConnectionHandle(worker, future));
        log.info("设备[{}]发起连接 {}:{}", device.getDeviceCode(), device.getIpAddress(), device.getPort());
    }

    public void disconnect(Long id) {
        ConnectionHandle handle = connections.remove(id);
        if (handle != null) {
            stopHandle(handle);
            log.info("设备[{}]已断开连接", handle.worker.deviceCode);
        } else if (!updateStatus(id, ConnectStatus.DISCONNECTED)) {
            throw new IllegalArgumentException("设备不存在: " + id);
        }
    }

    public byte[] readDB(Long deviceId, int dbNumber, int start, int size) {
        ConnectionHandle handle = connections.get(deviceId);
        if (handle == null || !handle.worker.isAlive()) {
            throw new IllegalStateException("堆垛机未连接，请先连接设备");
        }
        return handle.worker.readDB(dbNumber, start, size);
    }

    private DeviceInfo requireDevice(Long id) {
        DeviceInfo device = deviceInfoService.getById(id);
        if (device == null) {
            throw new IllegalArgumentException("设备不存在: " + id);
        }
        return device;
    }

    private boolean updateStatus(Long id, ConnectStatus status) {
        DeviceInfo entity = new DeviceInfo();
        entity.setId(id);
        entity.setStatus(status);
        return deviceInfoService.updateById(entity);
    }

    private void stopHandle(ConnectionHandle handle) {
        handle.worker.stop();
        handle.future.cancel(true);
    }

    @PreDestroy
    public void shutdown() {
        connections.values().forEach(this::stopHandle);
        connections.clear();
        executor.shutdownNow();
    }

    private static class ConnectionHandle {

        private final ConnectionWorker worker;
        private final Future<?> future;

        ConnectionHandle(ConnectionWorker worker, Future<?> future) {
            this.worker = worker;
            this.future = future;
        }
    }

    private class ConnectionWorker implements Runnable {

        private final Long id;
        private final String deviceCode;
        private final String ip;
        private final Integer port;

        private volatile boolean running = true;
        private volatile Socket socket;
        private volatile S7Connector s7Connector;
        private final Object ioLock = new Object();

        ConnectionWorker(Long id, String deviceCode, String ip, Integer port) {
            this.id = id;
            this.deviceCode = deviceCode;
            this.ip = ip;
            this.port = port;
        }

        @Override
        public void run() {
            int attempt = 0;
            while (running) {
                attempt++;
                try {
                    connectOnce(attempt);
                    monitorUntilLost();
                    if (running) {
                        log.warn("设备[{}]连接中断，{}秒后重连", deviceCode, RETRY_INTERVAL_MS / 1000);
                        sleep(RETRY_INTERVAL_MS);
                    }
                } finally {
                    closeQuietly(socket);
                    socket = null;
                    closeS7Quietly();
                }
            }
            log.info("设备[{}]连接任务结束", deviceCode);
        }

        private void connectOnce(int attempt) {
            try {
                log.info("设备[{}]第{}次尝试连接 {}:{} (S7 rack={} slot={})", deviceCode, attempt, ip, port, RACK, SLOT);
                socket = new Socket();
                socket.connect(new InetSocketAddress(ip, port), CONNECT_TIMEOUT_MS);
                openS7();
                updateStatus(ConnectStatus.CONNECTED);
                log.info("设备[{}]连接成功", deviceCode);
            } catch (SocketTimeoutException e) {
                cleanupAfterFailure();
                updateStatus(ConnectStatus.CONNECT_TIMEOUT);
                log.warn("设备[{}]连接超时({}ms)，{}秒后重试", deviceCode, CONNECT_TIMEOUT_MS, RETRY_INTERVAL_MS / 1000);
                sleep(RETRY_INTERVAL_MS);
            } catch (IOException | RuntimeException e) {
                cleanupAfterFailure();
                updateStatus(ConnectStatus.CONNECT_FAILED);
                log.warn("设备[{}]连接失败: {}，{}秒后重试", deviceCode, e.getMessage(), RETRY_INTERVAL_MS / 1000);
                sleep(RETRY_INTERVAL_MS);
            }
        }

        private void openS7() {
            try {
                s7Connector = S7ConnectorFactory.buildTCPConnector()
                        .withHost(ip)
                        .withPort(S7_PORT)
                        .withRack(RACK)
                        .withSlot(SLOT)
                        .withTimeout(CONNECT_TIMEOUT_MS)
                        .build();
                log.info("设备[{}]S7 握手成功", deviceCode);
            } catch (Exception e) {
                throw new IllegalStateException("S7 握手失败: " + e.getMessage(), e);
            }
        }

        private void cleanupAfterFailure() {
            closeQuietly(socket);
            socket = null;
            closeS7Quietly();
        }

        private void monitorUntilLost() {
            while (running) {
                sleep(MONITOR_INTERVAL_MS);
                Socket s = socket;
                if (s == null || s.isClosed() || !s.isConnected()) {
                    break;
                }
            }
        }

        boolean isAlive() {
            return running && s7Connector != null
                    && socket != null && socket.isConnected() && !socket.isClosed();
        }

        byte[] readDB(int dbNumber, int start, int size) {
            synchronized (ioLock) {
                S7Connector connector = s7Connector;
                if (connector == null) {
                    throw new IllegalStateException("堆垛机未连接，请先连接设备");
                }
                try {
                    return connector.read(DaveArea.DB, dbNumber, size, start);
                } catch (Exception e) {
                    log.error("设备[{}]读取 DB{} 偏移{} 长度{} 失败: {}", deviceCode, dbNumber, start, size, e.getMessage());
                    throw new IllegalStateException("S7 读取失败: " + e.getMessage());
                }
            }
        }

        private boolean updateStatus(ConnectStatus status) {
            if (!deviceInfoService.updateById(statusEntity(status))) {
                log.error("设备[{}]状态更新失败(可能已删除)，停止连接任务", deviceCode);
                stop();
                return false;
            }
            return true;
        }

        private DeviceInfo statusEntity(ConnectStatus status) {
            DeviceInfo entity = new DeviceInfo();
            entity.setId(id);
            entity.setStatus(status);
            return entity;
        }

        private void sleep(long millis) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                running = false;
            }
        }

        private void closeQuietly(Socket s) {
            if (s != null && !s.isClosed()) {
                try {
                    s.close();
                } catch (IOException e) {
                    log.debug("设备[{}]关闭连接异常: {}", deviceCode, e.getMessage());
                }
            }
        }

        private void closeS7Quietly() {
            S7Connector c = s7Connector;
            s7Connector = null;
            if (c != null) {
                try {
                    c.close();
                } catch (Exception e) {
                    log.debug("设备[{}]关闭 S7 连接异常: {}", deviceCode, e.getMessage());
                }
            }
        }

        void stop() {
            running = false;
            closeQuietly(socket);
            closeS7Quietly();
        }
    }
}
