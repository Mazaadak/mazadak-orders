package com.mazadak.orders.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "product-catalog")
public interface ProductClient {

}
