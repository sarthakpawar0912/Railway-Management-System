package com.railway.bookingservice.FiegnClient;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "USER-SERVICE")
public interface UserClient {
    // future calls
}