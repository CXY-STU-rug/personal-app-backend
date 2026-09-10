# ==================== 第一阶段：构建 ====================
# 用带 JDK + Maven 的镜像跑一次打包。
# 这个阶段的所有东西——Maven 本体、下载的几百个依赖 jar、源码、class 中间产物——
# 全都不会进最终镜像，只是借它的环境把 jar 编译出来。
# AS builder 是给这个阶段起个名字，第二阶段靠这个名字来取产物。
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# 📌 关于 Maven 镜像源：这里【故意不配】阿里云源，直连 Maven 中央仓库。
#    原因是在目标服务器（阿里云香港节点）上实测过：
#      中央仓库 repo.maven.apache.org : 0.27 秒 / 5.9 MB/s
#      阿里云   maven.aliyun.com      : 2.22 秒 / 0.73 MB/s
#    香港走国际线路直连更快，用阿里云源反而要绕回国内，慢 8 倍。
#    ⚠️ 如果以后换成【国内】服务器（杭州、北京等），要反过来加上阿里云镜像源，
#       否则直连中央仓库会慢到经常超时。

# ⭐ 先单独 COPY pom.xml，再单独下依赖——这两步的顺序不能反。
# Docker 的分层缓存是按"这一层用到的文件有没有变"来判断的：
#   pom.xml 没动过 → 直接复用已经缓存好的依赖层 → 改业务代码时省掉几分钟下载
# 如果一上来就 COPY 整个项目，那改任何一行 Java 代码都会让所有依赖重下一遍。
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 依赖齐了才放源码。这一层怎么变都不影响上面那层的缓存
COPY src ./src

# -DskipTests：部署阶段不跑测试。测试应该在本地或 CI 跑完再部署，
#              在服务器上跑既拖慢部署，测试挂了还会直接中断整个构建
# -B：批处理模式，不打进度条，日志干净可读
# MAVEN_OPTS 限制 Maven 自己的堆上限：
#   小内存服务器（2G 及以下）上 Maven 默认堆可能吃到系统内存耗尽，
#   直接被内核 OOM Killer 杀掉，表现为构建莫名其妙中断、没有报错
RUN MAVEN_OPTS="-Xmx512m" mvn clean package -DskipTests -B


# ==================== 第二阶段：运行 ====================
# 只要 JRE，不要 JDK 更不要 Maven。
# 最终镜像从 ~800MB 降到 ~250MB，拉取、启动、备份都快得多。
# 而且源码和构建工具都不在镜像里，被人拿到镜像也翻不出你的代码。
FROM eclipse-temurin:21-jre

WORKDIR /app

# --from=builder 表示从上面那个构建阶段里取文件，只取产出的那一个 jar。
# 用通配符 *.jar 是因为文件名带版本号（liyuq-project-1.0-SNAPSHOT.jar），
# 以后在 pom 里改 version 不用回来改这行
COPY --from=builder /build/target/*.jar app.jar

# 声明容器内监听 8080——必须和 application.yml 里的 server.port 一致。
# 这行只起文档作用，真正的端口映射在 docker-compose.yml 的 ports 里（9003:8080）
EXPOSE 8080

# -XX:MaxRAMPercentage=75.0
#   让 JVM 按【容器的内存限额】的 75% 来定堆上限。
#   不写的话老版本 JVM 会按宿主机总内存算，在小内存机器上一跑就被 OOM Killer 干掉，
#   现象是容器莫名其妙重启、日志里什么都没有。
# -Djava.security.egd=file:/dev/./urandom
#   容器里 /dev/random 熵池经常不够，Tomcat 启动时生成 session ID 会卡住几十秒，
#   换成 urandom 不阻塞。这是容器里跑 Java 的老坑了。
ENTRYPOINT ["java", \
            "-XX:MaxRAMPercentage=75.0", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-jar", "/app/app.jar"]
