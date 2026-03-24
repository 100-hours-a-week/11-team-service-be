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
                .withArgument("x-message-ttl", 300000)
                .build();
    }
    @Bean
    public Queue evaluationDeadLetterQueue() {
        return QueueBuilder.durable(QUEUE_EVALUATION + ".dead_letter")
                .withArgument("x-message-ttl", 300000)
                .build();
    }
    @Bean
    public Queue evaluationDelayQueue() {
        return QueueBuilder.durable(QUEUE_EVALUATION + ".delay")
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", QUEUE_EVALUATION)
                .withArgument("x-message-ttl", 300000)
                .build();
    }

    @Bean
    public Queue resumeQueue() {
        return QueueBuilder.durable(QUEUE_RESUME)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", QUEUE_RESUME + ".dead_letter")
                .withArgument("x-message-ttl", 300000)
                .build();
    }
    @Bean
    public Queue resumeDeadLetterQueue() {
        return QueueBuilder.durable(QUEUE_RESUME + ".dead_letter")
                .withArgument("x-message-ttl", 300000)
                .build();
    }
    @Bean
    public Queue resumeDelayQueue() {
        return QueueBuilder.durable(QUEUE_RESUME + ".delay")
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", QUEUE_RESUME)
                .withArgument("x-message-ttl", 300000)
                .build();
    }

    @Bean
    public Queue portfolioQueue() {
        return QueueBuilder.durable(QUEUE_PORTFOLIO)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", QUEUE_PORTFOLIO + ".dead_letter")
                .withArgument("x-message-ttl", 300000)
                .build();
    }
    @Bean
    public Queue portfolioDeadLetterQueue() {
        return QueueBuilder.durable(QUEUE_PORTFOLIO + ".dead_letter")
                .withArgument("x-message-ttl", 300000)
                .build();
    }
    @Bean
    public Queue portfolioDelayQueue() {
        return QueueBuilder.durable(QUEUE_PORTFOLIO + ".delay")
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", QUEUE_PORTFOLIO)
                .withArgument("x-message-ttl", 300000)
                .build();
    }

    @Bean
    public Queue comparisonQueue() {
        return QueueBuilder.durable(QUEUE_COMPARISON)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", QUEUE_COMPARISON + ".dead_letter")
                .withArgument("x-message-ttl", 300000)
                .build();
    }
    @Bean
    public Queue comparisonDeadLetterQueue() {
        return QueueBuilder.durable(QUEUE_COMPARISON + ".dead_letter")
                .withArgument("x-message-ttl", 300000)
                .build();
    }
    @Bean
    public Queue comparisonDelayQueue() {
        return QueueBuilder.durable(QUEUE_COMPARISON + ".delay")
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", QUEUE_COMPARISON)
                .withArgument("x-message-ttl", 30000)
                .build();
    }

    @Bean
    public Queue jobpostingQueue() {
        return QueueBuilder.durable(QUEUE_JOBPOSTING)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", QUEUE_JOBPOSTING + ".dead_letter")
                .withArgument("x-message-ttl", 30000)
                .build();
    }
    @Bean
    public Queue jobpostingDeadLetterQueue() {
        return QueueBuilder.durable(QUEUE_JOBPOSTING + ".dead_letter")
                .withArgument("x-message-ttl", 30000)
                .build();
    }
    @Bean
    public Queue jobpostingDelayQueue() {
        return QueueBuilder.durable(QUEUE_JOBPOSTING + ".delay")
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", QUEUE_JOBPOSTING)
                .withArgument("x-message-ttl", 30000)
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
    public Binding bindEvaluationDelay(Queue evaluationDelayQueue, DirectExchange aiExchange) {
        return BindingBuilder.bind(evaluationDelayQueue).to(aiExchange).with(QUEUE_EVALUATION + ".delay");
    }

    @Bean
    public Binding bindResumeDelay(Queue resumeDelayQueue, DirectExchange aiExchange) {
        return BindingBuilder.bind(resumeDelayQueue).to(aiExchange).with(QUEUE_RESUME + ".delay");
    }

    @Bean
    public Binding bindPortfolioDelay(Queue portfolioDelayQueue, DirectExchange aiExchange) {
        return BindingBuilder.bind(portfolioDelayQueue).to(aiExchange).with(QUEUE_PORTFOLIO + ".delay");
    }

    @Bean
    public Binding bindComparisonDelay(Queue comparisonDelayQueue, DirectExchange aiExchange) {
        return BindingBuilder.bind(comparisonDelayQueue).to(aiExchange).with(QUEUE_COMPARISON + ".delay");
    }

    @Bean
    public Binding bindJobPostingDelay(Queue jobpostingDelayQueue, DirectExchange aiExchange) {
        return BindingBuilder.bind(jobpostingDelayQueue).to(aiExchange).with(QUEUE_JOBPOSTING + ".delay");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
