import React, { useCallback, useEffect, useMemo, useState } from 'react';
import Header from './dashboard/Header';
import Sidebar from './dashboard/Sidebar';
import StatusOverview from './dashboard/StatusOverview';
import StatCards, { Layers, Gauge } from './dashboard/StatCards';
import BudgetCard from './dashboard/BudgetCard';
import ResolutionChart from './dashboard/ResolutionChart';
import WardDensityMap from './dashboard/WardDensityMap';
import ReportsView from './dashboard/ReportsView';
import WardsView from './dashboard/WardsView';
import AnalyticsView from './dashboard/AnalyticsView';
import MapView from './MapView';
import { getPotholes, updatePotholeStatus } from '../services/api';
import { BG } from '../theme';

const REFRESH_INTERVAL_MS = 15000;

export default function Dashboard({ official, wardNo, wardLabel, onChangeWard, onLogout }) {
  const [potholes, setPotholes] = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  const [focusPothole, setFocusPothole] = useState(null);
  const [activeView, setActiveView] = useState('dashboard');

  const load = useCallback(async () => {
    try {
      const data = await getPotholes(wardNo != null ? { ward_no: wardNo } : {});
      setPotholes(data);
    } catch (err) {
      console.error('Failed to load potholes', err);
    }
  }, [wardNo]);

  useEffect(() => {
    load();
    const interval = setInterval(load, REFRESH_INTERVAL_MS);
    return () => clearInterval(interval);
  }, [load]);

  async function handleStatusChange(id, newStatus) {
    try {
      await updatePotholeStatus(id, newStatus);
      await load();
    } catch (err) {
      console.error('Failed to update pothole status', err);
    }
  }

  // Always a fresh object so re-locating the same pothole re-triggers the
  // main map's flyTo/openPopup effect even if nothing else changed.
  function handleLocate(pothole) {
    setSelectedId(pothole.id);
    setFocusPothole({ ...pothole });
  }

  const wardsCovered = useMemo(() => new Set(potholes.map((p) => p.ward_no).filter((w) => w != null)).size, [potholes]);
  const avgConfidence = useMemo(() => {
    if (!potholes.length) return 0;
    const sum = potholes.reduce((s, p) => s + (p.confidence_score || 0), 0);
    return Math.round(sum / potholes.length);
  }, [potholes]);

  const secondaryStat =
    official?.role === 'field_officer'
      ? { icon: Gauge, label: 'Avg. Confidence Score', value: `${avgConfidence}%` }
      : { icon: Layers, label: 'Wards Covered', value: wardsCovered };

  return (
    <div style={{ minHeight: '100vh', width: '100%', background: BG, display: 'flex', flexDirection: 'column' }}>
      <Header
        official={official}
        wardLabel={wardLabel}
        onChangeWard={onChangeWard}
        onLogout={onLogout}
        activeView={activeView}
        onChangeView={setActiveView}
      />

      {activeView === 'dashboard' && (
        <>
          <main
            style={{
              height: 'min(72vh, 700px)',
              padding: '16px 16px 0',
              display: 'grid',
              gridTemplateColumns: 'minmax(260px, 320px) minmax(0, 1fr) minmax(260px, 320px)',
              gap: 16
            }}
            className="dashboard-grid"
          >
            <div className="dash-sidebar" style={{ minHeight: 0, height: '100%' }}>
              <Sidebar potholes={potholes} onSelectPothole={handleLocate} onLocatePothole={handleLocate} />
            </div>

            <div className="dash-main" style={{ display: 'flex', flexDirection: 'column', gap: 16, minHeight: 0, height: '100%', overflowY: 'auto' }}>
              <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
                <div style={{ flex: '2 1 260px' }}>
                  <StatCards totalReports={potholes.length} secondary={secondaryStat} />
                </div>
                <div style={{ flex: '1 1 200px' }}>
                  <BudgetCard />
                </div>
              </div>
              <StatusOverview potholes={potholes} />
              <ResolutionChart potholes={potholes} />
            </div>

            <div className="dash-analytics" style={{ display: 'flex', flexDirection: 'column', gap: 16, minHeight: 0, height: '100%', overflowY: 'auto' }}>
              <WardDensityMap potholes={potholes} />
            </div>
          </main>

          <div className="dash-livemap" style={{ padding: 16 }}>
            <div style={{ height: 'min(60vh, 560px)', minHeight: 360 }}>
              <MapView
                potholes={potholes}
                selectedPothole={focusPothole}
                onStatusChange={handleStatusChange}
                selectedWardNo={wardNo}
              />
            </div>
          </div>
        </>
      )}

      {activeView === 'reports' && (
        <main style={{ flex: 1, padding: 16 }}>
          <ReportsView potholes={potholes} />
        </main>
      )}

      {activeView === 'wards' && (
        <main style={{ flex: 1, padding: 16 }}>
          <WardsView potholes={potholes} wardNo={wardNo} />
        </main>
      )}

      {activeView === 'analytics' && (
        <main style={{ flex: 1, padding: 16 }}>
          <AnalyticsView potholes={potholes} />
        </main>
      )}
    </div>
  );
}
