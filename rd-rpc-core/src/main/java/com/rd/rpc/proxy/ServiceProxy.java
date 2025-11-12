package com.rd.rpc.proxy;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.rd.rpc.RpcApplication;
import com.rd.rpc.config.RpcConfig;
import com.rd.rpc.constant.RpcConstant;
import com.rd.rpc.model.RpcRequest;
import com.rd.rpc.model.RpcResponse;
import com.rd.rpc.model.ServiceMetaInfo;
import com.rd.rpc.registry.Registry;
import com.rd.rpc.registry.RegistryFactory;
import com.rd.rpc.serializer.Serializer;
import com.rd.rpc.serializer.SerializerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;

/**
 * 服务代理（JDK 动态代理）
 */
public class ServiceProxy implements InvocationHandler {

    /**
     * 调用代理
     *
     * @return
     * @throws Throwable
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 指定序列化器
        final Serializer serializer = SerializerFactory.getInstance(RpcApplication.getRpcConfig().getSerializer());

        // 构造请求
        String serviceName = method.getDeclaringClass().getName();
        RpcRequest rpcRequest = RpcRequest.builder()
                .serviceName(serviceName)
                .methodName(method.getName())
                .parameterTypes(method.getParameterTypes())
                .args(args)
                .build();
        try {
            // 序列化
            byte[] bodyBytes = serializer.serialize(rpcRequest);

            // 从注册中心获取服务提供者请求地址
            RpcConfig rpcConfig = RpcApplication.getRpcConfig();
            Registry registry = RegistryFactory.getInstance(rpcConfig.getRegistryConfig().getRegistry());
            ServiceMetaInfo serviceMetaInfo = new ServiceMetaInfo();
            serviceMetaInfo.setServiceName(serviceName);
            serviceMetaInfo.setServiceVersion(RpcConstant.DEFAULT_SERVICE_VERSION);
            List<ServiceMetaInfo> serviceMetaInfoList = registry.serviceDiscovery(serviceMetaInfo.getServiceKey());
            if (CollUtil.isEmpty(serviceMetaInfoList)) {
                throw new RuntimeException("暂无服务地址");
            }
            ServiceMetaInfo selectedServiceMetaInfo = serviceMetaInfoList.get(0);

            // 获取Content-Type
            String contentType = getContentType(rpcConfig.getSerializer());
            
            // 发送请求
            try (HttpResponse httpResponse = HttpRequest.post(selectedServiceMetaInfo.getServiceAddress())
                    .header("Content-Type", contentType)
                    .body(bodyBytes)
                    .execute()) {
                
                // 检查HTTP状态码
                int statusCode = httpResponse.getStatus();
                if (statusCode != 200) {
                    String errorBody = httpResponse.body();
                    throw new RuntimeException(String.format("RPC调用失败，HTTP状态码: %d, 响应: %s", statusCode, errorBody));
                }
                
                byte[] result = httpResponse.bodyBytes();
                if (result == null || result.length == 0) {
                    throw new RuntimeException("RPC调用失败，响应体为空");
                }
                
                // 反序列化
                RpcResponse rpcResponse = serializer.deserialize(result, RpcResponse.class);
                if (rpcResponse == null) {
                    throw new RuntimeException("RPC调用失败，反序列化响应为null");
                }
                
                // 检查响应是否有异常
                if (rpcResponse.getException() != null) {
                    throw new RuntimeException("RPC服务端异常: " + rpcResponse.getMessage(), rpcResponse.getException());
                }
                
                return rpcResponse.getData();
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("RPC调用IO异常", e);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * 根据序列化器类型获取Content-Type
     *
     * @param serializerKey 序列化器键名
     * @return Content-Type
     */
    private String getContentType(String serializerKey) {
        if ("json".equals(serializerKey)) {
            return "application/json";
        } else if ("hessian".equals(serializerKey)) {
            return "application/x-hessian";
        } else {
            // JDK、Kryo等其他序列化器使用二进制格式
            return "application/octet-stream";
        }
    }
}
