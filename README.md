# light-start

#### 项目介绍

在线生成基于light-4j架构的项目，包括rest、hybrid、graphql三种框架。

##### 快速上手

1. 本地编译：mvn compile，运行主类：com.networknt.server.Server
2. 访问主页：http://localhost:8080/
3. 打包部署：mvn package，执行jar包：java -jar target/light-start-0.1.jar

#### 对比light-codegen

1. codegen-cli是命令行工具，codegen-web用了hybrid框架且需编译react前端页面，light-start是rest框架更简单独立一点。
2. mvn:archetype也可以生成项目，但light-codegen更灵活一些，[config.json](https://doc.networknt.com/tool/light-codegen/openapi-generator/#config)和[openapi.yaml](https://github.com/networknt/model-config)自定义更丰富。
3. light-start最初也是通过骨架api-simple-web-archetype生成，加上swagger-ui后基本能实现功能因此没有再写ajax请求页面。
4. swagger-ui请求handler通过shell调用codegen-cli命令行工具是个不错的想法，使用solon-api或hutool.http会更轻量级，考虑加到[deploy](https://gitee.com/xlongwei/deploy)上。
5. 上面第4点已经实现，并且功能还得到了增强：支持配置文件上传，支持2.x版本，[在线生成](http://115.28.229.158:9881/specui.html)。
6. 参考上面，支持配置文件上传，支持2.x版本（使用微调过的 [codegen-cli-2.1.32.jar](https://github.com/xia188/light-codegen/tree/release.jdk8) 从而支持jdk8）

![light-start](/light-start.PNG "light-start")
