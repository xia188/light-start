package com.xlongwei.start;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.httpstring.AttachmentConstants;

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
		logger.info("\njava -jar target/codegen-cli-1.6.47.jar -f {} -c {} -m {} -o {}", map.get("framework"),
				map.get("config"), map.get("model"), "target/" + System.currentTimeMillis());
		exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
		exchange.getResponseSender().send(
				ByteBuffer.wrap(objectMapper.writeValueAsBytes(Collections.singletonMap("message", "Hello World"))));
	}
}
