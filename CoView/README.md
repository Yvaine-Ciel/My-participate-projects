# CoView

CoView 是一个 0 成本取向的临时共同观影网页项目。用户无需注册登录，先选择房主或房客身份，再创建或加入临时房间。房主填写视频页面或视频资源地址，后端自动判断使用同步播放模式还是屏幕共享模式。

## 核心方案

- 同步播放模式：适用于 `.mp4`、`.webm`、`.m3u8` 等可由浏览器直接播放和控制的资源。每位成员从原始视频地址加载资源，服务器只通过 WebSocket 同步播放、暂停和进度。
- 屏幕共享模式：适用于腾讯视频、抖音、B 站、爱奇艺、YouTube 等无法稳定嵌入或控制的第三方页面。房主在原网站正常播放，通过 WebRTC 共享浏览器标签页画面和声音。
- 后端不存储、不下载、不转发影视资源，只负责临时房间、模式判断、WebSocket 状态同步和 WebRTC 信令协调。
- 房主第二步会先看到系统随机生成的候选房间号，固定带有 `LJX-` 前缀，例如 `LJX-a1B2c3`。只有创建成功并进入房间后，该房间号才真正生效；房间存在期间房间号保持唯一。
- 房主创建房间时必须设置房间密码并二次确认，房客加入时需要确认房间号并输入密码，再等待房主同意后进入。
- 后端会从复制的分享文案中提取第一个可打开的网址，再进行播放模式判断。
- 房间侧栏提供可折叠交流对话框，房主和房客可以实时发送文字消息。
- 房客退出时仅从成员列表移除；房主退出时关闭整个临时房间。

## 技术栈

- 前端：React 静态页面，直接由 Spring Boot 托管
- 后端：Spring Boot 3 + REST + WebSocket
- 实时媒体：WebRTC 浏览器点对点连接
- 数据：内存临时存储，房间 12 小时无活动后自动清理

## 本地运行

需要 Java 17 和 Maven。

```bash
mvn spring-boot:run
```

打开：

```text
http://localhost:8080
```

打包：

```bash
mvn clean package
java -jar target/CoView-1.0-SNAPSHOT.jar
```

## API

- `GET /api/health`：健康检查
- `GET /api/rooms/candidate-id`：生成一个尚未生效的候选房间号
- `POST /api/rooms`：创建房间
- `GET /api/rooms/{roomId}`：读取房间
- `POST /api/rooms/{roomId}/join-requests`：房客提交加入申请
- `GET /api/rooms/{roomId}/join-requests?participantId=...`：房主读取待确认申请
- `GET /api/rooms/{roomId}/join-requests/{requestId}`：房客轮询申请状态
- `POST /api/rooms/{roomId}/join-requests/{requestId}/decision`：房主同意或拒绝加入申请
- `PUT /api/rooms/{roomId}/source`：房主更换视频来源
- `WS /ws/rooms/{roomId}?participantId=...`：播放同步和 WebRTC 信令

## 0 成本边界

本项目不依赖数据库、对象存储、视频转码、CDN、账号系统或收费实时音视频服务。房间、成员、密码校验信息和聊天消息都按临时房间思路处理，一个 Java 进程即可运行完整 MVP。

WebRTC 使用公开 STUN 服务器辅助建连，不包含 TURN 中继。大多数普通网络可以尝试直连；如果双方处于严格 NAT 或企业网络，连接可能失败。稳定商用通常需要 TURN 服务器，但这会引入额外成本或运维资源。

## 合规边界

CoView 不提供影视资源，不缓存媒体文件，也不绕过第三方平台限制。受限平台页面使用屏幕共享，由房主在原网站正常播放。
