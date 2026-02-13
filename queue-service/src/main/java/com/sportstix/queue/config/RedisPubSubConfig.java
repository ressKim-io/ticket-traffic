package com.sportstix.queue.config;

import com.sportstix.queue.websocket.RedisQueueMessageSubscriber;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
public class RedisPubSubConfig {

    public static final String QUEUE_UPDATE_CHANNEL = "ws:broadcast:queue-update";

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter queueMessageListenerAdapter
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(queueMessageListenerAdapter, new PatternTopic(QUEUE_UPDATE_CHANNEL));
        return container;
    }

    @Bean
    public MessageListenerAdapter queueMessageListenerAdapter(RedisQueueMessageSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "onMessage");
    }
}
