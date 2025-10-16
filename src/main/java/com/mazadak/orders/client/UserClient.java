package com.mazadak.orders.client;

import com.mazadak.orders.dto.client.UserProfileResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(
        name = "users"
)
public interface UserClient {
    @GetMapping("/{user-id}")
    public ResponseEntity<UserProfileResponse> getUserProfile(@PathVariable("user-id") UUID userId);
}
