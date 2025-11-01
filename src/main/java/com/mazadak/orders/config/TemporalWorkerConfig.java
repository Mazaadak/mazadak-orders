package com.mazadak.orders.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.mazadak.orders.client.*;
import com.mazadak.orders.service.OrderService;
import com.mazadak.orders.workflow.activity.FixedPriceCheckoutActivities;
import com.mazadak.orders.workflow.activity.impl.AuctionCheckoutActivitiesImpl;
import com.mazadak.orders.workflow.activity.CheckoutActivities;
import com.mazadak.orders.workflow.activity.impl.FixedPriceCheckoutActivitiesImpl;
import com.mazadak.orders.workflow.impl.AuctionCheckoutWorkflowImpl;
import com.mazadak.orders.workflow.activity.impl.CheckoutActivitiesImpl;
import com.mazadak.orders.workflow.impl.FixedPriceCheckoutWorkflowImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.common.converter.DefaultDataConverter;
import io.temporal.common.converter.JacksonJsonPayloadConverter;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TemporalWorkerConfig {
    @Value("${temporal.address:localhost:7233}")
    private String temporalAddress;

    @Bean
    public WorkflowServiceStubs workflowServiceStubs() {
        // Connect to Temporal server using configured address
        return WorkflowServiceStubs.newServiceStubs(
                WorkflowServiceStubsOptions.newBuilder()
                        .setTarget(temporalAddress)
                        .build()
        );
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

        return worker;
    }

    @Bean
    public Worker fixedPriceCheckoutWorker(
            WorkerFactory workerFactory,
            FixedPriceCheckoutActivities fixedPriceCheckoutActivities,
            CheckoutActivities checkoutActivities) {
        Worker worker = workerFactory.newWorker("FIXED_PRICE_CHECKOUT_TASK_QUEUE");

        worker.registerWorkflowImplementationTypes(
                FixedPriceCheckoutWorkflowImpl.class
        );

        worker.registerActivitiesImplementations(
                fixedPriceCheckoutActivities,
                checkoutActivities
        );

        return worker;
    }

    @Bean
    public ApplicationRunner startWorkerFactory(WorkerFactory workerFactory) {
        return args -> workerFactory.start();
    }
}
