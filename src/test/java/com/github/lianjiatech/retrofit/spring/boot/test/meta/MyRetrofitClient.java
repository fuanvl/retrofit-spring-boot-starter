package com.github.lianjiatech.retrofit.spring.boot.test.meta;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

import com.github.lianjiatech.retrofit.spring.boot.core.RetrofitClient;
import com.github.lianjiatech.retrofit.spring.boot.log.LogLevel;
import com.github.lianjiatech.retrofit.spring.boot.log.LogStrategy;
import com.github.lianjiatech.retrofit.spring.boot.log.Logging;
import com.github.lianjiatech.retrofit.spring.boot.retry.Retry;

import retrofit2.Converter;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * @author 陈添明
 * @since 2022/5/7 6:51 下午
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Inherited
@RetrofitClient(baseUrl = "${test.baseUrl}")
@Logging(logLevel = LogLevel.WARN)
@Retry(intervalMs = 200)
public @interface MyRetrofitClient {

    @AliasFor(annotation = RetrofitClient.class, attribute = "converterFactories")
    Class<? extends Converter.Factory>[] converterFactories() default {GsonConverterFactory.class};

    @AliasFor(annotation = Logging.class, attribute = "logStrategy")
    LogStrategy logStrategy() default LogStrategy.BODY;
}
