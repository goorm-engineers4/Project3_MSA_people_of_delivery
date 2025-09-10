package com.example.cloudfour.cartservice.client;

import com.example.cloudfour.cartservice.commondto.UserAddressResponseDTO;
import com.example.cloudfour.cartservice.commondto.UserResponseDTO;
import com.example.cloudfour.cartservice.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name="user-service", path = "/internal/users", configuration = FeignConfig.class  )
public interface UserFeignClient {

    @GetMapping("/addresses/{userId}")
    UserAddressResponseDTO addressById(@PathVariable("userId") UUID userId);

    @GetMapping("/{userId}")
    UserResponseDTO userById(@PathVariable("userId") UUID userId);

    @RequestMapping(method = RequestMethod.HEAD, value = "/exists")
    ResponseEntity<Void> existUserHead(@RequestParam("userId") UUID userId);
}
