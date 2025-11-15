package com.example.test_framework_api.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * types to check
 * 
 * UI Test Example:
 * {
 * "testType": "UI",
 * "url": "https://example.com",
 * "elementId": "submit-button",
 * "action": "click",
 * "expectedResult": "Form submitted"
 * }
 * 
 * UI Multi-Action Example:
 * {
 * "testType": "UI",
 * "url": "https://example.com/form",
 * "elementId": "username",
 * "actions": [
 * {"type": "clear"},
 * {"type": "type", "value": "testuser"},
 * {"type": "submit"}
 * ]
 * }
 * 
 * API GET Example:
 * {
 * "testType": "API",
 * "url": "https://api.example.com/users",
 * "httpMethod": "GET",
 * "expectedResult": "200"
 * }
 * 
 * API POST Example:
 * {
 * "testType": "API",
 * "url": "https://api.example.com/users",
 * "httpMethod": "POST",
 * "requestBody": "{\"name\":\"John\",\"email\":\"john@example.com\"}",
 * "expectedResult": "201"
 * }
 */
@Data
public class TestElementRequest {

    // === COMMON FIELDS (UI & API) ===

    @NotBlank(message = "URL is required")
    private String url;

    private String testType; // "UI" or "API" (default: UI)

    private String expectedResult; // UI: text/title/alert, API: status code or body content

    // === UI-SPECIFIC FIELDS ===

    private String elementId; // Element ID to interact with

    private String action; // Single action: "click", "type", "submit", etc.

    private List<ActionStep> actions; // Multiple actions (advanced)

    private String inputValue; // For "type" action (alternative to ActionStep value)

    // === API-SPECIFIC FIELDS ===

    private String httpMethod; // GET, POST, PUT, PATCH, DELETE

    private String requestBody; // JSON body for POST/PUT/PATCH

    /**
     * Action step for multi-action UI tests
     */
    @Data
    public static class ActionStep {
        private String type; // "click", "doubleclick", "rightclick", "clear", "type", "submit", "hover"
        private String value; // For "type" action (text to input)
    }

    // === VALIDATION HELPERS ===

    public boolean isUITest() {
        return testType == null || "UI".equalsIgnoreCase(testType);
    }

    public boolean isAPITest() {
        return "API".equalsIgnoreCase(testType);
    }

    public boolean hasMultipleActions() {
        return actions != null && !actions.isEmpty();
    }
}