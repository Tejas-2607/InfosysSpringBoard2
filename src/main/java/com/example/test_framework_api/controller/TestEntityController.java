// package com.example.test_framework_api.controller;

// import java.util.HashMap;
// // import java.util.List;
// import java.util.Map;
// import java.util.stream.Collectors;

// import com.example.test_framework_api.model.TestCase;
// import com.example.test_framework_api.service.TestRunService;
// import com.example.test_framework_api.worker.TestExecutor;

// // import org.springframework.amqp.rabbit.core.RabbitTemplate;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.PathVariable;
// import org.springframework.web.bind.annotation.PostMapping;
// import org.springframework.web.bind.annotation.RequestBody;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RestController;

// import com.example.test_framework_api.model.TestElementRequest;
// import com.example.test_framework_api.model.TestRun;
// import com.example.test_framework_api.model.TestRunRequest;
// import lombok.extern.slf4j.Slf4j;

// @RestController
// @RequestMapping("/test-element")
// @Slf4j
// public class TestEntityController {
//   // @Autowired
//   // private RabbitTemplate rabbitTemplate;
//   @Autowired
//   private TestRunService testRunService;
//   @Autowired
//   private TestExecutor testExecutor;

//   /**
//    * UNIFIED ENDPOINT: Execute UI or API test immediately
//    * 
//    * Request format:
//    * {
//    *   "testType": "UI" or "API",
//    *   "url": "https://example.com",
//    *   "elementId": "button-id",        // UI only
//    *   "action": "click",                // UI only (or HTTP method for API)
//    *   "actions": [                      // UI only (multiple actions)
//    *     {"type": "click"},
//    *     {"type": "type", "value": "text"}
//    *   ],
//    *   "httpMethod": "GET",              // API only
//    *   "requestBody": "{...}",           // API POST/PUT/PATCH
//    *   "expectedResult": "200 OK"
//    * }
//    */
//   @PostMapping
//   public ResponseEntity<?> runTestElement(@RequestBody TestElementRequest request) {
//     try {
//       // Validate request
//       if (request.getUrl() == null || request.getUrl().trim().isEmpty()) {
//         return ResponseEntity.badRequest()
//             .body(Map.of("error", "URL is required"));
//       }

//       String testType = request.getTestType() != null ? request.getTestType().toUpperCase() : "UI";
      
//       // Create test run for tracking
//       TestRunRequest trRequest = new TestRunRequest();
//       trRequest.setSuiteName("Dynamic Test: " + testType + " - " + 
//           (request.getElementId() != null ? request.getElementId() : request.getUrl()));
//       TestRun testRun = testRunService.createTestRun(trRequest);

//       // Build TestCase from request
//       TestCase testCase = buildTestCaseFromRequest(request, testType);

//       // Execute test synchronously (no queue)
//       log.info("Executing {} test: {}", testType, testCase.getTestName());
//       testExecutor.executeTestCase(testCase, testRun);

//       return ResponseEntity.ok(Map.of(
//           "message", "Test executed successfully",
//           "testRunId", testRun.getId(),
//           "testType", testType,
//           "status", "COMPLETED",
//           "resultsUrl", "/test-element/" + testRun.getId() + "/results"));

//     } catch (Exception e) {
//       log.error("Test execution failed: {}", e.getMessage());
//       return ResponseEntity.internalServerError()
//           .body(Map.of(
//               "error", "Test execution failed",
//               "message", e.getMessage()));
//     }
//   }

//   /**
//    * Get test run status
//    */
//   @GetMapping("/{id}")
//   public ResponseEntity<?> getTestRunStatus(@PathVariable Long id) {
//     TestRun testRun = testRunService.getTestRunById(id);

//     if (testRun == null) {
//       return ResponseEntity.notFound().build();
//     }

//     return ResponseEntity.ok(Map.of(
//         "testRunId", testRun.getId(),
//         "suiteName", testRun.getName(),
//         "status", testRun.getStatus(),
//         "createdAt", testRun.getCreatedAt(),
//         "resultsUrl", "/test-element/" + id + "/results"));
//   }

//   /**
//    * Get detailed test results
//    */
//   @GetMapping("/{id}/results")
//   public ResponseEntity<?> getTestResults(@PathVariable Long id) {
//     TestRun testRun = testRunService.getTestRunById(id);
//     if (testRun == null) {
//       return ResponseEntity.notFound().build();
//     }

