package com.caocao.springbootinit.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class MqInitMain {
    public static void main(String[] args) {
        try {
            //创建连接工厂
            ConnectionFactory factory = new ConnectionFactory();
            // 设置RabbitMQ地址
            factory.setHost(BiMqConstant.BI_MQ_HOST);
            factory.setUsername(BiMqConstant.BI_MQ_USERNAME);
            factory.setPassword(BiMqConstant.BI_MQ_PASSWORD);

            // 创建一个新的连接
            Connection connection = factory.newConnection();
            // 创建一个通道
            Channel channel = connection.createChannel();
            String EXCHANGE_NAME = MqConstant.EXCHANGE_NAME;
            //声明交换机,指定交换机类型为direct
            channel.exchangeDeclare(EXCHANGE_NAME, "direct");
            // 创建队列
            String queueName = MqConstant.QUEUE_NAME;
            // 声明队列，设置队列持久化、非独占、非自动删除，并传入额外的参数为 null
            channel.queueDeclare(queueName, true, false, false, null);
            // 将队列绑定到交换机，指定路由键为 "my_routingKey"
            channel.queueBind(queueName, EXCHANGE_NAME, MqConstant.ROUTING_KEY);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
}