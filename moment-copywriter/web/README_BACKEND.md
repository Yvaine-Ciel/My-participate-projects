# Moment Copywriter 后端说明

这是朋友圈文案生成小程序的 Java 后端，技术栈是 Servlet + JDBC + SQL Server。本文档只说明通用运行思路，不包含任何个人电脑的绝对路径、真实账号密码、真实 API Key 或固定本机地址。

## 目录结构

- `web/src/entity`：JavaBean 实体对象。
- `web/src/dao`：JDBC 数据库访问代码。
- `web/src/servlet`：HTTP API 接口代码。
- `web/src/util`：数据库、JSON、跨域、密码、AI 调用等工具类。
- `web/src/web`：可部署到 Tomcat 的 Web 根目录。
- `web/sql/init.sql`：SQL Server 数据库初始化脚本。
- `start-backend.bat`：项目根目录下的后端启动入口。
- `web/start-backend.bat`：后端目录下的实际启动脚本。

## 本机环境要求

- JDK 11 或更高版本。
- Tomcat 9.x，或其他支持 `javax.servlet` 的 Servlet 容器。
- SQL Server。
- `web/src/web/WEB-INF/lib/gson-2.13.1.jar`。
- `web/src/web/WEB-INF/lib/mssql-jdbc-13.2.1.jre11.jar`。

当前项目使用的是 `mssql-jdbc-13.2.1.jre11.jar`，所以 Tomcat 必须运行在 JDK 11+ 上。不要直接换成 Tomcat 10，因为 Tomcat 10 使用 `jakarta.servlet`，而本项目代码使用的是 `javax.servlet`。

## 环境变量

数据库配置：

```text
MOMENT_DB_URL=jdbc:sqlserver://<数据库主机或实例>;databaseName=MomentCopywriter;encrypt=true;trustServerCertificate=true
MOMENT_DB_USER=<数据库用户名>
MOMENT_DB_PASSWORD=<数据库密码>
```

AI 服务配置：

```text
AI_API_URL=<兼容 Chat Completions 的 API 地址>
AI_API_KEY=<AI 服务密钥>
AI_MODEL=<模型名称>
```

运行环境配置：

```text
JAVA_HOME=<JDK 安装目录>
CATALINA_HOME=<Tomcat 安装目录>
BACKEND_HEALTH_URL=<可选，后端健康检查完整地址>
```

安全要求：

- 不要把真实数据库密码、真实 API Key、个人目录、绝对路径写进 README、代码、截图或公开仓库。
- 不要提交 `.env`、本机启动脚本副本、IDE 私有配置、日志、编译产物。
- 生产环境不要使用数据库管理员账号，建议单独创建权限较低的业务账号。
- 数据库不要直接暴露到公网，优先限制为本机、内网或服务器访问。

## 数据库初始化

数据库结构脚本在：

```text
web/sql/init.sql
```

使用自己的 SQL Server 管理工具或命令行工具执行该脚本。执行时需要根据自己电脑或服务器的数据库地址、实例名、用户名和密码填写连接信息。

通用命令格式如下：

```powershell
sqlcmd -S "<数据库服务名或地址>" -U "<用户名>" -P "<密码>" -C -i "web\sql\init.sql"
```

脚本已做存在性判断：数据库、表和索引存在时会跳过，不会主动删除已有数据；旧环境缺少新表或索引时会补建缺失对象。

## 后端启动思路

最简单的方式是使用项目自带的启动脚本。不要在 README 预览页里点击 `.bat` 链接，应该在 Windows 文件资源管理器里找到实际文件后双击运行。

启动入口位置：

```text
项目根目录/start-backend.bat
```

也可以运行后端目录里的脚本：

```text
项目根目录/web/start-backend.bat
```

脚本的工作流程：

1. 读取本机环境变量中的 JDK、Tomcat、数据库和 AI 配置。
2. 编译 `web/src` 下的 Java 源码。
3. 将 `web/src/web` 部署到 Tomcat 的 `webapps` 目录。
4. 将编译后的 `.class` 文件放入部署目录的 `WEB-INF/classes`。
5. 启动 Tomcat。
6. 如果配置了 `BACKEND_HEALTH_URL`，自动打开健康检查接口。

如果只想检查环境和编译是否正常，不想启动 Tomcat，可以在项目根目录执行：

