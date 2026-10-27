package com.amirak.ecommerce.home;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HomeController {

    @GetMapping("/")
    public Map<String, String> home(){
        return Map.of(
                "message", "E-commerce API is running.",
                "products", "/api/products"
        );
    }
}
