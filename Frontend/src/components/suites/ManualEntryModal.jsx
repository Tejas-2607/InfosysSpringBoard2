import React, { useState } from "react";
import { useAuth } from "../../contexts/AuthContext";

const API_BASE = "http://localhost:8080/api";

const ManualEntryModal = ({ onClose, onSuccess }) => {
  const [suiteName, setSuiteName] = useState("");
  const [description, setDescription] = useState("");
  const [testCases, setTestCases] = useState([
    {
      testCaseId: "",
      testName: "",
      testType: "UI",
      urlEndpoint: "",
      httpMethodAction: "GET",
      locatorType: "id",
      locatorValue: "",
      inputData: "",
      expectedResult: "",
      priority: "Medium",
      run: true,
      description: "",
    },
  ]);
  const [loading, setLoading] = useState(false);
  const { token } = useAuth();

  const addTestCase = () => {
    setTestCases([
      ...testCases,
      {
        testCaseId: "",
        testName: "",
        testType: "UI",
        urlEndpoint: "",
        httpMethodAction: "GET",
        locatorType: "id",
        locatorValue: "",
        inputData: "",
        expectedResult: "",
        priority: "Medium",
        run: true,
        description: "",
      },
    ]);
  };

  const updateTestCase = (index, field, value) => {
    const updated = [...testCases];
    updated[index][field] = value;
    setTestCases(updated);
  };

  const removeTestCase = (index) => {
    setTestCases(testCases.filter((_, i) => i !== index));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    // Convert to CSV format
    const headers =
      "testCaseId,testName,testType,urlEndpoint,httpMethodAction,locatorType,locatorValue,inputData,expectedResult,priority,run,description\n";
    const rows = testCases
      .map(
        (tc) =>
          `${tc.testCaseId},${tc.testName},${tc.testType},${tc.urlEndpoint},${tc.httpMethodAction},${tc.locatorType},${tc.locatorValue},${tc.inputData},${tc.expectedResult},${tc.priority},${tc.run},${tc.description}`
      )
      .join("\n");
    const csvContent = headers + rows;
    const blob = new Blob([csvContent], { type: "text/csv" });
    const formData = new FormData();
    formData.append("csvFile", blob, "test-suite.csv");
    formData.append("suiteName", suiteName);
    formData.append("description", description);
    try {
      const res = await fetch(`${API_BASE}/suites/import-csv`, {
        method: "POST",
        headers: { Authorization: `Bearer ${token}` },
        body: formData,
      });
      if (!res.ok) throw new Error("Failed to create suite");
      alert("Suite created successfully!");
      onSuccess();
      onClose();
    } catch (error) {
      alert("Failed to create suite: " + error.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50 overflow-y-auto">
      <div className="bg-white rounded-lg shadow-xl p-6 w-full max-w-4xl my-8">
        <h3 className="text-xl font-bold mb-4">Create Test Suite Manually</h3>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Suite Name
              </label>
              <input
                type="text"
                required
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                value={suiteName}
                onChange={(e) => setSuiteName(e.target.value)}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Description
              </label>
              <input
                type="text"
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
              />
            </div>
          </div>
          <div className="space-y-4 max-h-96 overflow-y-auto">
            {testCases.map((tc, index) => (
              <div
                key={index}
                className="border border-gray-200 rounded-lg p-4 space-y-3"
              >
                <div className="flex justify-between items-center">
                  <h4 className="font-semibold">Test Case {index + 1}</h4>
                  {testCases.length > 1 && (
                    <button
                      type="button"
                      onClick={() => removeTestCase(index)}
                      className="text-red-600 hover:text-red-800"
                    >
                      <span className="w-4 h-4">🗑️</span>
                    </button>
                  )}
                </div>
                <div className="grid grid-cols-3 gap-3">
                  <input
                    type="text"
                    placeholder="Test Case ID"
                    required
                    className="px-3 py-2 border border-gray-300 rounded text-sm"
                    value={tc.testCaseId}
                    onChange={(e) =>
                      updateTestCase(index, "testCaseId", e.target.value)
                    }
                  />
                  <input
                    type="text"
                    placeholder="Test Name"
                    required
                    className="px-3 py-2 border border-gray-300 rounded text-sm"
                    value={tc.testName}
                    onChange={(e) =>
                      updateTestCase(index, "testName", e.target.value)
                    }
                  />
                  <select
                    className="px-3 py-2 border border-gray-300 rounded text-sm"
                    value={tc.testType}
                    onChange={(e) =>
                      updateTestCase(index, "testType", e.target.value)
                    }
                  >
                    <option value="UI">UI</option>
                    <option value="API">API</option>
                  </select>
                </div>
                <div className="grid grid-cols-2 gap-3">
                  <input
                    type="text"
                    placeholder="URL Endpoint"
                    required
                    className="px-3 py-2 border border-gray-300 rounded text-sm"
                    value={tc.urlEndpoint}
                    onChange={(e) =>
                      updateTestCase(index, "urlEndpoint", e.target.value)
                    }
                  />
                  <select
                    className="px-3 py-2 border border-gray-300 rounded text-sm"
                    value={tc.httpMethodAction}
                    onChange={(e) =>
                      updateTestCase(index, "httpMethodAction", e.target.value)
                    }
                  >
                    <option value="GET">GET</option>
                    <option value="POST">POST</option>
                    <option value="PUT">PUT</option>
                    <option value="DELETE">DELETE</option>
                    <option value="click">click</option>
                    <option value="type">type</option>
                  </select>
                </div>
                {tc.testType === "UI" && (
                  <div className="grid grid-cols-2 gap-3">
                    <select
                      className="px-3 py-2 border border-gray-300 rounded text-sm"
                      value={tc.locatorType}
                      onChange={(e) =>
                        updateTestCase(index, "locatorType", e.target.value)
                      }
                    >
                      <option value="id">ID</option>
                      <option value="name">Name</option>
                      <option value="xpath">XPath</option>
                      <option value="css">CSS</option>
                    </select>
                    <input
                      type="text"
                      placeholder="Locator Value"
                      className="px-3 py-2 border border-gray-300 rounded text-sm"
                      value={tc.locatorValue}
                      onChange={(e) =>
                        updateTestCase(index, "locatorValue", e.target.value)
                      }
                    />
                  </div>
                )}
                <div className="grid grid-cols-2 gap-3">
                  <input
                    type="text"
                    placeholder="Input Data"
                    className="px-3 py-2 border border-gray-300 rounded text-sm"
                    value={tc.inputData}
                    onChange={(e) =>
                      updateTestCase(index, "inputData", e.target.value)
                    }
                  />
                  <input
                    type="text"
                    placeholder="Expected Result"
                    className="px-3 py-2 border border-gray-300 rounded text-sm"
                    value={tc.expectedResult}
                    onChange={(e) =>
                      updateTestCase(index, "expectedResult", e.target.value)
                    }
                  />
                </div>
                <div className="grid grid-cols-3 gap-3">
                  <select
                    className="px-3 py-2 border border-gray-300 rounded text-sm"
                    value={tc.priority}
                    onChange={(e) =>
                      updateTestCase(index, "priority", e.target.value)
                    }
                  >
                    <option value="High">High</option>
                    <option value="Medium">Medium</option>
                    <option value="Low">Low</option>
                  </select>
                  <label className="flex items-center gap-2">
                    <input
                      type="checkbox"
                      checked={tc.run}
                      onChange={(e) =>
                        updateTestCase(index, "run", e.target.checked)
                      }
                    />
                    <span className="text-sm">Run</span>
                  </label>
                  <input
                    type="text"
                    placeholder="Description"
                    className="px-3 py-2 border border-gray-300 rounded text-sm"
                    value={tc.description}
                    onChange={(e) =>
                      updateTestCase(index, "description", e.target.value)
                    }
                  />
                </div>
              </div>
            ))}
          </div>
          <button
            type="button"
            onClick={addTestCase}
            className="w-full bg-green-600 text-white py-2 rounded hover:bg-green-700 transition flex items-center justify-center gap-2"
          >
            <span className="w-4 h-4">+</span> Add Another Test Case
          </button>
          <div className="flex gap-2">
            <button
              type="submit"
              disabled={loading}
              className="flex-1 bg-blue-600 text-white py-2 rounded hover:bg-blue-700 transition disabled:opacity-50"
            >
              {loading ? "Creating..." : "Create Suite"}
            </button>
            <button
              type="button"
              onClick={onClose}
              className="flex-1 bg-gray-300 text-gray-700 py-2 rounded hover:bg-gray-400 transition"
            >
              Cancel
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default ManualEntryModal;
