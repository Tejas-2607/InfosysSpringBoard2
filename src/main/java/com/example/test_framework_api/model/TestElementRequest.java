package com.example.test_framework_api.model;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * Request model for dynamic element testing.
 * Supports single action (string) or multiple (list of ActionStep).
 * Backward compatible: "action" as string works for single.
 */
public class TestElementRequest {

    @NotBlank(message = "URL is required")
    private String url;

    @NotBlank(message = "Element ID is required")
    private String elementId;

    private String action;  // Single action (backward compatible)

    private List<ActionStep> actions;  // Multiple actions (new)

    private String expectedResult; // Optional for final validation

    // ActionStep for multiple
    public static class ActionStep {
        private String type;  // "click", "doubleclick", "rightclick", "clear", "type", "submit"
        private String value; // For "type" (text to input)

        // Getters/Setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }

    // Constructors
    public TestElementRequest() {}

    // Getters & Setters
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getElementId() { return elementId; }
    public void setElementId(String elementId) { this.elementId = elementId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public List<ActionStep> getActions() { return actions; }
    public void setActions(List<ActionStep> actions) { this.actions = actions; }

    public String getExpectedResult() { return expectedResult; }
    public void setExpectedResult(String expectedResult) { this.expectedResult = expectedResult; }
}