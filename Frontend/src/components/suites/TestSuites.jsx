import React, { useState, useEffect } from "react";
import { useAuth } from "../../contexts/AuthContext";
import ImportCSVModal from "./ImportCSVModal";
import ManualEntryModal from "./ManualEntryModal";

const API_BASE = "http://localhost:8080/api";

const TestSuites = () => {
  const [suites, setSuites] = useState([]);
  const [showImport, setShowImport] = useState(false);
  const [showManual, setShowManual] = useState(false);
  const [showThreadModal, setShowThreadModal] = useState(false); // FIX #2
  const [selectedSuite, setSelectedSuite] = useState(null); // FIX #2
  const [parallelThreads, setParallelThreads] = useState(1); // FIX #2
  const { token, user } = useAuth();
  const isAdmin = user?.roles?.includes("ROLE_ADMIN");

  useEffect(() => {
    fetchSuites();
  }, []);

  const fetchSuites = () => {
    const endpoint = isAdmin
      ? `${API_BASE}/suites`
      : `${API_BASE}/suites/my-suites`;

    fetch(endpoint, {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then((res) => res.json())
      .then(setSuites)
      .catch(console.error);
  };

  // FIX #2: Open thread selection modal
  const handleRunClick = (suite) => {
    setSelectedSuite(suite);
    setParallelThreads(1); // Reset to default
    setShowThreadModal(true);
  };

  // FIX #2: Execute suite with selected threads
  const executeSuite = async () => {
    if (!selectedSuite) return;

    try {
      const res = await fetch(
        `${API_BASE}/suites/${selectedSuite.id}/execute-parallel?parallelThreads=${parallelThreads}`,
        {
          method: "POST",
          headers: { Authorization: `Bearer ${token}` },
        }
      );
      const data = await res.json();
      alert(
        `Suite execution started!\nTest Run ID: ${data.testRunId}\nMode: ${data.mode}\nThreads: ${data.parallelThreads}`
      );
      setShowThreadModal(false);
      fetchSuites();
    } catch (error) {
      alert("Failed to execute suite: " + error.message);
    }
  };

  const generateReport = async (suiteId) => {
    try {
      const res = await fetch(`${API_BASE}/suites/${suiteId}/report`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      const data = await res.json();

      if (data.reportPath) {
        const reportUrl = `http://localhost:8080/reports/suite-${suiteId}/suite-report.html`;
        window.open(reportUrl, "_blank");
      } else {
        alert(data.message || "Report generated successfully!");
      }
    } catch (error) {
      alert("Failed to generate report: " + error.message);
    }
  };

  const exportCSV = async (suiteId) => {
    try {
      const res = await fetch(`${API_BASE}/suites/${suiteId}/export/csv`, {
        headers: { Authorization: `Bearer ${token}` },
      });

      if (!res.ok) {
        const error = await res.json();
        alert(error.message || "Failed to export CSV");
        return;
      }

      const blob = await res.blob();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `suite-${suiteId}-report.csv`;
      a.click();
    } catch (error) {
      alert("Failed to export CSV: " + error.message);
    }
  };

  // FIX #3: Proper date formatting
  const formatDate = (dateStr) => {
    if (!dateStr) return "N/A";
    try {
      const date = new Date(dateStr);
      if (isNaN(date.getTime())) return "Invalid Date";
      return date.toLocaleDateString("en-US", {
        year: "numeric",
        month: "short",
        day: "numeric",
      });
    } catch {
      return "Invalid Date";
    }
  };

  const formatRole = (role) => {
    return role?.replace("ROLE_", "") || "";
  };

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h2 className="text-2xl font-bold text-gray-800">
          {isAdmin ? "All Test Suites" : "My Test Suites"}
        </h2>
        <div className="flex gap-2">
          <button
            onClick={() => setShowManual(true)}
            className="bg-green-600 text-white px-4 py-2 rounded hover:bg-green-700 transition flex items-center gap-2"
          >
            <span className="w-4 h-4">+</span> Manual Entry
          </button>
          <button
            onClick={() => setShowImport(true)}
            className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 transition flex items-center gap-2"
          >
            <span className="w-4 h-4">📤</span> Import CSV
          </button>
        </div>
      </div>

      {suites.length === 0 ? (
        <div className="bg-white rounded-lg shadow p-8 text-center">
          <p className="text-gray-600">
            {isAdmin
              ? "No test suites available."
              : "You haven't created any test suites yet."}
          </p>
          <p className="text-gray-500 text-sm mt-2">
            Click "Manual Entry" or "Import CSV" to create your first suite.
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-4">
          {suites.map((suite) => (
            <div key={suite.id} className="bg-white rounded-lg shadow p-6">
              <div className="flex justify-between items-start">
                <div className="flex-1">
                  <div className="flex items-center gap-3">
                    <h3 className="text-lg font-semibold text-gray-800">
                      {suite.name}
                    </h3>
                    {isAdmin && suite.createdBy && (
                      <span className="text-xs bg-gray-100 text-gray-700 px-2 py-1 rounded-full">
                        by {suite.createdBy.username} (
                        {formatRole(suite.createdBy.roles?.[0])})
                      </span>
                    )}
                  </div>
                  <p className="text-sm text-gray-600 mt-1">
                    {suite.description}
                  </p>
                  <div className="flex gap-4 mt-2 text-sm text-gray-500">
                    {/* FIX #3: Display test case count correctly - check multiple possible fields */}
                    <span>{suite.testCaseCount || suite.testCasesCount || suite.totalTestCases || suite.testCases?.length || 0} test cases</span>
                    <span
                      className={`font-semibold ${
                        suite.status === "PASSED" ||
                        suite.status === "COMPLETED"
                          ? "text-green-600"
                          : suite.status === "FAILED"
                          ? "text-red-600"
                          : "text-blue-600"
                      }`}
                    >
                      {suite.status}
                    </span>
                    {/* FIX #3: Format date correctly */}
                    <span>Created: {formatDate(suite.createdAt)}</span>
                  </div>
                </div>
                <div className="flex gap-2">
                  {/* FIX #2: Run button opens thread modal */}
                  <button
                    onClick={() => handleRunClick(suite)}
                    className="bg-green-600 text-white px-3 py-1 rounded hover:bg-green-700 transition flex items-center gap-1"
                    title="Execute suite with thread selection"
                  >
                    <span className="w-4 h-4">▶️</span> Run
                  </button>
                  <button
                    onClick={() => generateReport(suite.id)}
                    className="bg-blue-600 text-white px-3 py-1 rounded hover:bg-blue-700 transition flex items-center gap-1"
                    title="Generate and open report in new tab"
                  >
                    <span className="w-4 h-4">📄</span> Report
                  </button>
                  <button
                    onClick={() => exportCSV(suite.id)}
                    className="bg-purple-600 text-white px-3 py-1 rounded hover:bg-purple-700 transition flex items-center gap-1"
                    title="Export to CSV"
                  >
                    <span className="w-4 h-4">⬇️</span> CSV
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* FIX #2: Thread Selection Modal */}
      {showThreadModal && selectedSuite && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-lg shadow-xl p-6 w-full max-w-md">
            <h3 className="text-xl font-bold mb-4">
              Execute Suite: {selectedSuite.name}
            </h3>

            <div className="mb-6">
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Parallel Threads (1-8)
              </label>
              <input
                type="number"
                min="1"
                max="8"
                value={parallelThreads}
                onChange={(e) =>
                  setParallelThreads(
                    Math.min(8, Math.max(1, parseInt(e.target.value) || 1))
                  )
                }
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 text-lg"
              />

              {/* Quick select buttons */}
              <div className="flex gap-2 mt-3">
                {[1, 2, 4, 8].map((n) => (
                  <button
                    key={n}
                    type="button"
                    onClick={() => setParallelThreads(n)}
                    className={`px-4 py-2 rounded-lg font-medium ${
                      parallelThreads === n
                        ? "bg-blue-600 text-white"
                        : "bg-gray-200 text-gray-700 hover:bg-gray-300"
                    }`}
                  >
                    {n}
                  </button>
                ))}
              </div>

              <p className="text-xs text-gray-500 mt-3">
                <strong>1</strong> = Sequential execution (default)
                <br />
                <strong>2-4</strong> = Standard parallel execution
                <br />
                <strong>5-8</strong> = High concurrency (for large suites)
              </p>
            </div>

            <div className="bg-gray-50 rounded-lg p-3 mb-4">
              <p className="text-sm text-gray-600">
                <strong>Suite:</strong> {selectedSuite.name}
                <br />
                <strong>Test Cases:</strong>{" "}
                {selectedSuite.testCaseCount || selectedSuite.testCasesCount || selectedSuite.totalTestCases || selectedSuite.testCases?.length || 0}
                <br />
                <strong>Mode:</strong>{" "}
                {parallelThreads === 1 ? "Sequential" : "Parallel"}
              </p>
            </div>

            <div className="flex gap-2">
              <button
                onClick={executeSuite}
                className="flex-1 bg-green-600 text-white py-2 rounded-lg hover:bg-green-700 transition font-medium"
              >
                ▶️ Execute ({parallelThreads} thread
                {parallelThreads > 1 ? "s" : ""})
              </button>
              <button
                onClick={() => setShowThreadModal(false)}
                className="flex-1 bg-gray-300 text-gray-700 py-2 rounded-lg hover:bg-gray-400 transition"
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}

      {showImport && (
        <ImportCSVModal
          onClose={() => setShowImport(false)}
          onSuccess={fetchSuites}
        />
      )}
      {showManual && (
        <ManualEntryModal
          onClose={() => setShowManual(false)}
          onSuccess={fetchSuites}
        />
      )}
    </div>
  );
};

export default TestSuites;