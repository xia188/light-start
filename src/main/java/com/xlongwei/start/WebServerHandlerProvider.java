package com.xlongwei.start;

import com.networknt.config.Config;
import com.networknt.handler.HandlerProvider;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.builder.PredicatedHandlersParser;
import io.undertow.server.handlers.resource.ClassPathResourceManager;

import java.io.File;

import static io.undertow.Handlers.resource;


public class WebServerHandlerProvider implements HandlerProvider {

    public HttpHandler getHandler() {

        return Handlers.predicates(
                PredicatedHandlersParser.parse("not path-prefix('/images', '/assets', '/api') -> rewrite('/index.html')"
                        , WebServerHandlerProvider.class.getClassLoader()),
                new PathHandler(resource(new ClassPathResourceManager(WebServerHandlerProvider.class.getClassLoader(), "public")))
                        .addPrefixPath("/api/json", new JsonHandler(Config.getInstance().getMapper()))
                        .addPrefixPath("/api/text", new TextHandler())
        );
    }

}
