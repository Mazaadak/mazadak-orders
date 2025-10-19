package com.mazadak.orders.client;

import com.mazadak.orders.dto.client.UserProfileResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(name = "user-service")
public interface UserClient {
    @GetMapping("/users/{user-id}")
    public ResponseEntity<UserProfileResponse> getUser(@RequestHeader("user-id") UUID userId, @PathVariable("user-id") UUID user );
}
