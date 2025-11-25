import React, { useState, useEffect } from "react";
import { useAuth } from "../../contexts/AuthContext";
import MetricRow from "../common/MetricRow";

const API_BASE = "http://localhost:8080/api";

const SettingsPage = () => {
  const { user, token, logout } = useAuth();
  const [users, setUsers] = useState([]);
  const [expandedUser, setExpandedUser] = useState(null);
  const [loading, setLoading] = useState(false);
  const isAdmin = user?.roles?.includes("ROLE_ADMIN");

  useEffect(() => {
    if (isAdmin) {
      fetchUsers();
    }
  }, [isAdmin, token]);

  // FIX #6: Fetch all users for admin
  const fetchUsers = async () => {
    setLoading(true);
    try {
      const res = await fetch(`${API_BASE}/users`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (res.ok) {
        const data = await res.json();
        setUsers(data);
      }
    } catch (error) {
      console.error("Failed to fetch users:", error);
    } finally {
      setLoading(false);
    }
  };

  // FIX #6: Promote user to admin
  const promoteUser = async (userId) => {
    try {
      const res = await fetch(`${API_BASE}/users/${userId}/promote`, {
        method: "PUT",
        headers: { Authorization: `Bearer ${token}` },
      });
      if (res.ok) {
        alert("User promoted to admin successfully!");
        fetchUsers();
      } else {
        const error = await res.json();
        alert("Failed to promote user: " + (error.message || error.error));
      }
    } catch (error) {
      alert("Failed to promote user: " + error.message);
    }
  };

  // FIX #6: Demote admin to user
  const demoteUser = async (userId) => {
    try {
      const res = await fetch(`${API_BASE}/users/${userId}/demote`, {
        method: "PUT",
        headers: { Authorization: `Bearer ${token}` },
      });
      if (res.ok) {
        alert("User demoted successfully!");
        fetchUsers();
      } else {
        const error = await res.json();
        alert("Failed to demote user: " + (error.message || error.error));
      }
    } catch (error) {
      alert("Failed to demote user: " + error.message);
    }
  };

  const formatRole = (roles) => {
    if (!roles || roles.length === 0) return "USER";
    return roles.map((r) => r.replace("ROLE_", "")).join(", ");
  };

  return (
    <div className="space-y-6">
      <h2 className="text-2xl font-bold text-gray-800">Settings</h2>

      {/* User Information */}
      <div className="bg-white rounded-lg shadow p-6">
        <h3 className="text-lg font-semibold mb-4">Your Information</h3>
        <div className="space-y-3">
          <MetricRow label="Username" value={user?.username} />
          <MetricRow label="Email" value={user?.email} />
          <MetricRow label="Roles" value={formatRole(user?.roles)} />
        </div>
      </div>

      {/* FIX #6: User Management (Admin Only) */}
      {isAdmin && (
        <div className="bg-white rounded-lg shadow p-6">
          <h3 className="text-lg font-semibold mb-4 flex items-center gap-2">
            👥 User Management
            <span className="text-sm font-normal text-gray-500">
              ({users.length} users)
            </span>
          </h3>

          {loading ? (
            <div className="text-center py-4 text-gray-500">Loading users...</div>
          ) : users.length === 0 ? (
            <div className="text-center py-4 text-gray-500">No users found.</div>
          ) : (
            <div className="space-y-2">
              {users.map((u) => (
                <div
                  key={u.id}
                  className="border border-gray-200 rounded-lg overflow-hidden"
                >
                  {/* User Header - Clickable */}
                  <div
                    className="flex justify-between items-center p-4 cursor-pointer hover:bg-gray-50 transition"
                    onClick={() =>
                      setExpandedUser(expandedUser === u.id ? null : u.id)
                    }
                  >
                    <div className="flex items-center gap-3">
                      {/* Avatar */}
                      <div className="w-10 h-10 bg-blue-100 text-blue-600 rounded-full flex items-center justify-center font-semibold">
                        {u.username?.[0]?.toUpperCase() || "?"}
                      </div>
                      <div>
                        <p className="font-medium flex items-center gap-2">
                          {u.username}
                          {u.roles?.includes("ROLE_ADMIN") && (
                            <span className="px-2 py-0.5 text-xs bg-purple-100 text-purple-800 rounded-full">
                              ADMIN
                            </span>
                          )}
                          {u.id === user?.id && (
                            <span className="px-2 py-0.5 text-xs bg-blue-100 text-blue-800 rounded-full">
                              YOU
                            </span>
                          )}
                        </p>
                        <p className="text-sm text-gray-600">{u.email}</p>
                      </div>
                    </div>
                    <span className="text-gray-400">
                      {expandedUser === u.id ? "▲" : "▼"}
                    </span>
                  </div>

                  {/* User Details - Expanded */}
                  {expandedUser === u.id && (
                    <div className="p-4 border-t bg-gray-50">
                      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-4">
                        <div>
                          <p className="text-xs text-gray-500 uppercase">
                            User ID
                          </p>
                          <p className="font-medium">{u.id}</p>
                        </div>
                        <div>
                          <p className="text-xs text-gray-500 uppercase">
                            Status
                          </p>
                          <p className="font-medium">
                            {u.enabled !== false ? (
                              <span className="text-green-600">✅ Active</span>
                            ) : (
                              <span className="text-red-600">❌ Inactive</span>
                            )}
                          </p>
                        </div>
                        <div>
                          <p className="text-xs text-gray-500 uppercase">
                            Roles
                          </p>
                          <p className="font-medium">{formatRole(u.roles)}</p>
                        </div>
                        <div>
                          <p className="text-xs text-gray-500 uppercase">
                            Test Executions
                          </p>
                          <p className="font-medium">
                            {u.testExecutions || 0}
                          </p>
                        </div>
                      </div>

                      {/* FIX #6: Promote/Demote Actions */}
                      {u.id !== user?.id && (
                        <div className="flex gap-2 pt-3 border-t border-gray-200">
                          {u.roles?.includes("ROLE_ADMIN") ? (
                            <button
                              onClick={(e) => {
                                e.stopPropagation();
                                if (
                                  window.confirm(
                                    `Are you sure you want to demote ${u.username} to regular user?`
                                  )
                                ) {
                                  demoteUser(u.id);
                                }
                              }}
                              className="px-4 py-2 bg-orange-600 text-white rounded-lg hover:bg-orange-700 transition text-sm font-medium flex items-center gap-2"
                            >
                              ⬇️ Demote to User
                            </button>
                          ) : (
                            <button
                              onClick={(e) => {
                                e.stopPropagation();
                                if (
                                  window.confirm(
                                    `Are you sure you want to promote ${u.username} to admin?`
                                  )
                                ) {
                                  promoteUser(u.id);
                                }
                              }}
                              className="px-4 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700 transition text-sm font-medium flex items-center gap-2"
                            >
                              ⬆️ Promote to Admin
                            </button>
                          )}
                        </div>
                      )}

                      {u.id === user?.id && (
                        <p className="text-sm text-gray-500 pt-3 border-t border-gray-200">
                          ℹ️ You cannot modify your own role.
                        </p>
                      )}
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Account Actions */}
      <div className="bg-white rounded-lg shadow p-6">
        <h3 className="text-lg font-semibold mb-4">Account Actions</h3>
        <button
          onClick={logout}
          className="bg-red-600 text-white px-6 py-2 rounded hover:bg-red-700 transition flex items-center gap-2"
        >
          <span className="w-4 h-4">🚪</span> Logout
        </button>
      </div>
    </div>
  );
};

export default SettingsPage;