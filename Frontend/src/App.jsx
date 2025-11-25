import React from "react";
import { AuthProvider, useAuth } from "./contexts/AuthContext";
import LoginPage from "./components/auth/LoginPage";
import Layout from "./components/layout/Layout";

const AppContent = () => {
  const { user } = useAuth();
  if (!user) {
    return <LoginPage />;
  }
  return <Layout />;
};

const App = () => (
  <AuthProvider>
    <AppContent />
  </AuthProvider>
);

export default App;
