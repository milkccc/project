package com.xzzn.pollux.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DirectExchangeConfig {

    @Bean
    public Queue qaTaskB2MQueue() {
        return QueueBuilder.durable("qa.task.b2m.queue").build();
    }

    @Bean
    public Queue qaTaskM2BQueue() {
        return QueueBuilder.durable("qa.task.m2b.queue").build();
    }


    @Bean
    public Queue qaTaskbackend2modelQueue() {
        // 配置优先级队列
        return QueueBuilder.durable("qa.task.backend2model.queue")
                .withArgument("x-max-priority", 10) // 设置最大优先级
                .build();
    }

    @Bean
    public Queue qaTaskmodel2backendQueue() {
        // 配置优先级队列
        return QueueBuilder.durable("qa.task.model2backend.queue")
                .withArgument("x-max-priority", 10) // 设置最大优先级
                .build();
    }

    @Bean
    public Exchange polluxDirectExchange() {
        return ExchangeBuilder.directExchange("pollux.direct").build();
    }

    @Bean
    public Binding bindingQATaskB2MQueueToPolluxDirectExchange() {
        return BindingBuilder.bind(qaTaskB2MQueue()).to(polluxDirectExchange()).with("qa.task.b2m").noargs();
    }

    @Bean
    public Binding bindingQATaskM2BQueueToPolluxDirectExchange() {
        return BindingBuilder.bind(qaTaskM2BQueue()).to(polluxDirectExchange()).with("qa.task.m2b").noargs();
    }

    @Bean
    public Binding bindingQATaskbackend2modelQueueToPolluxDirectExchange() {
        return BindingBuilder.bind(qaTaskbackend2modelQueue()).to(polluxDirectExchange()).with("qa.task.backend2model").noargs();
    }

    @Bean
    public Binding bindingQATaskmodel2backendQueueToPolluxDirectExchange() {
        return BindingBuilder.bind(qaTaskmodel2backendQueue()).to(polluxDirectExchange()).with("qa.task.model2backend").noargs();
    }

    @Bean
    public Queue fileParseQueue() {
        return QueueBuilder.durable("file.parse.queue").build();
    }

    @Bean
    public Binding bindingFileParseQueueToPolluxDirectExchange() {
        return BindingBuilder.bind(fileParseQueue()).to(polluxDirectExchange()).with("file.parse").noargs();
    }

    @Bean
    public Queue fileParseWordQueue() {
        return QueueBuilder.durable("file.parse.word.queue").build();
    }

    @Bean
    public Binding bindingFileParseWordQueueToPolluxDirectExchange() {
        return BindingBuilder.bind(fileParseWordQueue()).to(polluxDirectExchange()).with("file.parse.word").noargs();
    }

    @Bean
    public Queue fileParseCsvQueue() {
        return QueueBuilder.durable("file.parse.csv.queue").build();
    }

    @Bean
    public Binding bindingFileParseCsvQueueToPolluxDirectExchange() {
        return BindingBuilder.bind(fileParseCsvQueue()).to(polluxDirectExchange()).with("file.parse.csv").noargs();
    }

    @Bean
    public Queue autogenTaskQueue() {
        return QueueBuilder.durable("crawler.file.txt.queue").build();
    }

    @Bean
    public Binding bindingAutogenTaskQueueToPolluxDirectExchange() {
        return BindingBuilder.bind(autogenTaskQueue()).to(polluxDirectExchange()).with("crawler.file.txt").noargs();
    }

}
