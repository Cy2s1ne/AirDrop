package com.example.localshare.service;

import com.example.localshare.model.DeviceInfo;
import com.example.localshare.model.FileTransferMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class DeviceRegistry {
    private final Map<String, DeviceInfo> onlineDevices = new ConcurrentHashMap<>();
    private final Map<WebSocketSession, String> sessionToDeviceId = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void registerDevice(DeviceInfo deviceInfo) {
        String deviceId = deviceInfo.getDeviceId();
        WebSocketSession session = deviceInfo.getSession();
        
        onlineDevices.put(deviceId, deviceInfo);
        sessionToDeviceId.put(session, deviceId);
        
        log.info("Device registered: {} ({})", deviceInfo.getDeviceName(), deviceId);
    }
    
    public void unregisterDevice(String deviceId) {
        DeviceInfo deviceInfo = onlineDevices.remove(deviceId);
        if (deviceInfo != null) {
            sessionToDeviceId.remove(deviceInfo.getSession());
            log.info("Device unregistered: {} ({})", deviceInfo.getDeviceName(), deviceId);
        }
    }
    
    public String getDeviceIdFromSession(WebSocketSession session) {
        return sessionToDeviceId.get(session);
    }
    
    public WebSocketSession getSession(String deviceId) {
        DeviceInfo deviceInfo = onlineDevices.get(deviceId);
        return deviceInfo != null ? deviceInfo.getSession() : null;
    }
    
    public String getDeviceName(String deviceId) {
        DeviceInfo deviceInfo = onlineDevices.get(deviceId);
        return deviceInfo != null ? deviceInfo.getDeviceName() : null;
    }
    
    public List<DeviceInfo> getAllDevices() {
        return new ArrayList<>(onlineDevices.values());
    }
    
    public void broadcastDeviceList() {
        List<DeviceInfo> devices = getAllDevices();
        
        // Create a simplified list for broadcasting (without session objects)
        List<DeviceInfo> simplifiedDevices = new ArrayList<>();
        for (DeviceInfo device : devices) {
            DeviceInfo simplified = new DeviceInfo();
            simplified.setDeviceId(device.getDeviceId());
            simplified.setDeviceName(device.getDeviceName());
            simplifiedDevices.add(simplified);
        }
        
        FileTransferMessage message = new FileTransferMessage();
        message.setType("device_list");
        message.setDeviceList(simplifiedDevices);
        
        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            TextMessage textMessage = new TextMessage(jsonMessage);
            
            for (DeviceInfo device : devices) {
                WebSocketSession session = device.getSession();
                if (session != null && session.isOpen()) {
                    try {
                        session.sendMessage(textMessage);
                    } catch (IOException e) {
                        // 只记录错误，但不阻止其他消息发送
                        log.error("Failed to send device list to {}", device.getDeviceId(), e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to broadcast device list", e);
        }
    }
} 