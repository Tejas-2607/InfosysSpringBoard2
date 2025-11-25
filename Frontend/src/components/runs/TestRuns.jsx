import React, { useState, useEffect } from "react";
import { useAuth } from "../../contexts/AuthContext";

const API_BASE = "http://localhost:8080/api";

const TestRuns = () => {
  const [activeTab, setActiveTab] = useState("runs"); // FIX #5: Two tabs
  const [testRuns, setTestRuns] = useState([]);
  const [testResults, setTestResults] = useState([]);
  const [showAllRuns, setShowAllRuns] = useState(false); // FIX #5: Pagination
  const [showAllResults, setShowAllResults] = useState(false); // FIX #5: Pagination
  const [loading, setLoading] = useState(true);
  const { token } = useAuth();

  useEffect(() => {
    fetchData();
  }, [token]);

  const fetchData = async () => {
    setLoading(true);
    try {
      // FIX #5: Fetch test runs from GET /api/runs
      const runsRes = await fetch(`${API_BASE}/runs`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      const runsData = await runsRes.json();
      // FIX #5: Reverse to show latest first
      setTestRuns(Array.isArray(runsData) ? runsData.reverse() : []);

      // FIX #5: Fetch test results from GET /api/runs/reports
      const resultsRes = await fetch(`${API_BASE}/runs/reports`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      const resultsData = await resultsRes.json();
      // FIX #5: Reverse to show latest first
      setTestResults(Array.isArray(resultsData) ? resultsData.reverse() : []);
    } catch (error) {
      console.error("Failed to fetch test data:", error);
      setTestRuns([]);
      setTestResults([]);
    } finally {
      setLoading(false);
    }
  };

  const generateReport = async (runId) => {
    try {
      const res = await fetch(`${API_BASE}/runs/${runId}/report`, {
        method: "POST",
        headers: { Authorization: `Bearer ${token}` },
      });
      const data = await res.json();
      if (data.url) {
        window.open(data.url, "_blank");
      } else {
        alert("Report generated! Check reports folder.");
      }
    } catch (error) {
      alert("Failed to generate report: " + error.message);
    }
  };

  // FIX #5: Show top 10 or all
  const displayedRuns = showAllRuns ? testRuns : testRuns.slice(0, 10);
  const displayedResults = showAllResults
    ? testResults
    : testResults.slice(0, 10);

  const formatDate = (dateStr) => {
    if (!dateStr) return "N/A";
    try {
      return new Date(dateStr).toLocaleString();
    } catch {
      return "Invalid Date";
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-gray-600">Loading test data...</div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <h2 className="text-2xl font-bold text-gray-800">Test Runs (Admin)</h2>

      {/* FIX #5: Tabs for Test Runs and Test Results */}
      <div className="flex gap-2 border-b border-gray-200">
        <button
          onClick={() => setActiveTab("runs")}
          className={`px-6 py-3 font-medium transition ${
            activeTab === "runs"
              ? "border-b-2 border-blue-600 text-blue-600 bg-blue-50"
              : "text-gray-600 hover:bg-gray-50"
          }`}
        >
          📋 Test Runs ({testRuns.length})
        </button>
        <button
          onClick={() => setActiveTab("results")}
          className={`px-6 py-3 font-medium transition ${
            activeTab === "results"
              ? "border-b-2 border-blue-600 text-blue-600 bg-blue-50"
              : "text-gray-600 hover:bg-gray-50"
          }`}
        >
          📊 Test Results ({testResults.length})
        </button>
      </div>

      {/* Test Runs Tab */}
      {activeTab === "runs" && (
        <div className="bg-white rounded-lg shadow overflow-hidden">
          {testRuns.length === 0 ? (
            <div className="p-8 text-center text-gray-500">
              No test runs found. Execute a test suite to create test runs.
            </div>
          ) : (
            <>
              <table className="min-w-full">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      ID
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Name
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Status
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Threads
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Created
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Actions
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {displayedRuns.map((run) => (
                    <tr key={run.id} className="hover:bg-gray-50">
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                        {run.id}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900 font-medium">
                        {run.name}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <span
                          className={`px-2 py-1 text-xs rounded-full ${
                            run.status === "PASSED"
                              ? "bg-green-100 text-green-800"
                              : run.status === "FAILED"
                              ? "bg-red-100 text-red-800"
                              : run.status === "RUNNING"
                              ? "bg-blue-100 text-blue-800"
                              : "bg-gray-100 text-gray-800"
                          }`}
                        >
                          {run.status}
                        </span>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                        {run.parallelThreads || 1}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        {formatDate(run.createdAt)}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm">
                        <button
                          onClick={() => generateReport(run.id)}
                          className="text-blue-600 hover:text-blue-800 font-medium"
                        >
                          View Report
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>

              {/* FIX #5: Load More button */}
              {testRuns.length > 10 && !showAllRuns && (
                <div className="p-4 text-center border-t bg-gray-50">
                  <button
                    onClick={() => setShowAllRuns(true)}
                    className="text-blue-600 hover:text-blue-800 font-medium"
                  >
                    📥 Load More ({testRuns.length - 10} remaining)
                  </button>
                </div>
              )}
              {showAllRuns && testRuns.length > 10 && (
                <div className="p-4 text-center border-t bg-gray-50">
                  <button
                    onClick={() => setShowAllRuns(false)}
                    className="text-gray-600 hover:text-gray-800 font-medium"
                  >
                    📤 Show Less
                  </button>
                </div>
              )}
            </>
          )}
        </div>
      )}

      {/* Test Results Tab */}
      {activeTab === "results" && (
        <div className="bg-white rounded-lg shadow overflow-hidden">
          {testResults.length === 0 ? (
            <div className="p-8 text-center text-gray-500">
              No test results found. Execute tests to see results here.
            </div>
          ) : (
            <>
              <table className="min-w-full">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      ID
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Test Name
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Status
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Duration
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Retries
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Created
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {displayedResults.map((result) => (
                    <tr key={result.id} className="hover:bg-gray-50">
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                        {result.id}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900 font-medium">
                        {result.testName}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <span
                          className={`px-2 py-1 text-xs rounded-full ${
                            result.status === "PASSED"
                              ? "bg-green-100 text-green-800"
                              : "bg-red-100 text-red-800"
                          }`}
                        >
                          {result.status}
                        </span>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        {result.duration ? `${result.duration}ms` : "N/A"}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        {result.retryCount || 0}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        {formatDate(result.createdAt)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>

              {/* FIX #5: Load More button */}
              {testResults.length > 10 && !showAllResults && (
                <div className="p-4 text-center border-t bg-gray-50">
                  <button
                    onClick={() => setShowAllResults(true)}
                    className="text-blue-600 hover:text-blue-800 font-medium"
                  >
                    📥 Load More ({testResults.length - 10} remaining)
                  </button>
                </div>
              )}
              {showAllResults && testResults.length > 10 && (
                <div className="p-4 text-center border-t bg-gray-50">
                  <button
                    onClick={() => setShowAllResults(false)}
                    className="text-gray-600 hover:text-gray-800 font-medium"
                  >
                    📤 Show Less
                  </button>
                </div>
              )}
            </>
          )}
        </div>
      )}
    </div>
  );
};

export default TestRuns;