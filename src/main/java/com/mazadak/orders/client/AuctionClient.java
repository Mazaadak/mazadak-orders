package com.mazadak.orders.client;

import com.mazadak.orders.dto.client.AuctionResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "auctions")
public interface AuctionClient {
    @GetMapping("/auctions/{id}")
    ResponseEntity<AuctionResponse> findAuctionById(@PathVariable UUID id);
}
