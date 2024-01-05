package com.sky.config;

import com.sky.properties.AliOssProperties;
import com.sky.utils.AliOssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 配置类，用于创建AliOssUtil对象。把AliOssUtil对象交给IOC管理
 */
@Configuration
@Slf4j
public class OssConfiguration {

    @Bean   // 声明第三方bean，方法的返回值是想要交给IOC管理的对象所属的类
    @ConditionalOnMissingBean   // 没有这个bean时才去创建
    public AliOssUtil aliOssUtil(AliOssProperties aliOssProperties) {
        log.info("开始上传阿里云文件上传工具类对象: {}", aliOssProperties);
        return new AliOssUtil(aliOssProperties.getEndpoint(),
                aliOssProperties.getAccessKeyId(),
                aliOssProperties.getAccessKeySecret(),
                aliOssProperties.getBucketName());
    }
}
