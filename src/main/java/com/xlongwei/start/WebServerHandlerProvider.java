package com.xlongwei.start;

import static io.undertow.Handlers.path;
import static io.undertow.Handlers.resource;

import com.networknt.config.Config;
import com.networknt.handler.HandlerProvider;

import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.resource.ClassPathResourceManager;

public class WebServerHandlerProvider implements HandlerProvider {

	public HttpHandler getHandler() {
		return path().addPrefixPath("/api/json", new JsonHandler(Config.getInstance().getMapper())).addPrefixPath("/",
				resource(new ClassPathResourceManager(WebServerHandlerProvider.class.getClassLoader(), "public")));
	}

}
