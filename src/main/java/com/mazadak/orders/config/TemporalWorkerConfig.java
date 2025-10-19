package com.mazadak.orders.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.mazadak.orders.client.PaymentClient;
import com.mazadak.orders.client.UserClient;
import com.mazadak.orders.service.OrderService;
import com.mazadak.orders.workflow.activity.impl.AuctionCheckoutActivitiesImpl;
import com.mazadak.orders.workflow.impl.AuctionCheckoutWorkflowImpl;
import com.mazadak.orders.workflow.activity.impl.CheckoutActivitiesImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.common.converter.DefaultDataConverter;
import io.temporal.common.converter.JacksonJsonPayloadConverter;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TemporalWorkerConfig {
    @Bean
    public WorkflowServiceStubs workflowServiceStubs() {
        // Connects to the Temporal server (default: localhost:7233)
        return WorkflowServiceStubs.newLocalServiceStubs();
    }

    @Bean
    public WorkflowClient workflowClient(WorkflowServiceStubs service) {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new ParameterNamesModule())
                .registerModule(new JavaTimeModule());

        JacksonJsonPayloadConverter jsonConverter = new JacksonJsonPayloadConverter(objectMapper);

        DefaultDataConverter dataConverter = DefaultDataConverter.newDefaultInstance().withPayloadConverterOverrides(jsonConverter);

        return WorkflowClient.newInstance(
                service,
                WorkflowClientOptions.newBuilder()
                        .setDataConverter(dataConverter)
                        .build()
        );
    }

    @Bean
    public WorkerFactory workerFactory(WorkflowClient workflowClient) {
        return WorkerFactory.newInstance(workflowClient);
    }

    @Bean
    public AuctionCheckoutActivitiesImpl auctionCheckoutActivitiesImpl(StreamBridge streamBridge,
                                                                       OrderService orderService) {
        return new AuctionCheckoutActivitiesImpl(streamBridge, orderService);
    }

    @Bean
    public CheckoutActivitiesImpl checkoutActivitiesImpl(OrderService orderService, UserClient userClient, PaymentClient paymentClient) {
        return new CheckoutActivitiesImpl(orderService, userClient, paymentClient);
    }

    @Bean
    public Worker auctionCheckoutWorker(
            WorkerFactory workerFactory,
            AuctionCheckoutActivitiesImpl auctionCheckoutActivities,
            CheckoutActivitiesImpl checkoutActivities
    ) {
        Worker worker = workerFactory.newWorker("AUCTION_CHECKOUT_TASK_QUEUE");

        worker.registerWorkflowImplementationTypes(
                AuctionCheckoutWorkflowImpl.class
        );

        worker.registerActivitiesImplementations(
                auctionCheckoutActivities,
                checkoutActivities
        );

        workerFactory.start();
        return worker;
    }
}
