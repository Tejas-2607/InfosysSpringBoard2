import React, { useState } from "react";
import { useAuth } from "../../contexts/AuthContext";

const API_BASE = "http://localhost:8080";

const SingleTestExecution = () => {
  const { token } = useAuth();
  const [testType, setTestType] = useState("UI");
  const [formData, setFormData] = useState({
    url: "",
    elementId: "",
    action: "click",
    expectedResult: "",
    httpMethod: "GET",
    requestBody: "",
    inputValue: "", // FIX #1: Added input value field
  });
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setResult(null);

    try {
      const payload = {
        testType,
        url: formData.url,
        expectedResult: formData.expectedResult,
        ...(testType === "UI"
          ? {
              elementId: formData.elementId,
              action: formData.action,
              inputValue: formData.inputValue, // FIX #1: Include input value
            }
          : {
              httpMethod: formData.httpMethod,
              requestBody: formData.requestBody,
            }),
      };

      const response = await fetch(`${API_BASE}/test-element`, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify(payload),
      });

      const data = await response.json();

      if (response.ok) {
        setResult({
          success: true,
          message: data.message,
          testRunId: data.testRunId,
          resultsUrl: data.resultsUrl,
        });
      } else {
        setResult({
          success: false,
          message: data.error || "Test execution failed",
        });
      }
    } catch (error) {
      setResult({
        success: false,
        message: `Error: ${error.message}`,
      });
    } finally {
      setLoading(false);
    }
  };

  const handleInputChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    });
  };

  return (
    <div className="space-y-6">
      <h2 className="text-2xl font-bold text-gray-800">
        Single Test Execution
      </h2>

      <div className="bg-white rounded-lg shadow p-6">
        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Test Type Selection */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Test Type
            </label>
            <div className="flex gap-4">
              <label className="flex items-center">
                <input
                  type="radio"
                  name="testType"
                  value="UI"
                  checked={testType === "UI"}
                  onChange={(e) => setTestType(e.target.value)}
                  className="mr-2"
                />
                UI Test
              </label>
              <label className="flex items-center">
                <input
                  type="radio"
                  name="testType"
                  value="API"
                  checked={testType === "API"}
                  onChange={(e) => setTestType(e.target.value)}
                  className="mr-2"
                />
                API Test
              </label>
            </div>
          </div>

          {/* URL (Common) */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              URL {testType === "API" ? "Endpoint" : ""}
            </label>
            <input
              type="url"
              name="url"
              required
              placeholder={
                testType === "UI"
                  ? "https://example.com"
                  : "https://api.example.com/endpoint"
              }
              value={formData.url}
              onChange={handleInputChange}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
            />
          </div>

          {/* UI-specific fields */}
          {testType === "UI" && (
            <>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Element ID
                </label>
                <input
                  type="text"
                  name="elementId"
                  required
                  placeholder="button-id"
                  value={formData.elementId}
                  onChange={handleInputChange}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Action
                </label>
                <select
                  name="action"
                  value={formData.action}
                  onChange={handleInputChange}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                >
                  <option value="click">Click</option>
                  <option value="doubleclick">Double Click</option>
                  <option value="type">Type</option>
                  <option value="clear">Clear</option>
                  <option value="submit">Submit</option>
                  <option value="hover">Hover</option>
                </select>
              </div>

              {/* FIX #1: Input value field for type action */}
              {formData.action === "type" && (
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Input Value (Text to Type)
                  </label>
                  <input
                    type="text"
                    name="inputValue"
                    placeholder="Enter text to type into the element"
                    value={formData.inputValue}
                    onChange={handleInputChange}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                  />
                </div>
              )}
            </>
          )}

          {/* API-specific fields */}
          {testType === "API" && (
            <>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  HTTP Method
                </label>
                <select
                  name="httpMethod"
                  value={formData.httpMethod}
                  onChange={handleInputChange}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                >
                  <option value="GET">GET</option>
                  <option value="POST">POST</option>
                  <option value="PUT">PUT</option>
                  <option value="PATCH">PATCH</option>
                  <option value="DELETE">DELETE</option>
                </select>
              </div>

              {["POST", "PUT", "PATCH"].includes(formData.httpMethod) && (
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Request Body (JSON)
                  </label>
                  <textarea
                    name="requestBody"
                    rows="4"
                    placeholder='{"key": "value"}'
                    value={formData.requestBody}
                    onChange={handleInputChange}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 font-mono text-sm"
                  />
                </div>
              )}
            </>
          )}

          {/* Expected Result (Common) */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Expected Result (Optional)
            </label>
            <input
              type="text"
              name="expectedResult"
              placeholder={testType === "UI" ? "Success message" : "200 OK"}
              value={formData.expectedResult}
              onChange={handleInputChange}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
            />
          </div>

          {/* Submit Button */}
          <button
            type="submit"
            disabled={loading}
            className="w-full bg-green-600 text-white py-3 rounded-lg hover:bg-green-700 transition disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
          >
            {loading ? (
              <>
                <span className="animate-spin">⏳</span>
                Executing...
              </>
            ) : (
              <>
                <span>▶️</span>
                Execute Test
              </>
            )}
          </button>
        </form>

        {/* Result Display */}
        {result && (
          <div
            className={`mt-6 p-4 rounded-lg ${
              result.success
                ? "bg-green-50 border border-green-200"
                : "bg-red-50 border border-red-200"
            }`}
          >
            <h3
              className={`font-semibold mb-2 ${
                result.success ? "text-green-800" : "text-red-800"
              }`}
            >
              {result.success ? "✅ Test Passed" : "❌ Test Failed"}
            </h3>
            <p className={result.success ? "text-green-700" : "text-red-700"}>
              {result.message}
            </p>
            {result.testRunId && (
              <div className="mt-3 pt-3 border-t border-green-200">
                <p className="text-sm text-green-600">
                  Test Run ID:{" "}
                  <span className="font-mono font-semibold">
                    {result.testRunId}
                  </span>
                </p>
              </div>
            )}
          </div>
        )}
      </div>

      {/* Instructions */}
      <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
        <h3 className="font-semibold text-blue-900 mb-2">💡 Quick Guide</h3>
        <ul className="text-sm text-blue-800 space-y-1">
          <li>
            <strong>UI Tests:</strong> Test web page interactions (clicks,
            typing, etc.)
          </li>
          <li>
            <strong>API Tests:</strong> Test REST API endpoints (GET, POST,
            etc.)
          </li>
          <li>
            <strong>Type Action:</strong> Enter the text you want to type in the
            "Input Value" field
          </li>
          <li>Results are saved and can be viewed in the test runs history</li>
        </ul>
      </div>
    </div>
  );
};

export default SingleTestExecution;
