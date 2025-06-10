package com.example.localshare.handler;

import com.example.localshare.model.DeviceInfo;
import com.example.localshare.model.FileTransferMessage;
import com.example.localshare.service.DeviceRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class FileTransferHandler extends AbstractWebSocketHandler {

    @Autowired
    private DeviceRegistry deviceRegistry;

    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Map to track file transfer sessions
    private final Map<String, Map<String, Object>> fileTransfers = new ConcurrentHashMap<>();
    
    // 存储已使用的设备名称
    private static final Set<String> usedDeviceNames = new HashSet<>();
    
    // 快餐食品列表（麦当劳和汉堡王单项）
    private static final String[] FAST_FOOD_ITEMS = {
        "巨无霸", "双层吉士堡", "麦辣鸡腿堡", "麦香鸡", "麦乐鸡", "麦香鱼", "苹果派", "双层脆鸡堡",
        "麦当劳薯条", "麦旋风", "麦辣鸡翅", "麦炫酷", "双层猪柳蛋麦满分", "双层培根芝士牛堡", "不素之霸",
        "大皇堡", "双层皇堡", "大嘴安格斯", "芝士皇堡", "火烤牛肉堡", "霸王鸡条", "经典安格斯", "薯霸王",
        "培根安格斯厚牛堡", "2层芝士牛堡", "3层芝士牛堡", "洋葱圈", "上校鸡块", "板烧鸡腿堡", "老北京鸡肉卷",
        "芝士安格斯厚牛堡","麦麦脆汁鸡"
    };
    
    private final Random random = new Random();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String deviceId = generateDeviceId(session);
        String deviceName = generateDeviceName();
        
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setDeviceId(deviceId);
        deviceInfo.setDeviceName(deviceName);
        deviceInfo.setSession(session);
        
        // Register the device
        deviceRegistry.registerDevice(deviceInfo);
        
        try {
            // Send device ID to the client
            FileTransferMessage message = new FileTransferMessage();
            message.setType("device_info");
            message.setDeviceId(deviceId);
            message.setDeviceName(deviceName);
            
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
            
            // Broadcast updated device list
            deviceRegistry.broadcastDeviceList();
        } catch (IOException e) {
            log.error("Error during connection establishment", e);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String payload = message.getPayload();
            log.debug("Received text message: {}", payload);
            FileTransferMessage transferMessage = objectMapper.readValue(payload, FileTransferMessage.class);
            
            switch (transferMessage.getType()) {
                case "request_transfer":
                    handleTransferRequest(session, transferMessage);
                    break;
                case "transfer_response":
                    handleTransferResponse(session, transferMessage);
                    break;
                case "file_metadata":
                    handleFileMetadata(session, transferMessage);
                    break;
                case "request_device_list":
                    // 处理刷新设备列表请求
                    deviceRegistry.broadcastDeviceList();
                    break;
                default:
                    log.info("Received unknown message type: {}", transferMessage.getType());
            }
        } catch (Exception e) {
            log.error("Error handling text message", e);
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        try {
            String senderId = getDeviceIdFromSession(session);
            if (senderId == null) {
                log.error("Received binary message from unknown sender");
                return;
            }
            
            String receiverId = null;
            String transferId = null;
            
            // Find the active file transfer for this sender
            for (Map.Entry<String, Map<String, Object>> entry : fileTransfers.entrySet()) {
                Map<String, Object> transfer = entry.getValue();
                if (senderId.equals(transfer.get("senderId"))) {
                    receiverId = (String) transfer.get("receiverId");
                    transferId = entry.getKey();
                    break;
                }
            }
            
            if (receiverId == null) {
                log.error("No active file transfer found for sender: {}", senderId);
                return;
            }
            
            WebSocketSession receiverSession = deviceRegistry.getSession(receiverId);
            if (receiverSession != null && receiverSession.isOpen()) {
                try {
                    receiverSession.sendMessage(message);
                    log.debug("Binary message forwarded: {} bytes from {} to {} (transfer: {})", 
                             message.getPayloadLength(), senderId, receiverId, transferId);
                } catch (IOException e) {
                    log.error("Failed to forward binary message to receiver: {}", receiverId, e);
                }
            } else {
                log.error("Receiver session not found or closed: {}", receiverId);
                // 通知发送者接收方已断开连接
                try {
                    FileTransferMessage errorMessage = new FileTransferMessage();
                    errorMessage.setType("transfer_error");
                    errorMessage.setTransferId(transferId);
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(errorMessage)));
                } catch (IOException ex) {
                    log.error("Failed to send error notification to sender", ex);
                }
            }
        } catch (Exception e) {
            log.error("Error handling binary message", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        try {
            String deviceId = getDeviceIdFromSession(session);
            if (deviceId != null) {
                // 获取设备名称并从已使用集合中移除
                String deviceName = deviceRegistry.getDeviceName(deviceId);
                if (deviceName != null) {
                    synchronized (usedDeviceNames) {
                        usedDeviceNames.remove(deviceName);
                    }
                    log.debug("Released device name: {}", deviceName);
                }
                
                deviceRegistry.unregisterDevice(deviceId);
                
                // Clean up any file transfers involving this device
                fileTransfers.entrySet().removeIf(entry -> {
                    Map<String, Object> transfer = entry.getValue();
                    return deviceId.equals(transfer.get("senderId")) || 
                           deviceId.equals(transfer.get("receiverId"));
                });
                
                // Broadcast updated device list
                deviceRegistry.broadcastDeviceList();
            }
        } catch (Exception e) {
            log.error("Error handling connection closed", e);
        }
    }

    private void handleTransferRequest(WebSocketSession session, FileTransferMessage message) throws IOException {
        String senderId = getDeviceIdFromSession(session);
        String receiverId = message.getTargetDeviceId();
        
        WebSocketSession receiverSession = deviceRegistry.getSession(receiverId);
        if (receiverSession != null && receiverSession.isOpen()) {
            // Create a transfer ID
            String transferId = UUID.randomUUID().toString();
            
            // Store transfer information
            Map<String, Object> transferInfo = new ConcurrentHashMap<>();
            transferInfo.put("senderId", senderId);
            transferInfo.put("receiverId", receiverId);
            transferInfo.put("fileName", message.getFileName());
            transferInfo.put("fileSize", message.getFileSize());
            fileTransfers.put(transferId, transferInfo);
            
            // Forward the request to the receiver
            message.setType("transfer_request");
            message.setTransferId(transferId);
            message.setSourceDeviceId(senderId);
            message.setSourceDeviceName(deviceRegistry.getDeviceName(senderId));
            
            receiverSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
        } else {
            log.error("Receiver not found or not connected: {}", receiverId);
        }
    }

    private void handleTransferResponse(WebSocketSession session, FileTransferMessage message) throws IOException {
        String transferId = message.getTransferId();
        boolean accepted = message.isAccepted();
        
        Map<String, Object> transferInfo = fileTransfers.get(transferId);
        if (transferInfo != null) {
            String senderId = (String) transferInfo.get("senderId");
            WebSocketSession senderSession = deviceRegistry.getSession(senderId);
            
            if (senderSession != null && senderSession.isOpen()) {
                message.setType("transfer_response");
                
                if (!accepted) {
                    // If rejected, clean up the transfer
                    fileTransfers.remove(transferId);
                }
                
                senderSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
            } else {
                log.error("Sender not found or not connected: {}", senderId);
            }
        } else {
            log.error("Transfer not found: {}", transferId);
        }
    }

    private void handleFileMetadata(WebSocketSession session, FileTransferMessage message) throws IOException {
        String transferId = message.getTransferId();
        
        if (transferId == null || transferId.isEmpty()) {
            log.error("Received file metadata with empty transferId from {}", getDeviceIdFromSession(session));
            
            // 尝试查找该发送者的任何活跃传输
            String senderId = getDeviceIdFromSession(session);
            for (Map.Entry<String, Map<String, Object>> entry : fileTransfers.entrySet()) {
                Map<String, Object> transfer = entry.getValue();
                if (senderId.equals(transfer.get("senderId"))) {
                    transferId = entry.getKey();
                    log.info("Found active transfer for sender {}: {}", senderId, transferId);
                    break;
                }
            }
            
            if (transferId == null || transferId.isEmpty()) {
                log.error("Could not find any active transfer for sender: {}", senderId);
                return;
            }
        }
        
        Map<String, Object> transferInfo = fileTransfers.get(transferId);
        
        if (transferInfo != null) {
            String receiverId = (String) transferInfo.get("receiverId");
            WebSocketSession receiverSession = deviceRegistry.getSession(receiverId);
            
            if (receiverSession != null && receiverSession.isOpen()) {
                // 确保元数据中包含正确的transferId
                message.setTransferId(transferId);
                receiverSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
            } else {
                log.error("Receiver not found or not connected: {}", receiverId);
            }
        } else {
            log.error("Transfer not found: {}", transferId);
        }
    }

    private String generateDeviceId(WebSocketSession session) {
        return UUID.randomUUID().toString();
    }
    
    private String generateDeviceName() {
        String foodItem = FAST_FOOD_ITEMS[random.nextInt(FAST_FOOD_ITEMS.length)];
        int number = random.nextInt(991) + 10; // 生成10-1000之间的随机数
        String deviceName = foodItem + " #" + number;
        
        // 确保设备名称不重复
        synchronized (usedDeviceNames) {
            // 如果名称已被使用，重新生成一个
            while (usedDeviceNames.contains(deviceName)) {
                foodItem = FAST_FOOD_ITEMS[random.nextInt(FAST_FOOD_ITEMS.length)];
                number = random.nextInt(991) + 10;
                deviceName = foodItem + " #" + number;
            }
            
            // 记录已使用的设备名称
            usedDeviceNames.add(deviceName);
        }
        
        return deviceName;
    }
    
    private String getDeviceIdFromSession(WebSocketSession session) {
        return deviceRegistry.getDeviceIdFromSession(session);
    }
} 