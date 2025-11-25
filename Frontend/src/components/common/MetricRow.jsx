import React from "react";

const MetricRow = ({ label, value }) => (
  <div className="flex justify-between items-center">
    <span className="text-gray-600">{label}</span>
    <span className="font-semibold text-gray-800">{value}</span>
  </div>
);

export default MetricRow;
