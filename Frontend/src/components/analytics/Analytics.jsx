import React, { useState, useEffect } from "react";
import { useAuth } from "../../contexts/AuthContext";
import StatCard from "../common/StatCard";
import MetricRow from "../common/MetricRow";

const API_BASE = "http://localhost:8080/api";

const Analytics = () => {
  const [analytics, setAnalytics] = useState(null);
  const [selectedSuite, setSelectedSuite] = useState(null);
  const [suites, setSuites] = useState([]);
  const [loading, setLoading] = useState(false);
  const { token, user } = useAuth();
  const isAdmin = user?.roles?.includes("ROLE_ADMIN");

  useEffect(() => {
    fetchSuites();
  }, [token]);

  // FIX #4: Fetch user-specific suites for non-admin users
  const fetchSuites = async () => {
    try {
      // Use different endpoint based on role
      const endpoint = isAdmin
        ? `${API_BASE}/suites`
        : `${API_BASE}/suites/my-suites`;

      const res = await fetch(endpoint, {
        headers: { Authorization: `Bearer ${token}` },
      });
      const data = await res.json();
      setSuites(Array.isArray(data) ? data : []);
    } catch (error) {
      console.error("Failed to fetch suites:", error);
      setSuites([]);
    }
  };

  const fetchAnalytics = async (suiteId) => {
    if (!suiteId) {
      setAnalytics(null);
      return;
    }

    setLoading(true);
    try {
      const res = await fetch(
        `${API_BASE}/suites/${suiteId}/analytics?days=7`,
        {
          headers: { Authorization: `Bearer ${token}` },
        }
      );

      if (!res.ok) {
        const error = await res.json();
        console.error("Analytics error:", error);
        alert(
          error.message ||
            "Failed to fetch analytics. Suite may not have been executed yet."
        );
        setAnalytics(null);
        return;
      }

      const data = await res.json();
      setAnalytics(data);
    } catch (error) {
      console.error("Failed to fetch analytics:", error);
      alert("Failed to fetch analytics. Please ensure the suite has been executed.");
      setAnalytics(null);
    } finally {
      setLoading(false);
    }
  };

  const handleSuiteChange = (e) => {
    const suiteId = e.target.value;
    setSelectedSuite(suiteId);
    if (suiteId) {
      fetchAnalytics(suiteId);
    } else {
      setAnalytics(null);
    }
  };

  return (
    <div className="space-y-6">
      <h2 className="text-2xl font-bold text-gray-800">
        Analytics {!isAdmin && "(My Tests Only)"}
      </h2>

      {/* FIX #4: Info banner for non-admin users */}
      {!isAdmin && (
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
          <p className="text-blue-800 text-sm">
            ℹ️ You are viewing analytics for test suites you created. Only your
            test executions are shown.
          </p>
        </div>
      )}

      {/* Suite Selection */}
      <div className="bg-white rounded-lg shadow p-6">
        <label className="block text-sm font-medium text-gray-700 mb-2">
          Select Test Suite
        </label>
        <select
          className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
          value={selectedSuite || ""}
          onChange={handleSuiteChange}
        >
          <option value="">Select a suite to view analytics...</option>
          {suites.map((suite) => (
            <option key={suite.id} value={suite.id}>
              {suite.name} ({suite.testCases?.length || 0} tests)
            </option>
          ))}
        </select>
        {suites.length === 0 && (
          <p className="text-sm text-gray-500 mt-2">
            {isAdmin
              ? "No test suites available."
              : "You haven't created any test suites yet."}
          </p>
        )}
      </div>

      {/* No Suite Selected */}
      {!selectedSuite && (
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-6 text-center">
          <p className="text-blue-800">
            📊 Select a test suite above to view its analytics
          </p>
        </div>
      )}

      {/* Loading State */}
      {selectedSuite && loading && (
        <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-6 text-center">
          <p className="text-yellow-800">⏳ Loading analytics for selected suite...</p>
        </div>
      )}

      {/* Analytics Display */}
      {analytics && selectedSuite && !loading && (
        <>
          {/* Summary Cards */}
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
            <StatCard
              title="Total Tests"
              value={analytics.summary?.totalTests || 0}
              icon={<span className="w-6 h-6">🔄</span>}
              color="bg-blue-500"
            />
            <StatCard
              title="Passed"
              value={analytics.summary?.passed || 0}
              icon={<span className="w-6 h-6">✅</span>}
              color="bg-green-500"
            />
            <StatCard
              title="Failed"
              value={analytics.summary?.failed || 0}
              icon={<span className="w-6 h-6">❌</span>}
              color="bg-red-500"
            />
            <StatCard
              title="Pass Rate"
              value={`${(analytics.summary?.passRate || 0).toFixed(1)}%`}
              icon={<span className="w-6 h-6">📈</span>}
              color="bg-purple-500"
            />
          </div>

          {/* Performance & Trends */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="bg-white rounded-lg shadow p-6">
              <h3 className="text-lg font-semibold mb-4">Performance Metrics</h3>
              <div className="space-y-3">
                <MetricRow
                  label="Avg Duration"
                  value={`${(analytics.summary?.avgDurationMs || 0).toFixed(0)}ms`}
                />
                <MetricRow
                  label="Stability"
                  value={`${(analytics.summary?.stability || 0).toFixed(1)}%`}
                />
                <MetricRow
                  label="Suite"
                  value={
                    suites.find((s) => s.id === parseInt(selectedSuite))?.name ||
                    "N/A"
                  }
                />
              </div>
            </div>

            <div className="bg-white rounded-lg shadow p-6">
              <h3 className="text-lg font-semibold mb-4">Trends (5 Days)</h3>
              {analytics.trends?.data && analytics.trends.data.length > 0 ? (
                <div className="space-y-2">
                  {analytics.trends.data.slice(0, 5).map((trend, idx) => (
                    <div key={idx} className="flex justify-between text-sm">
                      <span className="text-gray-600">{trend.date}</span>
                      <span className="font-semibold text-gray-800">
                        {(trend.passRate || 0).toFixed(1)}% ({trend.passed || 0}/
                        {trend.totalTests || 0})
                      </span>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="text-gray-500 text-sm">
                  No trend data available. Execute the suite multiple times to see
                  trends.
                </p>
              )}
            </div>
          </div>

          {/* Flaky Tests */}
          {analytics.flakyTests && analytics.flakyTests.count > 0 && (
            <div className="bg-white rounded-lg shadow p-6">
              <h3 className="text-lg font-semibold mb-4 flex items-center gap-2">
                <span className="w-5 h-5 text-yellow-600">⚠️</span>
                Flaky Tests ({analytics.flakyTests.count})
              </h3>
              <div className="space-y-2">
                {analytics.flakyTests.tests.slice(0, 5).map((test, idx) => (
                  <div
                    key={idx}
                    className="flex justify-between items-center p-3 bg-yellow-50 rounded"
                  >
                    <div>
                      <p className="font-medium text-gray-800">{test.testName}</p>
                      <p className="text-xs text-gray-600">
                        {test.totalRuns} runs | {test.retryCount} retries |{" "}
                        {test.passes} passed, {test.fails} failed
                      </p>
                    </div>
                    <span className="text-sm font-semibold text-yellow-700">
                      Score: {(test.flakyScore || 0).toFixed(1)}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* No Flaky Tests Message */}
          {analytics.flakyTests && analytics.flakyTests.count === 0 && (
            <div className="bg-green-50 border border-green-200 rounded-lg p-6 text-center">
              <p className="text-green-800">
                ✅ No flaky tests detected! Your tests are stable.
              </p>
            </div>
          )}
        </>
      )}
    </div>
  );
};

export default Analytics;