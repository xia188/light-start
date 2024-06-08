package com.xlongwei.start;

import java.io.File;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteStreamHandler;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.LogOutputStream;
import org.apache.commons.exec.OS;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.FileUtils;
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
import io.undertow.server.handlers.form.FormData.FileItem;
import io.undertow.util.Headers;

public class JsonHandler implements HttpHandler {
	static final Logger logger = LoggerFactory.getLogger(JsonHandler.class);
	private final ObjectMapper objectMapper;
	private static final URLCodec base64 = new URLCodec();
	private static long timeout = 120000;
	private static String cliJar1 = StringUtils.defaultIfBlank(System.getProperty("codegen.jar1"), "files/codegen-cli-1.6.47.jar");
	private static String cliJar2 = StringUtils.defaultIfBlank(System.getProperty("codegen.jar2"), "files/codegen-cli-2.1.32.jar");

	public JsonHandler(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public void handleRequest(HttpServerExchange exchange) throws Exception {
		@SuppressWarnings("unchecked")
		Map<String, Object> map = (Map<String, Object>) exchange.getAttachment(AttachmentConstants.REQUEST_BODY);

		String framework = (String) map.get("framework");
		String config = getParam((String) map.get("config"),map.get("configFile"));
		String model = getParam((String) map.get("model"),map.get("modelFile"));
		String release = StringUtils.trimToEmpty((String) map.get("release"));
		String message = "unknown";

		// 三种方式：1，参考codegen-cli的Cli、codegen-web的CodegenSingleHandler；2，执行shell调用codegen-cli包；3，执行rest调用codegen-web服务
		String cliJar = release.startsWith("2") ? cliJar2 : cliJar1;
		String output = HashUtil.generateUUID();
		String zip = "files/" + output + ".zip";
		File configFile = new File(output + ".config.json"), modelFile = new File(output + ".yaml");
		File outputFile = new File(output), zipFile = new File(zip);

		try {
			if (StringUtils.isNotBlank(config) && !config.startsWith("http")) {
				if (!isJson(config)) {
					config = base64.decode(config);
					if (!isJson(config)) {
						configFile = new File(output + ".config.yaml");
					}
				}
				FileUtils.writeStringToFile(configFile, config, StandardCharsets.UTF_8);
			}
			if (StringUtils.isNotBlank(model) && !model.startsWith("http")) {
				if (isJson(model)) {
					// json格式可以不用base64转码
					modelFile = new File(output + ".json");
				} else {
					model = base64.decode(model);
					if (isJson(model)) {
						modelFile = new File(output + ".json");
					} else if (model.startsWith("schema")) {
						modelFile = new File(output + ".grqphqls");
					} else {
						modelFile = new File(output + ".yaml");
					}
				}
				FileUtils.writeStringToFile(modelFile, model, StandardCharsets.UTF_8);
			}
			if(release.startsWith("2")) {
				//处理非网址的情形
				if(configFile.exists()) config = configFile.getName();
				if(modelFile.exists()) model = modelFile.getName();
				//生成项目，打包，删除目录
				if (StringUtils.isNoneBlank(framework, config)) {
					String shell = String.format("java -jar %s -f %s -c %s -m %s -o %s", cliJar, framework, config, model, output);
					new ShellTask(shell, timeout).execute();
					new ShellTask("jar cf " + zip + " " + output, timeout).execute();
					System.out.println(output + "=" + outputFile.exists() + " " + zip + "=" + zipFile.exists());
					message = zipFile.exists() ? zip : "生成失败";
				} else if (modelFile.exists()) {
					modelFile.renameTo(new File("files/" + modelFile.getName()));
					message = "?spec=/files/" + modelFile.getName();
				}
			}else {
				Object anyModel = null;
				if (StringUtils.isNotBlank(model)) {
					if (Utils.isUrl(model)) {
						if (model.endsWith("json")) {
							anyModel = JsonIterator.deserialize(Utils.urlToByteArray(new URL(model)));
						} else {
							anyModel = new String(Utils.urlToByteArray(new URL(model)), StandardCharsets.UTF_8);
						}
					} else {
						if (isJson(model)) {
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
					Generator generator = FrameworkRegistry.getInstance().getGenerator(framework);
					generator.generate(output, anyModel, anyConfig);
					File file = new File(output);
					if (!file.exists()) {
						message = "生成失败：" + output + "，请检查model配置";
						logger.warn(message);
					} else {
						logger.info(zip);
						NioUtils.create(zip, output);
						message = zip;
					}
				}else if(modelFile.exists()) {
					modelFile.renameTo(new File("files/" + modelFile.getName()));
					message = "?spec=/files/" + modelFile.getName();
				}
			}
		} catch (Exception e) {
			message = e.getMessage();
			logger.warn(message);
		}
//		FileUtils.deleteQuietly(outputFile);
		FileUtils.deleteQuietly(configFile);
		FileUtils.deleteQuietly(modelFile);
		exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
		exchange.getResponseSender()
				.send(ByteBuffer.wrap(objectMapper.writeValueAsBytes(Collections.singletonMap("message", message))));
	}
	
	private String getParam(String str, Object file) throws Exception {
		if (file != null && file instanceof FileItem) {
			FileItem upload = (FileItem) file;
			str = NioUtils.toString(upload.getInputStream()).trim();
			str = isJson(str) ? str : base64.encode(str);
		}
		return str;
	}
	
	private boolean isJson(String config) {
		return config.startsWith("{") || config.startsWith("[");
	}
	
    public static class ShellTask {
        private String shell;
        private long timeout;
        private CommandLine command;
        private Charset gbk = Charset.forName("GBK");
        public List<String> outputs = null;

        public ShellTask(String shell, long timeout) {
            this.shell = shell;
            this.timeout = timeout;
            this.command = CommandLine.parse(shell);
        }

        public void execute() {
            try {
                System.out.println(shell);
                Executor exe = new DefaultExecutor();

                ExecuteWatchdog watchdog = new ExecuteWatchdog(timeout);
                exe.setWatchdog(watchdog);

                LogOutputStream outAndErr = new LogOutputStream() {
                    @Override
                    protected void processLine(String line, int logLevel) {
                        String output = new String(line.getBytes(),
                                OS.isFamilyWindows() ? gbk : StandardCharsets.UTF_8);
                        System.out.println(output);
                        if (outputs != null) {
                            outputs.add(output);
                        }
                    }
                };
                ExecuteStreamHandler streamHandler = new PumpStreamHandler(outAndErr);
                exe.setStreamHandler(streamHandler);

                long s = System.currentTimeMillis();
                int exitvalue = exe.execute(command);
                streamHandler.stop();
                outAndErr.close();
                if (exe.isFailure(exitvalue) && watchdog.killedProcess()) {
                    System.out.println("timeout and killed by watchdog");
                } else {
                    System.out.printf("exec succeeded millis= %s ms\n", (System.currentTimeMillis() - s));
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }

    }
}
