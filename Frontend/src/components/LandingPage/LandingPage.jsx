
import React, { useState, useEffect } from 'react';
import { LineChart, Line, BarChart, Bar, PieChart, Pie, Cell, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { useNavigate } from 'react-router-dom';

const LandingPage = ({  }) => {
  const [stats, setStats] = useState({
    lastRun: {
      status: 'SUCCESS',
      timestamp: new Date().toISOString(),
      duration: 245,
      environment: 'Production',
      passRate: 96.5
    },
    summary: {
      passed: 387,
      failed: 14,
      skipped: 3,
      stability: 98.2
    },
    trends: [
      { run: 'Run 20', passRate: 94.5, duration: 230 },
      { run: 'Run 21', passRate: 96.2, duration: 240 },
      { run: 'Run 22', passRate: 95.8, duration: 235 },
      { run: 'Run 23', passRate: 97.1, duration: 242 },
      { run: 'Run 24', passRate: 96.5, duration: 245 }
    ],
    failureReasons: [
      { reason: 'TimeoutException', count: 5 },
      { reason: 'NoSuchElementException', count: 4 },
      { reason: 'AssertionError', count: 3 },
      { reason: 'NetworkError', count: 2 }
    ],
    flakyTests: [
      { name: 'LoginFlowTest', flakiness: 15 },
      { name: 'CheckoutProcessTest', flakiness: 12 },
      { name: 'SearchFunctionalityTest', flakiness: 8 }
    ],
    coverage: {
      automated: 404,
      manual: 96,
      total: 500
    },
    platforms: [
      { name: 'Chrome', passRate: 97.2, icon: '🌐' },
      { name: 'Firefox', passRate: 96.8, icon: '🦊' },
      { name: 'Safari', passRate: 95.5, icon: '🧭' },
      { name: 'Mobile', passRate: 94.3, icon: '📱' }
    ]
  });
  const navigate = useNavigate();
  const handleLoginRedirect = () => {
  // This tells React Router to change the URL to /login
  navigate('/login');
};
  const pieData = [
    { name: 'Passed', value: stats.summary.passed, color: '#10b981' },
    { name: 'Failed', value: stats.summary.failed, color: '#ef4444' },
    { name: 'Skipped', value: stats.summary.skipped, color: '#f59e0b' }
  ];

  const automationCoverageData = [
    { name: 'Automated', value: stats.coverage.automated, color: '#3b82f6' },
    { name: 'Manual', value: stats.coverage.manual, color: '#94a3b8' }
  ];

  return (
    <div className="min-h-screen bg-linear-to-br from-slate-900 via-blue-900 to-slate-900">
      {/* Header */}
      <header className="bg-slate-900/50 backdrop-blur-lg border-b border-slate-700 sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-6 py-4 flex justify-between items-center">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-linear-to-br from-blue-500 to-purple-600 rounded-lg flex items-center justify-center">
              <span className="text-white text-xl font-bold">TF</span>
            </div>
            <div>
              <h1 className="text-xl font-bold text-white">Test Framework</h1>
              <p className="text-xs text-slate-400">Automated Testing Dashboard</p>
            </div>
          </div>
          <button
            onClick={handleLoginRedirect}
            className="bg-blue-600 hover:bg-blue-700 text-white px-6 py-2 rounded-lg font-medium transition-all transform hover:scale-105 shadow-lg hover:shadow-blue-500/50"
          >
            Sign In
          </button>
        </div>
      </header>

      <div className="max-w-360 mx-auto px-6 py-8">
        {/* Hero Section - At-a-Glance Summary */}
        <section className="mb-8">
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            {/* Last Run Status */}
            <div className="lg:col-span-2 bg-linear-to-br from-slate-800 to-slate-900 rounded-2xl p-6 border border-slate-700 shadow-2xl">
              <div className="flex justify-between items-start mb-4">
                <div>
                  <h2 className="text-2xl font-bold text-white mb-2">Latest Build Status</h2>
                  <p className="text-slate-400 text-sm">Real-time test execution results</p>
                </div>
                <div className={`px-4 py-2 rounded-full font-bold text-sm ${
                  stats.lastRun.status === 'SUCCESS' 
                    ? 'bg-green-500/20 text-green-400 border border-green-500/50' 
                    : 'bg-red-500/20 text-red-400 border border-red-500/50'
                }`}>
                  {stats.lastRun.status === 'SUCCESS' ? '✓ PASSING' : '✗ FAILING'}
                </div>
              </div>

              <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mt-6">
                <div className="bg-slate-700/50 rounded-xl p-4 border border-slate-600">
                  <div className="text-slate-400 text-xs mb-1">Pass Rate</div>
                  <div className="text-3xl font-bold text-green-400">{stats.lastRun.passRate}%</div>
                </div>
                <div className="bg-slate-700/50 rounded-xl p-4 border border-slate-600">
                  <div className="text-slate-400 text-xs mb-1">Duration</div>
                  <div className="text-3xl font-bold text-blue-400">{stats.lastRun.duration}s</div>
                </div>
                <div className="bg-slate-700/50 rounded-xl p-4 border border-slate-600">
                  <div className="text-slate-400 text-xs mb-1">Environment</div>
                  <div className="text-xl font-bold text-purple-400">{stats.lastRun.environment}</div>
                </div>
                <div className="bg-slate-700/50 rounded-xl p-4 border border-slate-600">
                  <div className="text-slate-400 text-xs mb-1">Stability</div>
                  <div className="text-3xl font-bold text-emerald-400">{stats.summary.stability}%</div>
                </div>
              </div>

              <div className="mt-4 text-xs text-slate-500">
                {new Date(stats.lastRun.timestamp).toLocaleString()} 
              </div>
            </div>

            {/* Success Rate Pie Chart */}
            <div className="bg-linear-to-br from-slate-800 to-slate-900 rounded-2xl p-6 border border-slate-700 shadow-2xl">
              <h3 className="text-lg font-bold text-white mb-4">Test Results</h3>
              <ResponsiveContainer width="100%" height={200}>
                <PieChart>
                  <Pie
                    data={pieData}
                    cx="50%"
                    cy="50%"
                    innerRadius={50}
                    outerRadius={80}
                    paddingAngle={2}
                    dataKey="value"
                  >
                    {pieData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.color} />
                    ))}
                  </Pie>
                  <Tooltip contentStyle={{ backgroundColor: '#1e293b', border: '1px solid #475569' }} />
                </PieChart>
              </ResponsiveContainer>
              <div className="grid grid-cols-3 gap-2 mt-4">
                {pieData.map((item) => (
                  <div key={item.name} className="text-center">
                    <div className="text-2xl font-bold" style={{ color: item.color }}>
                      {item.value}
                    </div>
                    <div className="text-xs text-slate-400">{item.name}</div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </section>

        {/* Trends Section */}
        <section className="mb-8">
          <h2 className="text-2xl font-bold text-white mb-4">📈 Trends & Historical Data</h2>
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Pass/Fail Trend */}
            <div className="bg-linear-to-br from-slate-800 to-slate-900 rounded-2xl p-6 border border-slate-700 shadow-2xl">
              <h3 className="text-lg font-semibold text-white mb-4">Pass Rate Trend (Last 5 Runs)</h3>
              <ResponsiveContainer width="100%" height={250}>
                <LineChart data={stats.trends}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#475569" />
                  <XAxis dataKey="run" stroke="#94a3b8" />
                  <YAxis stroke="#94a3b8" domain={[90, 100]} />
                  <Tooltip contentStyle={{ backgroundColor: '#1e293b', border: '1px solid #475569' }} />
                  <Legend />
                  <Line type="monotone" dataKey="passRate" stroke="#10b981" strokeWidth={3} name="Pass Rate %" />
                </LineChart>
              </ResponsiveContainer>
            </div>

            {/* Execution Duration */}
            <div className="bg-linear-to-br from-slate-800 to-slate-900 rounded-2xl p-6 border border-slate-700 shadow-2xl">
              <h3 className="text-lg font-semibold text-white mb-4">Execution Duration (Seconds)</h3>
              <ResponsiveContainer width="100%" height={250}>
                <BarChart data={stats.trends}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#475569" />
                  <XAxis dataKey="run" stroke="#94a3b8" />
                  <YAxis stroke="#94a3b8" />
                  <Tooltip contentStyle={{ backgroundColor: '#1e293b', border: '1px solid #475569' }} />
                  <Legend />
                  <Bar dataKey="duration" fill="#3b82f6" name="Duration (s)" />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>
        </section>

        {/* Failure Analysis */}
        <section className="mb-8">
          <h2 className="text-2xl font-bold text-white mb-4">🔍 Failure Analysis</h2>
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Top Failure Reasons */}
            <div className="bg-linear-to-br from-slate-800 to-slate-900 rounded-2xl p-6 border border-slate-700 shadow-2xl">
              <h3 className="text-lg font-semibold text-white mb-4">Top Failure Reasons</h3>
              <ResponsiveContainer width="100%" height={250}>
                <BarChart data={stats.failureReasons} layout="vertical">
                  <CartesianGrid strokeDasharray="3 3" stroke="#475569" />
                  <XAxis type="number" stroke="#94a3b8" />
                  <YAxis type="category" dataKey="reason" stroke="#94a3b8" width={150} />
                  <Tooltip contentStyle={{ backgroundColor: '#1e293b', border: '1px solid #475569' }} />
                  <Bar dataKey="count" fill="#ef4444" />
                </BarChart>
              </ResponsiveContainer>
            </div>

            {/* Flaky Tests */}
            <div className="bg-linear-to-br from-slate-800 to-slate-900 rounded-2xl p-6 border border-slate-700 shadow-2xl">
              <h3 className="text-lg font-semibold text-white mb-4">⚠️ Top Flaky Tests</h3>
              <div className="space-y-3">
                {stats.flakyTests.map((test, idx) => (
                  <div key={idx} className="bg-slate-700/50 rounded-lg p-4 border border-slate-600">
                    <div className="flex justify-between items-center mb-2">
                      <span className="text-white font-medium">{test.name}</span>
                      <span className="text-yellow-400 font-bold">{test.flakiness}%</span>
                    </div>
                    <div className="h-2 bg-slate-600 rounded-full overflow-hidden">
                      <div 
                        className="h-full bg-linear-to-r from-yellow-500 to-red-500"
                        style={{ width: `${test.flakiness}%` }}
                      />
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </section>

        {/* Infrastructure & Coverage */}
        <section className="mb-8">
          <h2 className="text-2xl font-bold text-white mb-4">⚙️ Infrastructure & Coverage</h2>
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Platform Coverage */}
            <div className="bg-linear-to-br from-slate-800 to-slate-900 rounded-2xl p-6 border border-slate-700 shadow-2xl">
              <h3 className="text-lg font-semibold text-white mb-4">Browser/Device Coverage</h3>
              <div className="grid grid-cols-2 gap-4">
                {stats.platforms.map((platform, idx) => (
                  <div key={idx} className="bg-slate-700/50 rounded-xl p-4 border border-slate-600">
                    <div className="flex items-center gap-2 mb-2">
                      <span className="text-2xl">{platform.icon}</span>
                      <span className="text-white font-medium">{platform.name}</span>
                    </div>
                    <div className="text-2xl font-bold text-green-400">{platform.passRate}%</div>
                    <div className="text-xs text-slate-400">Pass Rate</div>
                  </div>
                ))}
              </div>
            </div>

            {/* Test Inventory */}
            <div className="bg-linear-to-br from-slate-800 to-slate-900 rounded-2xl p-6 border border-slate-700 shadow-2xl">
              <h3 className="text-lg font-semibold text-white mb-4">Test Inventory</h3>
              <ResponsiveContainer width="100%" height={200}>
                <PieChart>
                  <Pie
                    data={automationCoverageData}
                    cx="50%"
                    cy="50%"
                    innerRadius={50}
                    outerRadius={80}
                    paddingAngle={2}
                    dataKey="value"
                  >
                    {automationCoverageData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.color} />
                    ))}
                  </Pie>
                  <Tooltip contentStyle={{ backgroundColor: '#1e293b', border: '1px solid #475569' }} />
                </PieChart>
              </ResponsiveContainer>
              <div className="grid grid-cols-2 gap-4 mt-4">
                <div className="text-center">
                  <div className="text-3xl font-bold text-blue-400">{stats.coverage.automated}</div>
                  <div className="text-sm text-slate-400">Automated Tests</div>
                </div>
                <div className="text-center">
                  <div className="text-3xl font-bold text-slate-400">{stats.coverage.manual}</div>
                  <div className="text-sm text-slate-400">Manual Tests</div>
                </div>
              </div>
              <div className="mt-4 text-center">
                <div className="text-lg font-bold text-white">
                  {((stats.coverage.automated / stats.coverage.total) * 100).toFixed(1)}%
                </div>
                <div className="text-xs text-slate-400">Automation Coverage</div>
              </div>
            </div>
          </div>
        </section>

        {/* Quick Actions */}
        <section>
          <h2 className="text-2xl font-bold text-white mb-4">🚀 Quick Actions</h2>
          <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
            <button className="bg-linear-to-br from-green-600 to-green-700 hover:from-green-500 hover:to-green-600 text-white p-6 rounded-xl font-semibold transition-all transform hover:scale-105 shadow-lg">
              ▶️ Run Suite Now
            </button>
            <button className="bg-linear-to-br from-blue-600 to-blue-700 hover:from-blue-500 hover:to-blue-600 text-white p-6 rounded-xl font-semibold transition-all transform hover:scale-105 shadow-lg">
              📚 Documentation
            </button>
            <button className="bg-linear-to-br from-purple-600 to-purple-700 hover:from-purple-500 hover:to-purple-600 text-white p-6 rounded-xl font-semibold transition-all transform hover:scale-105 shadow-lg">
              📊 Detailed Reports
            </button>
            
          </div>
        </section>

        {/* Footer */}
        <footer className="mt-12 text-center text-slate-500 text-sm border-t border-slate-700 pt-6">
          <p>Test Framework v2.0 | Framework Version: 3.5.6 | Java 21 | Spring Boot | React</p>
          <p className="mt-2">Environment: Production • Last Updated: {new Date().toLocaleString()}</p>
        </footer>
      </div>
    </div>
  );
};

export default LandingPage;