import React from "react";

const StatCard = ({ title, value, icon, color }) => (
  <div className="bg-white rounded-lg shadow p-6">
    <div className="flex items-center justify-between">
      <div>
        <p className="text-sm text-gray-600">{title}</p>
        <p className="text-2xl font-bold text-gray-800 mt-1">{value}</p>
      </div>
      <div className={`${color} text-white p-3 rounded-lg`}>{icon}</div>
    </div>
  </div>
);

export default StatCard;
