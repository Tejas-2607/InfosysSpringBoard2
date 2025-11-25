import React, { useState } from "react";
import { useAuth } from "../../contexts/AuthContext";

const API_BASE = "http://localhost:8080/api";

const ImportCSVModal = ({ onClose, onSuccess }) => {
  const [file, setFile] = useState(null);
  const [suiteName, setSuiteName] = useState("");
  const [description, setDescription] = useState("");
  const [loading, setLoading] = useState(false);
  const { token } = useAuth();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    const formData = new FormData();
    formData.append("csvFile", file);
    formData.append("suiteName", suiteName);
    formData.append("description", description);
    try {
      const res = await fetch(`${API_BASE}/suites/import-csv`, {
        method: "POST",
        headers: { Authorization: `Bearer ${token}` },
        body: formData,
      });
      if (!res.ok) throw new Error("Import failed");
      alert("Suite imported successfully!");
      onSuccess();
      onClose();
    } catch (error) {
      alert("Failed to import: " + error.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
      <div className="bg-white rounded-lg shadow-xl p-6 w-full max-w-md">
        <h3 className="text-xl font-bold mb-4">Import Test Suite from CSV</h3>
        <form onSubmit={handleSubmit} className="space-y-4">
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
            <textarea
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
              rows="3"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              CSV File
            </label>
            <input
              type="file"
              accept=".csv"
              required
              className="w-full px-4 py-2 border border-gray-300 rounded-lg"
              onChange={(e) => setFile(e.target.files[0])}
            />
            <p className="text-xs text-gray-500 mt-1">
              Expected columns: testCaseId, testName, testType, urlEndpoint,
              httpMethodAction, locatorType, locatorValue, inputData,
              expectedResult, priority, run, description
            </p>
          </div>
          <div className="flex gap-2">
            <button
              type="submit"
              disabled={loading}
              className="flex-1 bg-blue-600 text-white py-2 rounded hover:bg-blue-700 transition disabled:opacity-50"
            >
              {loading ? "Importing..." : "Import"}
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

export default ImportCSVModal;
