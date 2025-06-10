package com.example.localshare.model;

import lombok.Data;
import java.util.List;

@Data
public class FileTransferMessage {
    // Message type: device_info, device_list, request_transfer, transfer_request, 
    // transfer_response, file_metadata, etc.
    private String type;
    
    // Device information
    private String deviceId;
    private String deviceName;
    private List<DeviceInfo> deviceList;
    
    // Transfer request/response
    private String sourceDeviceId;
    private String sourceDeviceName;
    private String targetDeviceId;
    private String transferId;
    private boolean accepted;
    
    // File information
    private String fileName;
    private long fileSize;
    private String fileType;
    private int chunks;
    private String checksum;
} 