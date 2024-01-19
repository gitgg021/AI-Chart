package com.caocao.springbootinit.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class MqInitMain {
    public static void main(String[] args) {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();
            String EXCHANGE_NAME = MqConstant.EXCHANGE_NAME;
            channel.exchangeDeclare(EXCHANGE_NAME, "direct");
            // 创建队列
            String queueName = MqConstant.QUEUE_NAME;
            /**
             * durable：指示队列是否持久化。如果设置为true，则RabbitMQ会将队列保存到磁盘上，以便在服务器重启后恢复。如果设置为false，则消息仅存在于内存中，服务器重启时会丢失。默认为false。
             * exclusive：指示队列是否为专有队列。如果设置为true，则只有声明队列的连接可以使用该队列。一旦连接关闭，队列就会自动删除。默认为false。
             * autoDelete：指示队列是否为自动删除队列。如果设置为true，则队列在消费者断开连接时会自动删除。默认为false。
             * arguments：用于设置一些额外的参数。它是一个Map类型的参数，可以根据需要传递一些额外的参数。例如，可以设置队列的最大长度、超时时间等。
             */
            channel.queueDeclare(queueName, true, false, false, null);
            channel.queueBind(queueName, EXCHANGE_NAME, MqConstant.ROUTING_KEY);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
}