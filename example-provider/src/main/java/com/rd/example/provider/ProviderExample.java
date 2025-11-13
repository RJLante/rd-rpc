package com.rd.example.provider;

import com.rd.example.common.service.UserService;
import com.rd.rpc.RpcApplication;
import com.rd.rpc.config.RegistryConfig;
import com.rd.rpc.config.RpcConfig;
import com.rd.rpc.model.ServiceMetaInfo;
import com.rd.rpc.registry.LocalRegistry;
import com.rd.rpc.registry.Registry;
import com.rd.rpc.registry.RegistryFactory;
import com.rd.rpc.server.HttpServer;
import com.rd.rpc.server.VertxHttpServer;
import com.rd.rpc.server.tcp.VertxTcpClient;
import com.rd.rpc.server.tcp.VertxTcpServer;

/**
 * 服务提供者示例
 */
public class ProviderExample {

    public static void main(String[] args) {
        // RPC 框架初始化
        RpcApplication.init();

        // 注册服务
        String serviceName = UserService.class.getName();
        LocalRegistry.register(serviceName, UserServiceImpl.class);

        // 注册服务到注册中心
        RpcConfig rpcConfig = RpcApplication.getRpcConfig();
        RegistryConfig registryConfig = rpcConfig.getRegistryConfig();
        Registry registry = RegistryFactory.getInstance(registryConfig.getRegistry());
        ServiceMetaInfo serviceMetaInfo = new ServiceMetaInfo();
        serviceMetaInfo.setServiceName(serviceName);
        serviceMetaInfo.setServiceHost(rpcConfig.getServerHost());
        serviceMetaInfo.setServicePort(rpcConfig.getServerPort());
        try {
            registry.register(serviceMetaInfo);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // 启动 web 服务
        VertxTcpServer vertxTcpServer = new VertxTcpServer();
        vertxTcpServer.doStart(8080);
    }
}
