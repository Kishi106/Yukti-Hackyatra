import React, { useMemo, useState } from 'react';
import { Wallet } from 'lucide-react';
import { WARDS } from '../../data/gvmcWards';
import { getTotalAllocated } from '../../services/budgetAllocations';
import { INK, INK_LO, LINE, GOV, inter, mono } from '../../theme';
import BudgetAllocationModal from './BudgetAllocationModal';

function fmt(lakh) {
  return `₹${lakh.toFixed(1)}L`;
}

export default function BudgetCard() {
  const [showModal, setShowModal] = useState(false);
  // Bumped after a successful allocation to recompute totals below.
  const [version, setVersion] = useState(0);

  const totalBudget = useMemo(() => WARDS.reduce((sum, w) => sum + w.budget, 0), []);
  const allocatedBudget = useMemo(() => getTotalAllocated(), [version]);
  const remainingBudget = Math.max(0, totalBudget - allocatedBudget);

  return (
    <div
      style={{
        background: 'white',
        border: `1px solid ${LINE}`,
        borderRadius: 12,
        boxShadow: '0 1px 2px rgba(18,33,47,0.05)',
        padding: 14,
        flex: 1,
        minWidth: 0,
        display: 'flex',
        flexDirection: 'column'
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 10 }}>
        <div
          style={{
            width: 30,
            height: 30,
            borderRadius: 8,
            background: '#E7F0FA',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            flexShrink: 0
          }}
        >
          <Wallet size={15} color={GOV} />
        </div>
        <p style={{ ...inter, fontSize: 11, fontWeight: 600, color: INK_LO, margin: 0 }}>Budget Allotment</p>
      </div>

      <div style={{ display: 'flex', gap: 10, marginBottom: 12, flexWrap: 'wrap' }}>
        <div>
          <p style={{ ...mono, fontSize: 15, fontWeight: 600, color: INK, margin: 0 }}>{fmt(totalBudget)}</p>
          <p style={{ ...inter, fontSize: 9.5, color: INK_LO, margin: '1px 0 0' }}>Total</p>
        </div>
        <div>
          <p style={{ ...mono, fontSize: 15, fontWeight: 600, color: GOV, margin: 0 }}>{fmt(allocatedBudget)}</p>
          <p style={{ ...inter, fontSize: 9.5, color: INK_LO, margin: '1px 0 0' }}>Allocated</p>
        </div>
        <div>
          <p style={{ ...mono, fontSize: 15, fontWeight: 600, color: INK, margin: 0 }}>{fmt(remainingBudget)}</p>
          <p style={{ ...inter, fontSize: 9.5, color: INK_LO, margin: '1px 0 0' }}>Remaining</p>
        </div>
      </div>

      <button
        type="button"
        onClick={() => setShowModal(true)}
        style={{
          ...inter,
          marginTop: 'auto',
          padding: '8px 0',
          borderRadius: 8,
          border: `1px solid ${GOV}`,
          fontSize: 12,
          fontWeight: 600,
          color: GOV,
          background: 'white',
          cursor: 'pointer'
        }}
      >
        Allocate Budget
      </button>

      {showModal && (
        <BudgetAllocationModal
          onClose={() => setShowModal(false)}
          onAllocated={() => {
            setShowModal(false);
            setVersion((v) => v + 1);
          }}
        />
      )}
    </div>
  );
}
