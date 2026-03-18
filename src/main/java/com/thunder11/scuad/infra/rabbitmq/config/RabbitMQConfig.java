package com.thunder11.scuad.infra.rabbitmq.config;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    public static final String EXCHANGE_NAME = "scuad.ai.exchange";
    public static final String QUEUE_EVALUATION = "scuad.ai.queue.evaluation";
    public static final String QUEUE_RESUME = "scuad.ai.queue.resume";
    public static final String QUEUE_PORTFOLIO = "scuad.ai.queue.portfolio";
    public static final String QUEUE_COMPARISON = "scuad.ai.queue.comparison";
    public static final String QUEUE_JOBPOSTING = "scuad.ai.request.jobposting.queue";

    @Bean
    public DirectExchange aiExchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue evaluationQueue() {
        return QueueBuilder.durable(QUEUE_EVALUATION)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", QUEUE_EVALUATION + ".dead_letter")
                .build();
    }
    @Bean
    public Queue resumeQueue() {
        return QueueBuilder.durable(QUEUE_RESUME)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", QUEUE_RESUME + ".dead_letter")
                .build();
    }
    @Bean
    public Queue portfolioQueue() {
        return QueueBuilder.durable(QUEUE_PORTFOLIO)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", QUEUE_PORTFOLIO + ".dead_letter")
                .build();
    }
    @Bean
    public Queue comparisonQueue() {
        return QueueBuilder.durable(QUEUE_COMPARISON)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", QUEUE_COMPARISON + ".dead_letter")
                .build();
    }
    @Bean
    public Queue jobpostingQueue() {
        return QueueBuilder.durable(QUEUE_JOBPOSTING)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", QUEUE_JOBPOSTING + ".dead_letter")
                .build();
    }


    @Bean
    public Binding bindEvaluation(Queue evaluationQueue, DirectExchange aiExchange) {
        return BindingBuilder.bind(evaluationQueue).to(aiExchange).with(QUEUE_EVALUATION);
    }

    @Bean
    public Binding bindResume(Queue resumeQueue, DirectExchange aiExchange) {
        return BindingBuilder.bind(resumeQueue).to(aiExchange).with(QUEUE_RESUME);
    }

    @Bean
    public Binding bindPortfolio(Queue portfolioQueue, DirectExchange aiExchange) {
        return BindingBuilder.bind(portfolioQueue).to(aiExchange).with(QUEUE_PORTFOLIO);
    }

    @Bean
    public Binding bindComparison(Queue comparisonQueue, DirectExchange aiExchange) {
        return BindingBuilder.bind(comparisonQueue).to(aiExchange).with(QUEUE_COMPARISON);
    }

    @Bean
    public Binding bindJobPosting(Queue jobpostingQueue, DirectExchange aiExchange) {
        return BindingBuilder.bind(jobpostingQueue).to(aiExchange).with(QUEUE_JOBPOSTING);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