```powershell
.\start-backend.bat --check
```

手动启动时，思路也是一样的：先配置环境变量，再编译 Java 源码，然后把 Web 根目录和编译结果放到 Servlet 容器规定的位置，最后启动 Tomcat。

健康检查接口的相对路径是：

```text
/api/health
```

完整访问地址由实际部署的服务器地址、端口和应用上下文决定。

## 前后端运行关系

前端和后端是分开运行的：

- HBuilderX / 微信开发者工具：运行前端页面。
- Tomcat：运行 Java 后端接口。
- SQL Server：保存用户、标签、历史记录、优化过程和收藏数据。
- AI 服务：生成朋友圈文案。

如果只运行 HBuilderX，页面可以打开，但注册、登录、历史记录、收藏、生成文案等功能会因为后端没有启动而失败。

前端请求地址需要与后端部署后的访问地址一致。不同电脑、不同 Tomcat 配置、不同部署方式下地址可能不同，应按实际环境配置。

## 接口通用说明

接口基础地址由实际部署环境决定。下面只列出应用内的相对路径。

请求参数支持常见表单参数；当请求头为 `application/json` 时，也支持从 JSON 请求体读取字段。

所有接口默认返回 JSON，成功格式：

```json
{
  "success": true,
  "message": "OK",
  "data": {}
}
```

失败格式：

```json
{
  "success": false,
  "message": "错误信息"
}
```

需要登录的接口依赖 Servlet Session。登录成功后，客户端需要保留并继续携带服务端返回的会话 Cookie。

## 接口列表

| 方法 | 路径 | 是否需要登录 | 参数 | 说明 |
| --- | --- | --- | --- | --- |
| `GET` | `/api/health` | 否 | 无 | 健康检查，确认后端是否启动。 |
| `POST` | `/api/register` | 否 | `username` 必填，`password` 必填，`phone` 可选 | 注册用户。 |
| `POST` | `/api/login` | 否 | `username` 必填，`password` 必填 | 登录用户，成功后写入 Session。 |
| `POST` | `/api/logout` | 否 | 无 | 退出登录，销毁当前 Session。 |
| `GET` | `/api/current-user` | 是 | 无 | 获取当前登录用户。 |
| `GET` | `/api/user-tags` | 是 | 无 | 获取当前用户标签。 |
| `POST` | `/api/user-tags/add` | 是 | `name` 必填，不超过 12 个字 | 添加用户标签，返回最新标签列表。 |
| `POST` | `/api/user-tags/delete` | 是 | `name` 必填 | 删除用户标签，返回最新标签列表。 |
| `POST` | `/api/copywriting/generate` | 是 | `scene` 必填，`mood` 可选，`style` 可选，`keywords` 可选 | 调用 AI 服务生成朋友圈文案，并保存历史记录。 |
| `POST` | `/api/copywriting/optimize` | 是 | `recordId` 或 `id` 必填，`message` 必填 | 按用户要求继续优化文案，更新当前文案内容，并保存优化过程。 |
| `GET` / `POST` | `/api/copywriting/steps` | 是 | `recordId` 或 `id` 必填 | 获取某条文案的生成和优化过程。 |
| `GET` / `POST` | `/api/copywriting/history` | 是 | 无 | 获取当前用户最近 50 条文案历史。 |
| `GET` / `POST` | `/api/copywriting/favorites` | 是 | 无 | 获取当前用户最近 50 条收藏记录。 |
| `POST` | `/api/copywriting/favorite/add` | 是 | `recordId` 或 `id` 必填 | 收藏一条属于当前用户的文案记录。 |
| `POST` / `DELETE` | `/api/copywriting/favorite/delete` | 是 | `recordId` 或 `id` 必填 | 取消收藏一条属于当前用户的文案记录。 |
| `POST` / `DELETE` | `/api/copywriting/delete` | 是 | `id` 必填 | 删除一条属于当前用户的文案历史。 |
| `POST` / `DELETE` | `/api/copywriting/clear-history` | 是 | 无 | 清空当前用户的文案历史和优化过程，保留收藏，并返回删除数量。 |
| `OPTIONS` | 所有接口 | 否 | 无 | 跨域预检请求。 |

## 接口返回数据

`GET /api/health` 成功时 `data` 包含：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `status` | `string` | 服务状态。 |
| `service` | `string` | 服务名称。 |

