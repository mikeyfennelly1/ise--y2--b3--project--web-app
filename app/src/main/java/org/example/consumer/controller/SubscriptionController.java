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

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionController.class);

    private final SubscriptionFactory subscriptionFactory;

    @Autowired
    public SubscriptionController(
            SubscriptionFactory subscriptionFactory
    ) {
        this.subscriptionFactory = subscriptionFactory;
    }

    @GetMapping("")
    public List<String> getActiveCategorySubscriptions() {
        return subscriptionManager.getActiveSubscriptions();
    }

    @PostMapping("")
    public ResponseEntity<?> createNewCategorySubscription(@RequestBody(required = false) Map<String, String> body) {
        logger.debug("POST /api/consumer/subscriptions - body: {}", body);
        if (body == null || !body.containsKey("name") || body.get("name") == null || body.get("name").isBlank()) {
            logger.debug("POST /api/consumer/subscriptions - rejected: missing or blank 'name' field");
            return ResponseEntity.badRequest().body(Map.of("error", "Request body must include a 'name' field."));
        }
        if (!body.containsKey("parent")) {
            logger.debug("POST /api/consumer/subscriptions - rejected: missing 'parent' field");
            return ResponseEntity.badRequest().body(Map.of("error", "Request body must include a 'parent' field (set to null to create a root node)."));
        }
        try {
            subscriptionFactory.newSubscription(body.get("name"), body.get("parent"));
        } catch (InvalidSubscriptionTreePathFormatException e) {
            logger.debug("POST /api/consumer/subscriptions - rejected: invalid path format - {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (TreePathNotFoundException e) {
            logger.debug("POST /api/consumer/subscriptions - rejected: parent path not found - {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (SubscriptionAlreadyExistsException e) {
            logger.debug("POST /api/consumer/subscriptions - rejected: subscription already exists - {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
        logger.debug("POST /api/consumer/subscriptions - created subscription name='{}' parent='{}'", body.get("name"), body.get("parent"));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/children")
    public ResponseEntity<?> getChildSubscriptions(@RequestParam String parent) {
        logger.debug("GET /api/consumer/subscriptions/children - parent='{}'", parent);
        try {
            List<SubscriptionNode> childSubscriptions = subscriptionFactory.getChildSubscriptions(parent);
            return ResponseEntity.ok(childSubscriptions);
        } catch (InvalidSubscriptionTreePathFormatException e) {
            logger.debug("GET /api/consumer/subscriptions/children - rejected: invalid path format - {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (TreePathNotFoundException e) {
            logger.debug("GET /api/consumer/subscriptions/children - rejected: parent path not found - {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
}
