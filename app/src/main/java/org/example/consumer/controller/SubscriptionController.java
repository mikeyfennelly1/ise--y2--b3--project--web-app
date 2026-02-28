package org.example.consumer.controller;

import org.example.consumer.model.SourceCategory;
import org.example.consumer.subscriber.SubscriptionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/consumer/subscriptions")
public class SubscriptionController {

    private final SubscriptionManager subscriptionManager;

    @Autowired
    public SubscriptionController(
            SubscriptionManager subscriptionManager
    ) {
        this.subscriptionManager = subscriptionManager;
    }

    @GetMapping("")
    public List<String> getActiveCategorySubscriptions() {
        return subscriptionManager.getActiveSubscriptions();
    }

    @PostMapping("")
    public ResponseEntity<String> createNewCategorySubscription(@RequestBody(required = false) Map<String, String> body) {
        if (body == null || !body.containsKey("name") || body.get("name").isBlank()) {
            return ResponseEntity.badRequest().body("Request body must include a 'name' field.");
        }
        subscriptionManager.createNewCategorySubscription(body.get("name"));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/subcategories")
    public List<String> getSubcategories(@RequestParam String category) {
        return subscriptionManager.getSubcategoriesByCategory(category);
    }
}
