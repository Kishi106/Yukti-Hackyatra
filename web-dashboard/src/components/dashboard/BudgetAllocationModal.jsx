import React, { useState } from 'react';
import { X } from 'lucide-react';
import { WARDS } from '../../data/gvmcWards';
import { addAllocation } from '../../services/budgetAllocations';
import { INK, INK_LO, LINE, GOV, inter } from '../../theme';

const fieldStyle = {
  ...inter,
  width: '100%',
  padding: '9px 11px',
  fontSize: 13,
  borderRadius: 8,
  border: `1px solid ${LINE}`,
  color: INK,
  boxSizing: 'border-box'
};

export default function BudgetAllocationModal({ onClose, onAllocated }) {
  const [wardNo, setWardNo] = useState(WARDS[0]?.wardNo ?? '');
  const [amount, setAmount] = useState('');
  const [remarks, setRemarks] = useState('');
  const [error, setError] = useState('');

  function handleSubmit(e) {
    e.preventDefault();
    const parsedAmount = Number(amount);
    if (!wardNo) {
      setError('Select a ward.');
      return;
    }
    if (!amount || Number.isNaN(parsedAmount) || parsedAmount <= 0) {
      setError('Enter a valid budget amount.');
      return;
    }
    addAllocation({ wardNo: Number(wardNo), amount: parsedAmount, remarks });
    onAllocated();
  }

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        background: 'rgba(18,33,47,0.45)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 1200,
        padding: 16
      }}
      onClick={onClose}
    >
      <div
        style={{
          width: '100%',
          maxWidth: 380,
          background: 'white',
          borderRadius: 14,
          border: `1px solid ${LINE}`,
          boxShadow: '0 12px 32px rgba(18,33,47,0.18)',
          padding: 20
        }}
        onClick={(e) => e.stopPropagation()}
      >
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16 }}>
          <p style={{ ...inter, fontSize: 15, fontWeight: 700, color: INK, margin: 0 }}>Allocate Budget</p>
          <button
            type="button"
            onClick={onClose}
            style={{ background: 'none', border: 'none', cursor: 'pointer', color: INK_LO, padding: 4 }}
          >
            <X size={16} />
          </button>
        </div>

        <form onSubmit={handleSubmit}>
          <label style={{ display: 'block', marginBottom: 12 }}>
            <span style={{ ...inter, fontSize: 11.5, fontWeight: 600, color: INK_LO, display: 'block', marginBottom: 5 }}>
              Ward
            </span>
            <select value={wardNo} onChange={(e) => setWardNo(e.target.value)} style={fieldStyle}>
              {WARDS.map((w) => (
                <option key={w.wardNo} value={w.wardNo}>
                  Ward {w.wardNo} — {w.zoneArea}
                </option>
              ))}
            </select>
          </label>

          <label style={{ display: 'block', marginBottom: 12 }}>
            <span style={{ ...inter, fontSize: 11.5, fontWeight: 600, color: INK_LO, display: 'block', marginBottom: 5 }}>
              Budget amount (₹ lakh)
            </span>
            <input
              type="number"
              min="0"
              step="0.1"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              style={fieldStyle}
              placeholder="e.g. 5.0"
            />
          </label>

          <label style={{ display: 'block', marginBottom: 16 }}>
            <span style={{ ...inter, fontSize: 11.5, fontWeight: 600, color: INK_LO, display: 'block', marginBottom: 5 }}>
              Remarks (optional)
            </span>
            <textarea
              value={remarks}
              onChange={(e) => setRemarks(e.target.value)}
              style={{ ...fieldStyle, resize: 'vertical', minHeight: 60 }}
            />
          </label>

          {error && (
            <p style={{ ...inter, fontSize: 12, color: '#C0392B', margin: '0 0 12px' }}>{error}</p>
          )}

          <button
            type="submit"
            style={{
              ...inter,
              width: '100%',
              padding: '10px 0',
              borderRadius: 8,
              border: 'none',
              fontSize: 13.5,
              fontWeight: 600,
              color: 'white',
              background: GOV,
              cursor: 'pointer'
            }}
          >
            Submit
          </button>
        </form>
      </div>
    </div>
  );
}
