package com.xlongwei.start;

import static io.undertow.Handlers.path;
import static io.undertow.Handlers.resource;

import java.io.File;

import com.networknt.config.Config;
import com.networknt.handler.HandlerProvider;

import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.server.handlers.resource.FileResourceManager;

public class WebServerHandlerProvider implements HandlerProvider {

	public HttpHandler getHandler() {
		return path().addPrefixPath("/codegen", new JsonHandler(Config.getInstance().getMapper())).addPrefixPath("/",
				resource(new ClassPathResourceManager(WebServerHandlerProvider.class.getClassLoader(), "public")))
				.addPrefixPath("/files",
						resource(new FileResourceManager(new File("files"))).setDirectoryListingEnabled(true));
	}

}
