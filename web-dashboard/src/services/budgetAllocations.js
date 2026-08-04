// Local-only ward budget allocations.
//
// There is no backend/database budget feature (no table, no API route) —
// this persists allocations in the browser's localStorage only, scoped to
// the existing per-ward budget figures already computed in data/gvmcWards.js
// (deterministic pseudo-random reference numbers, not live financial data).
// If a real backend budget service is added later, this module is the only
// place that needs to change.

const STORAGE_KEY = 'gvmc_budget_allocations';

export function getAllocations() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? JSON.parse(raw) : [];
  } catch {
    return [];
  }
}

export function addAllocation({ wardNo, amount, remarks }) {
  const allocations = getAllocations();
  const record = {
    id: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
    wardNo,
    amount,
    remarks: remarks || '',
    createdAt: new Date().toISOString()
  };
  const next = [...allocations, record];
  localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
  return record;
}

export function getTotalAllocated() {
  return getAllocations().reduce((sum, a) => sum + (Number(a.amount) || 0), 0);
}