`POST /api/register` 成功时 `data` 包含：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `username` | `string` | 注册成功的用户名。 |

`POST /api/login` 和 `GET /api/current-user` 成功时 `data.user` 包含：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `number` | 用户 ID。 |
| `username` | `string` | 用户名。 |
| `phone` | `string` | 手机号，可为空。 |
| `role` | `string` | 用户角色。 |
| `createTime` | `string` | 创建时间。 |

`GET /api/user-tags` 成功时 `data` 是字符串数组，按创建时间升序排列。

`POST /api/user-tags/add` 和 `POST /api/user-tags/delete` 成功时 `data` 包含：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `tags` | `string[]` | 当前用户最新标签列表。 |

`POST /api/copywriting/generate` 成功时 `data` 包含：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `content` | `string` | AI 生成的朋友圈文案。 |
| `recordId` | `number` | 保存后的文案记录 ID；保存失败时可能为 `0`。 |
| `keywords` | `string` | 本次请求传入并保存的关键词，可为空；用户标签只作为隐藏生成上下文使用，不写入该字段。 |
| `saved` | `boolean` | 是否成功保存历史记录。 |
| `model` | `string` | 实际使用的模型名称。 |

`POST /api/copywriting/optimize` 成功时 `data` 包含：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `recordId` | `number` | 被优化的文案记录 ID。 |
| `content` | `string` | 优化后的最新文案内容。 |
| `stepNo` | `number` | 本次优化保存后的步骤序号。 |
| `steps` | `CopywritingRecordStep[]` | 当前文案的完整生成和优化过程。 |

`GET /api/copywriting/steps` 和 `POST /api/copywriting/steps` 成功时 `data` 是步骤数组，每条步骤包含：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `number` | 步骤 ID。 |
| `recordId` | `number` | 所属文案记录 ID。 |
| `stepNo` | `number` | 步骤序号，初始生成是 `1`，后续优化依次递增。 |
| `userMessage` | `string` | 本次生成要求或优化要求。 |
| `generatedContent` | `string` | 该步骤生成后的文案内容。 |
| `createTime` | `string` | 步骤创建时间。 |

历史和收藏接口返回文案记录数组，每条记录包含：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `number` | 文案记录 ID。 |
| `userId` | `number` | 所属用户 ID；清空历史后仍保留的收藏记录可能返回 `0`。 |
| `scene` | `string` | 场景。 |
| `mood` | `string` | 心情，可为空。 |
| `style` | `string` | 风格，可为空。 |
| `keywords` | `string` | 关键词，可为空。 |
| `generatedContent` | `string` | 生成内容。 |
| `aiModel` | `string` | 生成时使用的模型。 |
| `createTime` | `string` | 创建时间。 |
| `favorite` | `boolean` | 是否已收藏。 |
| `favoriteTime` | `string` | 收藏时间，可为空。 |

收藏和清空历史接口的额外返回：

| 接口 | 字段 | 类型 | 说明 |
| --- | --- | --- | --- |
| `/api/copywriting/favorite/add` | `recordId` | `number` | 被收藏的文案记录 ID。 |
| `/api/copywriting/favorite/add` | `favorite` | `boolean` | 固定为 `true`。 |
| `/api/copywriting/favorite/delete` | `recordId` | `number` | 被取消收藏的文案记录 ID。 |
| `/api/copywriting/favorite/delete` | `favorite` | `boolean` | 固定为 `false`。 |
| `/api/copywriting/clear-history` | `deletedCount` | `number` | 本次从历史中清空的记录数量；已收藏文案仍保留在收藏列表。 |

## 数据库表设计

数据库初始化脚本会创建 `MomentCopywriter` 数据库和 5 张业务表。

### users

用户表，保存账号、密码哈希和基础资料。

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| `id` | `INT IDENTITY(1,1)` | 主键 | 用户 ID。 |
| `username` | `NVARCHAR(50)` | 非空，唯一 | 用户名。 |
| `password_hash` | `VARCHAR(64)` | 非空 | 加盐后的密码哈希，不保存明文密码。 |
| `password_salt` | `VARCHAR(32)` | 非空 | 密码盐值。 |
| `phone` | `NVARCHAR(20)` | 可空 | 手机号。 |
| `role` | `VARCHAR(20)` | 非空，默认 `user` | 用户角色。 |
| `create_time` | `DATETIME` | 非空，默认当前时间 | 用户创建时间。 |

