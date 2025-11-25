import React, { createContext, useContext, useState, useEffect } from "react";

const API_BASE = "http://localhost:8080/api";

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(localStorage.getItem("token"));

  useEffect(() => {
    if (token) {
      fetch(`${API_BASE}/auth/me`, {
        headers: { Authorization: `Bearer ${token}` },
      })
        .then((res) => res.json())
        .then((data) => setUser(data))
        .catch(() => {
          localStorage.removeItem("token");
          setToken(null);
        });
    }
  }, [token]);

  const login = async (username, password) => {
    const res = await fetch(`${API_BASE}/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username, password }),
    });

    if (!res.ok) throw new Error("Login failed");

    const data = await res.json();
    localStorage.setItem("token", data.token);
    setToken(data.token);
    setUser({
      id: data.userId,
      username: data.username,
      email: data.email,
      roles: data.roles,
    });
    return data;
  };

  const register = async (username, password, email) => {
    const res = await fetch(`${API_BASE}/auth/register`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username, password, email }),
    });

    if (!res.ok) {
      const error = await res.json();
      throw new Error(error.error || "Registration failed");
    }

    return res.json();
  };

  const logout = () => {
    localStorage.removeItem("token");
    setToken(null);
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, token, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);
