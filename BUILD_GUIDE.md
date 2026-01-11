# AutoRewriter - 构建说明

## 问题修复总结

### 修复的问题

1. **Parent POM 配置错误**
   - 修正了 groupId：`com.autorewriter` → `org.autorewriter`
   - 修正了版本：`${reversion}` → `1.0-SNAPSHOT`
   - 添加了正确的 `relativePath`

2. **清理了无关依赖**
   - 移除了所有 `com.kuaishou.adaptiveengine` 相关依赖
   - 移除了 Hadoop 相关依赖
   - 只保留必要的基础依赖

3. **依赖管理优化**
   - 在父 POM 的 `dependencyManagement` 中统一管理依赖版本
   - 子模块只需声明依赖，无需指定版本

## 构建步骤

### 第一步：安装本地 JAR 文件

由于项目使用了自定义版本的 Calcite Core (1.38.0-U6)，需要先将本地 JAR 安装到 Maven 本地仓库：

```bash
# 给脚本添加执行权限
chmod +x install-local-jars.sh

# 运行安装脚本
./install-local-jars.sh
```

或者手动运行以下命令：

```bash
# 安装 Calcite Core
mvn install:install-file \
  -Dfile=libs/calcite-core-1.38.0-U6.jar \
  -DgroupId=org.apache.calcite \
  -DartifactId=calcite-core \
  -Dversion=1.38.0-U6 \
  -Dpackaging=jar

# 安装 ShardingSphere SQL Federation Core
mvn install:install-file \
  -Dfile=libs/shardingsphere-sql-federation-core-5.5.4-SNAPSHOT-shade.jar \
  -DgroupId=org.apache.shardingsphere \
  -DartifactId=shardingsphere-sql-federation-core \
  -Dversion=5.5.4-SNAPSHOT \
  -Dpackaging=jar
```

### 第二步：构建项目

```bash
# 清理并编译
mvn clean compile

# 运行测试
mvn test

# 安装到本地仓库（跳过测试）
mvn install -DskipTests

# 完整构建
mvn clean install
```

## 项目结构

```
AutoRewriter/
├── pom.xml                          # 父 POM
├── libs/                            # 本地 JAR 文件
│   ├── calcite-core-1.38.0-U6.jar
│   └── shardingsphere-sql-federation-core-5.5.4-SNAPSHOT-shade.jar
├── sql-core/                        # SQL 核心模块
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   └── java/
│       │       └── com/
│       │           └── autorewriter/
│       │               └── sql/     # 包：com.autorewriter.sql
│       └── test/
│           └── java/
│               └── com/
│                   └── autorewriter/
│                       └── sql/
└── install-local-jars.sh            # JAR 安装脚本
```

## 依赖说明

### 已配置的依赖

- **Calcite Core 1.38.0-U6**: SQL 解析和优化引擎
- **ShardingSphere SQL Federation Core 5.5.4-SNAPSHOT**: 分布式 SQL 执行引擎
- **Lombok 1.18.30**: 减少样板代码
- **SLF4J 2.0.9**: 日志接口
- **Logback 1.4.14**: 日志实现
- **JUnit 5.10.1**: 单元测试框架
- **Mockito 5.8.0**: Mock 测试框架

## IDE 配置

### IntelliJ IDEA

1. 运行 `install-local-jars.sh` 安装本地 JAR
2. 在 IDEA 中打开项目：`File` → `Open` → 选择 `AutoRewriter` 目录
3. IDEA 会自动识别为 Maven 项目
4. 等待 Maven 依赖下载完成
5. 刷新 Maven 项目：右键点击项目 → `Maven` → `Reload Project`

现在你应该可以在 `sql-core/src/main/java` 下创建 `com.autorewriter.sql` 包了。

## 故障排除

### JDK 版本问题

如果看到 "错误: 无效的目标发行版" 错误：

```bash
# 检查当前 JDK 版本
java -version
javac -version

# 确保使用 JDK 11 或更高版本
# 如果版本不对，需要配置 JAVA_HOME 环境变量
export JAVA_HOME=/path/to/jdk-11
export PATH=$JAVA_HOME/bin:$PATH
```

项目配置为使用 **JDK 11**。如果你的系统 JDK 版本低于 11，需要升级或配置正确的 JAVA_HOME。

### 无法创建包

如果在 IDE 中无法创建 `com.autorewriter.sql` 包：

1. 确保已运行 `install-local-jars.sh`
2. 在 IDEA 中刷新 Maven 项目
3. 执行 `File` → `Invalidate Caches / Restart`
4. 重新打开项目

### Maven 构建失败

如果看到 "Non-resolvable parent POM" 错误：

1. 检查 `sql-core/pom.xml` 的 parent 配置是否正确
2. 确保 groupId 是 `org.autorewriter`（不是 `com.autorewriter`）
3. 确保 version 是 `1.0-SNAPSHOT`（不是 `${reversion}`）

### 依赖解析失败

如果看到 "Could not resolve dependencies" 错误：

1. 确保已运行 `install-local-jars.sh`
2. 检查 `~/.m2/repository` 中是否存在已安装的 JAR
3. 清理本地仓库缓存：`mvn dependency:purge-local-repository`

