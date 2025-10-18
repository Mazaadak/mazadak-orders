package com.mazadak.orders.config;

import com.mazadak.orders.workflow.activities.FixedPriceCheckoutActivities;
import com.mazadak.orders.workflow.impl.FixedPriceCheckoutWorkflowImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TemporalConfig {

    @Bean
    public WorkflowServiceStubs serviceStubs() {
        // Default connects to localhost:7233.
        return WorkflowServiceStubs.newInstance();
    }

    @Bean
    public WorkflowClient workflowClient(WorkflowServiceStubs serviceStubs) {
        return WorkflowClient.newInstance(serviceStubs);
    }

    @Bean
    public WorkerFactory workerFactory(
            WorkflowClient workflowClient,
            FixedPriceCheckoutActivities activities) {

        WorkerFactory factory = WorkerFactory.newInstance(workflowClient);

        Worker worker = factory.newWorker("ORDER_TASK_QUEUE");

        worker.registerWorkflowImplementationTypes(FixedPriceCheckoutWorkflowImpl.class);

        worker.registerActivitiesImplementations(activities);


        factory.start();

        return factory;
    }
}

