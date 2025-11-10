package com.rd.example.provider;

import com.rd.example.common.service.UserService;
import com.rd.rpc.registry.LocalRegistry;
import com.rd.rpc.server.HttpServer;
import com.rd.rpc.server.VertxHttpServer;

/**
 * 简易服务提供者示例
 */
public class EasyProviderExample {

    public static void main(String[] args) {
        // 注册服务
        LocalRegistry.register(UserService.class.getName(), UserServiceImpl.class);

        // 启动 web 服务
        HttpServer httpServer = new VertxHttpServer();
        httpServer.doStart(8080);
    }
}