### user_tags

用户标签表，保存用户用于生成文案的个性化标签。

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| `id` | `INT IDENTITY(1,1)` | 主键 | 标签记录 ID。 |
| `user_id` | `INT` | 非空，外键 | 标签所属用户。 |
| `name` | `NVARCHAR(20)` | 非空 | 标签名称；接口层限制不超过 12 个字。 |
| `create_time` | `DATETIME` | 非空，默认当前时间 | 标签创建时间。 |

唯一约束：

```text
uq_user_tags_user_name(user_id, name)
```

索引：

```text
idx_user_tags_user_time(user_id, create_time ASC)
```

外键：

```text
user_tags.user_id -> users.id
ON DELETE CASCADE
```

### copywriting_records

文案历史表，保存每次 AI 生成的内容。

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| `id` | `INT IDENTITY(1,1)` | 主键 | 文案记录 ID。 |
| `user_id` | `INT` | 可空，外键 | 所属用户。用户删除或清空历史保留收藏时，该字段可置空。 |
| `scene` | `NVARCHAR(200)` | 非空 | 生成场景。 |
| `mood` | `NVARCHAR(50)` | 可空 | 心情。 |
| `style` | `NVARCHAR(50)` | 可空 | 文案风格。 |
| `keywords` | `NVARCHAR(500)` | 可空 | 关键词；用户标签作为隐藏上下文传给 AI，不写入该字段。 |
| `generated_content` | `NVARCHAR(MAX)` | 非空 | AI 生成内容。 |
| `ai_model` | `NVARCHAR(100)` | 可空 | 生成时使用的模型。 |
| `create_time` | `DATETIME` | 非空，默认当前时间 | 创建时间。 |

索引：

```text
idx_copywriting_records_user_time(user_id, create_time DESC)
```

外键：

```text
copywriting_records.user_id -> users.id
ON DELETE SET NULL
```

### copywriting_record_steps

生成和优化过程记录表，保存一条文案的初始生成和后续每次优化。

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| `id` | `INT IDENTITY(1,1)` | 主键 | 步骤 ID。 |
| `record_id` | `INT` | 非空，外键 | 所属文案记录。 |
| `step_no` | `INT` | 非空 | 步骤序号；同一文案内唯一。 |
| `user_message` | `NVARCHAR(500)` | 可空 | 生成要求或优化要求。 |
| `generated_content` | `NVARCHAR(MAX)` | 非空 | 该步骤生成后的文案内容。 |
| `create_time` | `DATETIME` | 非空，默认当前时间 | 步骤创建时间。 |

唯一约束：

```text
uq_copywriting_record_steps_no(record_id, step_no)
```

索引：

```text
idx_copywriting_record_steps_record_no(record_id, step_no ASC)
```

外键：

```text
copywriting_record_steps.record_id -> copywriting_records.id
ON DELETE CASCADE
```

### favorites

收藏表，保存用户收藏过的文案记录。

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| `id` | `INT IDENTITY(1,1)` | 主键 | 收藏记录 ID。 |
| `user_id` | `INT` | 非空，外键 | 收藏所属用户。 |
| `record_id` | `INT` | 非空，外键 | 被收藏的文案记录。 |
| `create_time` | `DATETIME` | 非空，默认当前时间 | 收藏时间。 |

唯一约束：

```text
uq_favorites_user_record(user_id, record_id)
```

索引：

```text
idx_favorites_user_time(user_id, create_time DESC)
```

外键：

```text
favorites.user_id -> users.id
ON DELETE CASCADE

favorites.record_id -> copywriting_records.id
ON DELETE CASCADE
```

## 数据保留规则

- 删除单条历史：删除该文案记录，同时通过外键级联删除对应优化过程；如果该记录已收藏，收藏关系也会随记录删除。
- 清空全部历史：清空当前用户历史列表和对应优化过程；未收藏的文案记录会被删除，已收藏的文案记录会保留内容并将 `copywriting_records.user_id` 置空，因此仍可在收藏列表查看。
- 取消收藏：删除收藏关系；如果这条文案已经因为清空历史而不再属于历史记录，会同步删除孤立的文案内容。
- 删除用户：用户标签和收藏关系会级联删除；文案记录的 `user_id` 会置空。
