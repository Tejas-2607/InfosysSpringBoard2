import React, { useState, useEffect } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import StatCard from '../common/StatCard';

const API_BASE = 'http://localhost:8080/api';

const Dashboard = () => {
  const [stats, setStats] = useState(null);
  const { token, user } = useAuth();
  const isAdmin = user?.roles?.includes('ROLE_ADMIN');

  useEffect(() => {
    // FIXED: Only fetch metrics if user is admin
    if (isAdmin) {
      fetch(`${API_BASE}/runs/metrics`, {
        headers: { 'Authorization': `Bearer ${token}` }
      })
        .then(res => {
          if (!res.ok) throw new Error('Failed to fetch metrics');
          return res.json();
        })
        .then(setStats)
        .catch(err => {
          console.error('Metrics error:', err);
          // Set default stats for graceful degradation
          setStats({
            total: 0,
            passed: 0,
            failed: 0,
            passRate: 0,
            avgDurationMs: 0,
            stability: 0
          });
        });
    } else {
      // FIXED: For users, fetch their personal stats
      fetch(`${API_BASE}/users/me/tests`, {
        headers: { 'Authorization': `Bearer ${token}` }
      })
        .then(res => res.json())
        .then(data => {
          setStats({
            total: data.totalTests || 0,
            passed: data.passedTests || 0,
            failed: data.failedTests || 0,
            passRate: data.passRate || 0,
            avgDurationMs: 0,
            stability: 0
          });
        })
        .catch(err => {
          console.error('User stats error:', err);
          setStats({
            total: 0,
            passed: 0,
            failed: 0,
            passRate: 0,
            avgDurationMs: 0,
            stability: 0
          });
        });
    }
  }, [token, isAdmin]);

  if (!stats) return <div className="text-center py-8">Loading...</div>;

  return (
    <div className="space-y-6">
      <h2 className="text-2xl font-bold text-gray-800">
        {isAdmin ? 'Admin Dashboard' : 'My Dashboard'}
      </h2>
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <StatCard
          title="Total Tests"
          value={stats.total}
          icon={<span className="w-6 h-6">📄</span>}
          color="bg-blue-500"
        />
        <StatCard
          title="Passed"
          value={stats.passed}
          icon={<span className="w-6 h-6">✅</span>}
          color="bg-green-500"
        />
        <StatCard
          title="Failed"
          value={stats.failed}
          icon={<span className="w-6 h-6">❌</span>}
          color="bg-red-500"
        />
        <StatCard
          title="Pass Rate"
          value={`${stats.passRate.toFixed(1)}%`}
          icon={<span className="w-6 h-6">📈</span>}
          color="bg-purple-500"
        />
      </div>
      
      {isAdmin && (
        <div className="bg-white rounded-lg shadow p-6">
          <h3 className="text-xl font-semibold mb-4 text-gray-800">Why Automated Testing Matters</h3>
          <div className="space-y-4 text-gray-700">
            <div>
              <h4 className="font-semibold text-lg mb-2 flex items-center gap-2">
                <span className="text-blue-600">🚀</span> Faster Release Cycles
              </h4>
              <p className="text-sm leading-relaxed">
                Automated testing dramatically reduces the time required for regression testing, allowing teams to deploy new features and updates more frequently. What once took days of manual testing can now be completed in minutes, enabling continuous integration and deployment practices.
              </p>
            </div>
            
            <div>
              <h4 className="font-semibold text-lg mb-2 flex items-center gap-2">
                <span className="text-green-600">💰</span> Cost Efficiency
              </h4>
              <p className="text-sm leading-relaxed">
                While there's an initial investment in setting up automated tests, the long-term savings are substantial. Automated tests can run thousands of times without additional cost, catch bugs early when they're cheaper to fix, and free up QA teams to focus on exploratory testing and complex scenarios that require human insight.
              </p>
            </div>
            
            <div>
              <h4 className="font-semibold text-lg mb-2 flex items-center gap-2">
                <span className="text-purple-600">🎯</span> Improved Accuracy & Consistency
              </h4>
              <p className="text-sm leading-relaxed">
                Human testers can make mistakes, especially when performing repetitive tasks. Automated tests execute the same steps precisely every time, eliminating human error and providing consistent, reliable results. This consistency is crucial for maintaining quality standards across multiple releases.
              </p>
            </div>
            
            <div>
              <h4 className="font-semibold text-lg mb-2 flex items-center gap-2">
                <span className="text-red-600">🛡️</span> Enhanced Code Quality & Confidence
              </h4>
              <p className="text-sm leading-relaxed">
                Comprehensive automated test coverage gives developers the confidence to refactor code and make improvements without fear of breaking existing functionality. This safety net encourages better code practices, reduces technical debt, and ultimately leads to more maintainable and robust applications.
              </p>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Dashboard;