//     var testResultsList = testRunService.getTestResultsByTestRunId(id).stream()
//         .map(result -> {
//           Map<String, Object> resultMap = new HashMap<>();
//           resultMap.put("id", result.getId());
//           resultMap.put("testName", result.getTestName());
//           resultMap.put("status", result.getStatus());
//           resultMap.put("retryCount", result.getRetryCount());
//           resultMap.put("duration", result.getDuration());
//           resultMap.put("createdAt", result.getCreatedAt());
//           if (result.getErrorMessage() != null) {
//             resultMap.put("errorMessage", result.getErrorMessage());
//           }
//           return resultMap;
//         })
//         .collect(Collectors.toList());

//     return ResponseEntity.ok(Map.of(
//         "testRunId", id,
//         "testRun", Map.of(
//             "name", testRun.getName(),
//             "status", testRun.getStatus(),
//             "createdAt", testRun.getCreatedAt()),
//         "testResults", testResultsList));
//   }

//   /**
//    * Build TestCase from unified request
//    */
//   private TestCase buildTestCaseFromRequest(TestElementRequest request, String testType) {
//     TestCase testCase = new TestCase();
//     testCase.setTestName("Dynamic Test: " + testType);
//     testCase.setTestType(testType);
//     testCase.setUrlEndpoint(request.getUrl());

//     if ("UI".equals(testType)) {
//       // UI Test
//       testCase.setLocatorType("id");
//       testCase.setLocatorValue(request.getElementId());
      
//       // Action priority: actions > action
//       if (request.getActions() != null && !request.getActions().isEmpty()) {
//         // Multi-action (convert to JSON)
//         try {
//           testCase.setActionsJson(new com.fasterxml.jackson.databind.ObjectMapper()
//               .writeValueAsString(request.getActions()));
//           testCase.setHttpMethodAction("multi");
//         } catch (Exception e) {
//           log.error("Failed to serialize actions: {}", e.getMessage());
//           testCase.setHttpMethodAction(request.getAction() != null ? request.getAction() : "click");
//         }
//       } else {
//         // Single action
//         testCase.setHttpMethodAction(request.getAction() != null ? request.getAction() : "click");
//       }
      
//       testCase.setInputData(request.getInputValue());
      
//     } else if ("API".equals(testType)) {
//       // API Test
//       String httpMethod = request.getHttpMethod() != null ? 
//           request.getHttpMethod().toUpperCase() : "GET";
//       testCase.setHttpMethodAction(httpMethod);
//       testCase.setInputData(request.getRequestBody());
//     }

//     testCase.setExpectedResult(request.getExpectedResult());
//     testCase.setRun(true);
    
//     return testCase;
//   }
// }
package com.example.test_framework_api.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.example.test_framework_api.model.TestCase;
import com.example.test_framework_api.model.User;
import com.example.test_framework_api.repository.UserRepository;
import com.example.test_framework_api.service.TestRunService;
import com.example.test_framework_api.worker.TestExecutor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.test_framework_api.model.TestElementRequest;
import com.example.test_framework_api.model.TestRun;
import com.example.test_framework_api.model.TestRunRequest;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/test-element")
@Slf4j
public class TestEntityController {
  @Autowired
  private TestRunService testRunService;
  @Autowired
  private TestExecutor testExecutor;
  @Autowired
  private UserRepository userRepository;

  @PostMapping
  public ResponseEntity<?> runTestElement(@RequestBody TestElementRequest request, Authentication authentication) {
    try {
      // Get current user
      String username = authentication.getName();
      User currentUser = userRepository.findByUsername(username).orElseThrow();

      // Validate request
      if (request.getUrl() == null || request.getUrl().trim().isEmpty()) {
        return ResponseEntity.badRequest()
            .body(Map.of("error", "URL is required"));
      }

      String testType = request.getTestType() != null ? request.getTestType().toUpperCase() : "UI";
      
      // Create test run for tracking
      TestRunRequest trRequest = new TestRunRequest();
      trRequest.setSuiteName("Dynamic Test: " + testType + " - " + 
          (request.getElementId() != null ? request.getElementId() : request.getUrl()));
      TestRun testRun = testRunService.createTestRun(trRequest);
      
      // NEW: Set the user who created this test run
      testRun.setCreatedBy(currentUser);
      testRunService.updateTestRun(testRun);

      // Build TestCase from request
      TestCase testCase = buildTestCaseFromRequest(request, testType);

      // Execute test synchronously (no queue)
      log.info("User '{}' executing {} test: {}", username, testType, testCase.getTestName());
      testExecutor.executeTestCase(testCase, testRun);

      return ResponseEntity.ok(Map.of(
          "message", "Test executed successfully",
          "testRunId", testRun.getId(),
          "executedBy", username,
          "userId", currentUser.getId(),
          "testType", testType,
          "status", "COMPLETED",
          "resultsUrl", "/test-element/" + testRun.getId() + "/results"));

    } catch (Exception e) {
      log.error("Test execution failed: {}", e.getMessage());
      return ResponseEntity.internalServerError()
          .body(Map.of(
              "error", "Test execution failed",
              "message", e.getMessage()));
    }
  }

