# AirDrop 局域网文件共享系统

<div align="center">
  <img src="https://via.placeholder.com/150" alt="AirDrop Logo" width="150" height="150">
  <h3>简单、高效的局域网文件传输解决方案</h3>
</div>

## 📝 项目概述

AirDrop 是一个基于 WebSocket 技术的局域网文件共享系统，提供类似 Apple AirDrop 的用户体验。该项目允许局域网内的设备无需借助外部服务器或云存储即可快速安全地交换文件，特别适用于临时文件共享需求。

### 💡 核心特点

- **零配置设备发现**: 自动发现并显示局域网内的所有兼容设备
- **即时文件传输**: 点对点直接传输，无中间服务器转发
- **大文件支持**: 通过分块传输技术支持大文件传输
- **实时传输状态**: 显示传输进度、速度和预计剩余时间
- **跨平台兼容**: 任何支持现代浏览器的设备均可使用
- **无需互联网**: 完全在局域网内运行，无需外部网络连接

## 🛠️ 技术栈

- **后端框架**: Spring Boot 3.2.0
- **WebSocket 实现**: Spring WebSocket
- **前端技术**: HTML5, CSS3, JavaScript
- **构建工具**: Maven
- **Java 版本**: JDK 21
- **模板引擎**: Thymeleaf
- **其他依赖**:
  - Lombok (简化代码)
  - Jackson (JSON 处理)

## 📋 系统架构

### 后端组件

- **DeviceRegistry**: 管理在线设备信息的注册表服务
- **FileTransferHandler**: 处理 WebSocket 连接和文件传输逻辑
- **WebSocketConfig**: 配置 WebSocket 端点和握手处理
- **WebSocketBinaryMessageSizeConfig**: 配置消息缓冲区大小，支持大文件传输

### 数据模型

- **DeviceInfo**: 设备信息模型，包含设备 ID、名称和 WebSocket 会话
- **FileTransferMessage**: 传输消息模型，支持多种消息类型，包括设备信息、传输请求、文件元数据等

### 通信协议

系统使用基于 JSON 的消息协议进行控制通信，使用二进制消息进行实际的文件数据传输。消息类型包括：

- `device_info`: 设备信息注册
- `device_list`: 在线设备列表更新
- `request_transfer`: 发起文件传输请求
- `transfer_response`: 接受或拒绝传输请求
- `file_metadata`: 文件元数据信息
- `file_chunk`: 文件数据块

## 🚀 快速开始

### 环境需求

- JDK 21 或更高版本
- Maven 3.6 或更高版本
- 支持现代 WebSocket 的浏览器

### 构建与运行

1. **克隆项目**

```bash
git clone https://github.com/yourusername/airdrop.git
cd airdrop
```

2. **构建项目**

```bash
mvn clean package
```

3. **运行应用**

```bash
java -jar target/AirDrop-1.0-SNAPSHOT.jar
```

4. **访问应用**

打开浏览器，访问 `http://localhost:8080` 或局域网内的 `http://[服务器IP]:8080`

### 使用方法

1. **设备发现**
   - 启动应用后，系统自动在局域网内广播设备信息
   - 所有在线设备将在界面左侧面板中显示

2. **连接设备**
   - 点击目标设备旁的"连接"按钮
   - 成功连接后，设备状态会变为"已连接"

3. **发送文件**
   - 点击"选择文件"按钮或将文件拖放到指定区域
   - 文件将显示在待发送列表中
   - 点击"发送"按钮向目标设备发送传输请求

4. **接收文件**
   - 收到传输请求时，系统会显示确认对话框
   - 接受请求后，文件传输自动开始
   - 传输完成后，文件会自动下载或提供下载链接

## 📊 文件传输机制

1. **分块传输**
   - 大文件被分割成固定大小的块(默认 1MB)
   - 每个块通过二进制 WebSocket 消息发送
   - 采用滑动窗口机制控制传输速率和可靠性

2. **断点续传**
   - 传输过程记录已传输块的索引
   - 连接中断后可从上次中断位置继续传输
   - 避免重复传输已成功发送的数据

3. **数据验证**
   - 使用校验和验证每个数据块的完整性
   - 接收端重组文件前验证完整性

## 🔧 系统配置

### 应用配置

服务器配置文件位于 `src/main/resources/application.yml`:

```yaml
server:
  port: 8080  # 服务器端口，可根据需要修改

spring:
  servlet:
    multipart:
      max-file-size: 100MB  # 上传文件大小限制
      max-request-size: 100MB
  
  websocket:
    max-text-message-size: 8192  # 文本消息大小限制
    max-binary-message-size: 4194304  # 二进制消息大小限制 (4MB)
```

### WebSocket 配置

WebSocket 消息大小配置位于 `WebSocketBinaryMessageSizeConfig.java`，默认设置为:

- 文本消息缓冲区: 8KB
- 二进制消息缓冲区: 4MB
- 会话超时: 60秒

## 📂 项目结构

```
src/
├── main/
│   ├── java/
│   │   └── com/
│   │       └── example/
│   │           └── localshare/
│   │               ├── config/
│   │               │   ├── WebSocketBinaryMessageSizeConfig.java
│   │               │   └── WebSocketConfig.java
│   │               ├── controller/
│   │               │   └── HomeController.java
│   │               ├── handler/
│   │               │   └── FileTransferHandler.java
│   │               ├── model/
│   │               │   ├── DeviceInfo.java
│   │               │   └── FileTransferMessage.java
│   │               ├── service/
│   │               │   └── DeviceRegistry.java
│   │               └── LocalShareApplication.java
│   └── resources/
│       ├── application.yml
│       ├── static/
│       │   └── index.html
│       └── templates/
│           └── index.html
```

## 🔒 安全考虑

当前版本主要关注功能实现，在生产环境部署前应注意以下安全事项:

1. **WebSocket 安全**
   - 替换当前的 `setAllowedOrigins("*")` 为特定来源
   - 考虑添加 SSL/TLS 加密 WebSocket 连接

2. **文件验证**
   - 实现更严格的文件类型检查
   - 扫描恶意软件或病毒

3. **用户认证**
   - 添加设备认证机制
   - 实现传输请求的确认机制

## 🔄 未来计划

- **点对点加密**: 实现端到端加密确保文件传输安全
- **多文件传输**: 支持同时传输多个文件
- **设备认证**: 添加设备验证码或PIN码机制
- **传输恢复**: 改进断点续传功能
- **传输速率控制**: 添加带宽限制选项
- **移动应用**: 开发配套移动应用程序
- **历史记录**: 添加文件传输历史记录功能
- **文件预览**: 添加常见文件类型的预览功能

## 🤝 贡献指南

欢迎对本项目做出贡献！请按照以下步骤参与:

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'Add some amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 提交 Pull Request

## 📜 许可证

本项目采用 MIT 许可证 - 详情参阅 [LICENSE](LICENSE) 文件

## 👨‍💻 作者

- **[Cy2s1ne]** - *学生* - [https://github.com/Cy2s1ne](https://github.com/yourusername)

## 📝 致谢

- 感谢 Spring 团队提供的优秀 WebSocket 框架
- 感谢所有测试和提供反馈的用户
- 灵感来源于 Apple 的 AirDrop 功能

---

<div align="center">
  © 2025 AirDrop 项目组
</div>