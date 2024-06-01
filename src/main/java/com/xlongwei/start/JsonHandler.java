package com.xlongwei.start;

import java.io.File;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jsoniter.JsonIterator;
import com.jsoniter.any.Any;
import com.networknt.codegen.FrameworkRegistry;
import com.networknt.codegen.Generator;
import com.networknt.codegen.Utils;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.utility.HashUtil;
import com.networknt.utility.NioUtils;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

public class JsonHandler implements HttpHandler {
	static final Logger logger = LoggerFactory.getLogger(JsonHandler.class);
	private final ObjectMapper objectMapper;

	public JsonHandler(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public void handleRequest(HttpServerExchange exchange) throws Exception {
		@SuppressWarnings("unchecked")
		Map<String, String> map = (Map<String, String>) exchange.getAttachment(AttachmentConstants.REQUEST_BODY);
		logger.info("{}", map);

		String framework = (String) map.get("framework");
		String config = (String) map.get("config");
		String model = (String) map.get("model");
		String message = "unknown";

		FrameworkRegistry registry = FrameworkRegistry.getInstance();
		Set<String> frameworks = registry.getFrameworks();
		if (!frameworks.contains(framework)) {
			message = "framework不支持，合法值为：" + StringUtils.join(frameworks, ',');
		} else {
			// 这里有三种实现方式：1，参考codegen-cli的Cli、codegen-web的CodegenSingleHandler；2，执行shell调用codegen-cli包；3，执行rest调用codegen-web服务
			// 这里选择方式1，方式2和3仅输出命令即可，需要的jar包可以执行sh cli-web.sh jars获得
			String cliJar = "target/codegen-cli-1.6.47.jar";
			String output = HashUtil.generateUUID();
			String zipFile = "target/" + output + ".zip";

			// 输出方式2的命令，方式3参考cli-web.sh：java -jar target/codegen-web-1.6.47.jar
			if (Utils.isUrl(model) && Utils.isUrl(config)) {
				logger.info("\njava -jar {} -f {} -c {} -m {} -o {}", cliJar, framework, config, model, output);
			}

			try {
				Generator generator = registry.getGenerator(framework);
				Object anyModel = null;
				// model can be empty in some cases.
				if (StringUtils.isNotBlank(model)) {
					// check if model is json or not before loading.
					if (Utils.isUrl(model)) {
						if (model.endsWith("json")) {
							anyModel = JsonIterator.deserialize(Utils.urlToByteArray(new URL(model)));
						} else {
							anyModel = new String(Utils.urlToByteArray(new URL(model)), StandardCharsets.UTF_8);
						}
					} else {
						model = org.apache.commons.codec.binary.StringUtils.newStringUtf8(Base64.decodeBase64(model));
						if (model.startsWith("{") || model.startsWith("[")) {
							anyModel = JsonIterator.deserialize(model);
						} else {
							anyModel = model;
						}
					}
				}

				Any anyConfig = null;
				if (StringUtils.isNotBlank(config)) {
					if (Utils.isUrl(config)) {
						anyConfig = JsonIterator.deserialize(Utils.urlToByteArray(new URL(config)));
					} else {
						anyConfig = JsonIterator.deserialize(config);
					}
				}

				generator.generate(output, anyModel, anyConfig);
				File file = new File(output);
				if (!file.exists()) {
					message = "生成失败：" + output + "，请检查model配置";
					logger.warn(message);
				} else {
					logger.info(zipFile);

					NioUtils.create(zipFile, output);
					// 返回下载链接，不用codegen-web直接响应字节
//					exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/zip")
//							.add(new HttpString("Content-Disposition"), "attachment; filename=" + file.getName());
//					exchange.getResponseSender().send(NioUtils.toByteBuffer(file));
					message = zipFile;
				}
			} catch (Exception e) {
				message = e.getMessage();
				logger.warn(message);
			}
			// delete the project folder. 取消.peek(System.out::println)
			Files.walk(Paths.get(output), FileVisitOption.FOLLOW_LINKS).sorted(Comparator.reverseOrder())
					.map(Path::toFile).forEach(File::delete);
		}

		if (exchange.isComplete())
			return;
		exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
		exchange.getResponseSender()
				.send(ByteBuffer.wrap(objectMapper.writeValueAsBytes(Collections.singletonMap("message", message))));
	}
}