  @GetMapping("/{id}")
  public ResponseEntity<?> getTestRunStatus(@PathVariable Long id, Authentication authentication) {
    TestRun testRun = testRunService.getTestRunById(id);

    if (testRun == null) {
      return ResponseEntity.notFound().build();
    }

    Map<String, Object> response = new HashMap<>();
    response.put("testRunId", testRun.getId());
    response.put("suiteName", testRun.getName());
    response.put("status", testRun.getStatus());
    response.put("createdAt", testRun.getCreatedAt());
    response.put("resultsUrl", "/test-element/" + id + "/results");
    
    // NEW: Include user info
    if (testRun.getCreatedBy() != null) {
      response.put("executedBy", Map.of(
          "userId", testRun.getCreatedBy().getId(),
          "username", testRun.getCreatedBy().getUsername()
      ));
    }

    return ResponseEntity.ok(response);
  }

  @GetMapping("/{id}/results")
  public ResponseEntity<?> getTestResults(@PathVariable Long id) {
    TestRun testRun = testRunService.getTestRunById(id);
    if (testRun == null) {
      return ResponseEntity.notFound().build();
    }

    var testResultsList = testRunService.getTestResultsByTestRunId(id).stream()
        .map(result -> {
          Map<String, Object> resultMap = new HashMap<>();
          resultMap.put("id", result.getId());
          resultMap.put("testName", result.getTestName());
          resultMap.put("status", result.getStatus());
          resultMap.put("retryCount", result.getRetryCount());
          resultMap.put("duration", result.getDuration());
          resultMap.put("createdAt", result.getCreatedAt());
          
          // NEW: Include user info
          if (result.getExecutedBy() != null) {
            resultMap.put("executedBy", Map.of(
                "userId", result.getExecutedBy().getId(),
                "username", result.getExecutedBy().getUsername()
            ));
          }
          
          if (result.getErrorMessage() != null) {
            resultMap.put("errorMessage", result.getErrorMessage());
          }
          return resultMap;
        })
        .collect(Collectors.toList());

    Map<String, Object> response = new HashMap<>();
    response.put("testRunId", id);
    
    Map<String, Object> runInfo = new HashMap<>();
    runInfo.put("name", testRun.getName());
    runInfo.put("status", testRun.getStatus());
    runInfo.put("createdAt", testRun.getCreatedAt());
    
    if (testRun.getCreatedBy() != null) {
      runInfo.put("executedBy", Map.of(
          "userId", testRun.getCreatedBy().getId(),
          "username", testRun.getCreatedBy().getUsername()
      ));
    }
    
    response.put("testRun", runInfo);
    response.put("testResults", testResultsList);

    return ResponseEntity.ok(response);
  }

  private TestCase buildTestCaseFromRequest(TestElementRequest request, String testType) {
    TestCase testCase = new TestCase();
    testCase.setTestName("Dynamic Test: " + testType);
    testCase.setTestType(testType);
    testCase.setUrlEndpoint(request.getUrl());

    if ("UI".equals(testType)) {
      testCase.setLocatorType("id");
      testCase.setLocatorValue(request.getElementId());
      
      if (request.getActions() != null && !request.getActions().isEmpty()) {
        try {
          testCase.setActionsJson(new com.fasterxml.jackson.databind.ObjectMapper()
              .writeValueAsString(request.getActions()));
          testCase.setHttpMethodAction("multi");
        } catch (Exception e) {
          log.error("Failed to serialize actions: {}", e.getMessage());
          testCase.setHttpMethodAction(request.getAction() != null ? request.getAction() : "click");
        }
      } else {
        testCase.setHttpMethodAction(request.getAction() != null ? request.getAction() : "click");
      }
      
      testCase.setInputData(request.getInputValue());
      
    } else if ("API".equals(testType)) {
      String httpMethod = request.getHttpMethod() != null ? 
          request.getHttpMethod().toUpperCase() : "GET";
      testCase.setHttpMethodAction(httpMethod);
      testCase.setInputData(request.getRequestBody());
    }

    testCase.setExpectedResult(request.getExpectedResult());
    testCase.setRun(true);
    
    return testCase;
  }
}