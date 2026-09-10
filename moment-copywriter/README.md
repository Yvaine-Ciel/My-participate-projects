# Moment Copywriter

朋友圈文案生成小程序。项目包含 uni-app 前端和 Java Servlet 后端，支持用户注册登录、AI 文案生成、隐藏标签上下文、继续优化、生成过程记录、复制结果、历史记录、收藏管理和清空历史。

本文档面向公开仓库编写，只使用相对路径和占位符，不包含个人电脑绝对路径、真实账号密码、真实 API Key 或固定本机访问地址。

## 功能概览

- 账号注册、登录、退出登录。
- 基于场景、心情、风格、关键词生成朋友圈文案。
- 支持朋友圈文案、节日祝福、自我介绍、演讲稿、短视频配文、治愈短句等场景。
- 支持用户标签，生成和继续优化时会作为隐藏上下文参与 AI 生成。
- 支持对已生成文案继续优化，并记录每次生成和优化过程。
- 生成结果可复制到剪贴板。
- 登录用户可保存生成历史。
- 支持收藏、取消收藏、只看收藏。
- 支持删除单条历史记录、清空全部历史记录；清空历史会移除优化过程记录，但保留已收藏文案。

## 技术栈

前端：

- uni-app
- Vue 3
- 微信小程序配置

后端：

- Java Servlet
- JDBC
- SQL Server
- Tomcat 9.x
- Gson
- Microsoft SQL Server JDBC Driver

## 项目结构

```text
.
├── App.vue
├── main.js
├── manifest.json
├── pages.json
├── common/
│   ├── auth.js
│   ├── config.js
│   ├── favorites.js
│   └── request.js
├── components/
│   └── app-tabbar/
├── pages/
│   ├── index/
│   ├── history/
│   ├── profile/
│   ├── login/
│   └── register/
├── start-backend.bat
└── web/
    ├── README_BACKEND.md
    ├── start-backend.bat
    ├── sql/
    │   └── init.sql
    └── src/
        ├── dao/
        ├── entity/
        ├── servlet/
        ├── util/
        └── web/
            └── WEB-INF/
```

## 环境要求

前端开发：

- HBuilderX
- 微信开发者工具

后端开发：

- JDK 11 或更高版本
- Tomcat 9.x
- SQL Server

后端依赖 JAR 已放在：

```text
web/src/web/WEB-INF/lib/
```

其中 `mssql-jdbc-13.2.1.jre11.jar` 需要配合 JDK 11+ 使用。不要直接使用 Tomcat 10，因为本项目代码使用的是 `javax.servlet`。

## 环境变量

后端运行前需要配置数据库、AI 服务和 Java/Tomcat 环境变量。

数据库：

```text
MOMENT_DB_URL=jdbc:sqlserver://<数据库主机或实例>;databaseName=MomentCopywriter;encrypt=true;trustServerCertificate=true
MOMENT_DB_USER=<数据库用户名>
MOMENT_DB_PASSWORD=<数据库密码>
```

AI 服务：

```text
AI_API_URL=<兼容 Chat Completions 的 API 地址>
AI_API_KEY=<AI 服务密钥>
AI_MODEL=<模型名称>
```

Java 和 Tomcat：

```text
JAVA_HOME=<JDK 安装目录>
CATALINA_HOME=<Tomcat 安装目录>
BACKEND_HEALTH_URL=<可选，后端健康检查完整地址>
```

安全要求：

- 不要把真实数据库密码、真实 API Key、个人目录、绝对路径写进代码、README、截图或公开仓库。
- 不要提交 `.env`、日志、编译产物、IDE 私有配置。
- 生产环境建议使用低权限数据库账号，不要使用数据库管理员账号。
- 数据库不要直接暴露到公网。

## 数据库初始化

数据库初始化脚本：

```text
web/sql/init.sql
```

使用 SQL Server 管理工具或命令行工具执行该脚本。连接地址、实例名、用户名和密码按自己的环境填写。

通用命令格式：

```powershell
sqlcmd -S "<数据库服务名或地址>" -U "<用户名>" -P "<密码>" -C -i "web\sql\init.sql"
```

脚本会创建 `MomentCopywriter` 数据库和 5 张业务表：

- `users`：用户表。
- `user_tags`：用户标签表。
- `copywriting_records`：文案生成历史表。
- `copywriting_record_steps`：生成和优化过程记录表。
- `favorites`：收藏表。

