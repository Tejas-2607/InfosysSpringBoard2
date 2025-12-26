import React from "react";
// 1. Ensure BrowserRouter is imported and aliased as Router
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom'; 
import { AuthProvider, useAuth } from "./contexts/AuthContext";
import LoginPage from "./components/auth/LoginPage";
import Layout from "./components/layout/Layout";
import LandingPage from "./components/LandingPage/LandingPage";

const AppContent = () => {
  const { user } = useAuth();

  return (
    <Routes>
      {/* 2. Route logic: Show LandingPage if no user, otherwise Layout */}
      <Route path="/" element={!user ? <LandingPage /> : <Layout />} />
      <Route path="/login" element={<LoginPage />} />
    </Routes>
  );
};

const App = () => (
  <AuthProvider>
    {/* 3. The Router MUST wrap AppContent to provide navigation context */}
    <Router>
      <AppContent />
    </Router>
  </AuthProvider>
);

export default App;