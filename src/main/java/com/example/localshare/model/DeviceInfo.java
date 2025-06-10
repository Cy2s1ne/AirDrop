package com.example.localshare.model;

import lombok.Data;
import org.springframework.web.socket.WebSocketSession;

@Data
public class DeviceInfo {
    private String deviceId;
    private String deviceName;
    private WebSocketSession session;
} 