脚本已做存在性判断，重复执行不会主动删除已有数据；如果旧环境缺少表或索引，会补建缺失对象。清空历史功能依赖 `copywriting_records.user_id` 可为空，以便已收藏文案从历史中移除后仍可保留在收藏列表。

数据库字段、索引和外键说明见：

```text
web/README_BACKEND.md
```

## 启动后端

后端不能由 HBuilderX 自动启动，需要单独运行 Tomcat。项目提供了 Windows 启动脚本。

在 Windows 文件资源管理器中打开项目根目录，双击：

```text
start-backend.bat
```

也可以进入后端目录双击：

```text
web/start-backend.bat
```

脚本会自动完成：

1. 读取本机环境变量。
2. 检查 JDK、Tomcat 和后端依赖 JAR。
3. 编译 Java 源码。
4. 部署后端到 Tomcat。
5. 启动 Tomcat。
6. 如果配置了 `BACKEND_HEALTH_URL`，自动打开健康检查页面。

只检查环境和编译，不启动 Tomcat：

```powershell
.\start-backend.bat --check
```

后端健康检查接口的相对路径：

```text
/api/health
```

完整访问地址由实际服务器地址、端口和应用上下文决定。

## 启动前端

使用 HBuilderX 打开项目根目录，然后运行到微信开发者工具。

前端请求后端的基础地址配置在：

```text
common/config.js
```

运行前需要确保该配置与当前后端实际部署地址一致。不同电脑、不同 Tomcat 配置、不同部署方式下地址可能不同。

## 前后端运行关系

前端和后端是分开运行的：

- HBuilderX / 微信开发者工具：运行小程序前端页面。
- Tomcat：运行 Java 后端接口。
- SQL Server：保存用户、历史记录和收藏数据。
- AI 服务：生成朋友圈文案。

如果只运行前端，页面可以打开，但注册、登录、生成文案、历史记录和收藏相关功能会因为后端不可用而失败。

## 页面说明

| 页面 | 路径 | 说明 |
| --- | --- | --- |
| 文案生成 | `pages/index/index` | 输入需求、选择文案类型、调用 AI 生成文案、继续优化、复制和收藏结果。 |
| 历史记录 | `pages/history/history` | 查看生成历史和优化过程，支持只看收藏、复制、收藏切换、删除记录。 |
| 我的 | `pages/profile/profile` | 查看登录状态，进入收藏和历史，管理标签，清空历史，退出登录。 |
| 登录 | `pages/login/login` | 用户登录。 |
| 注册 | `pages/register/register` | 用户注册。 |

## 后端接口概览

接口基础地址由实际部署环境决定，下面只列出相对路径。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/health` | 健康检查。 |
| `POST` | `/api/register` | 注册用户。 |
| `POST` | `/api/login` | 登录用户。 |
| `POST` | `/api/logout` | 退出登录。 |
| `GET` | `/api/current-user` | 获取当前登录用户。 |
| `GET` | `/api/user-tags` | 获取当前用户标签。 |
| `POST` | `/api/user-tags/add` | 添加用户标签。 |
| `POST` | `/api/user-tags/delete` | 删除用户标签。 |
| `POST` | `/api/copywriting/generate` | 生成文案并保存历史。 |
| `POST` | `/api/copywriting/optimize` | 继续优化已生成文案，并保存优化过程。 |
| `GET` / `POST` | `/api/copywriting/steps` | 获取某条文案的生成和优化过程。 |
| `GET` / `POST` | `/api/copywriting/history` | 获取历史记录。 |
| `GET` / `POST` | `/api/copywriting/favorites` | 获取收藏记录。 |
| `POST` | `/api/copywriting/favorite/add` | 添加收藏。 |
| `POST` / `DELETE` | `/api/copywriting/favorite/delete` | 取消收藏。 |
| `POST` / `DELETE` | `/api/copywriting/delete` | 删除单条历史。 |
| `POST` / `DELETE` | `/api/copywriting/clear-history` | 清空历史和优化过程，保留收藏。 |

完整接口参数、返回数据和数据库表设计见：

```text
web/README_BACKEND.md
```

## 备注

本项目当前没有使用 Maven、Gradle 或 npm 包管理后端依赖，后端依赖 JAR 放在 `web/src/web/WEB-INF/lib/` 中。后续如果要多人协作或部署到服务器，建议逐步迁移到标准构建工具，减少手动配置成本。
