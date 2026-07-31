import React, { useState, useEffect, useMemo } from "react";
import {
  MapPin, AlertTriangle, Users, ClipboardList, CheckCircle2, Clock,
  Wrench, LayoutDashboard, IndianRupee, Siren, ArrowUpDown, Radio, Layers,
} from "lucide-react";
import {
  ZONES, WARDS as WARD_SEED, zoneOf, zoneRollup,
} from "@/data/gvmcWards.js";
import { getPotholes, updatePotholeStatus } from "../services/api";

const REFRESH_INTERVAL_MS = 15000;

/* ---------------------------------------------------------
   API -> RoadWatchApp shape mapping

   The live backend returns { id, lat, lng, severity: "low"|"medium"|"high",
   source, photo_url, status: "new"|"in_progress"|"fixed", ward, created_at }.
   RoadWatchApp's FieldOfficer/AnalystDashboard views expect the shape the
   original mock POTHOLES array used: numeric wardNo, capitalized
   severity/status strings, ageDays, hazardScore, etc. This section converts
   one into the other without touching how the views consume that data.
--------------------------------------------------------- */
const SEVERITY_LABELS = { low: "Low", medium: "Medium", high: "High" };
const STATUS_LABELS = { new: "New", in_progress: "In Progress", fixed: "Resolved" };
const HAZARD_BASELINE = { High: 80, Medium: 55, Low: 20 };

function resolveWardNo(wardName, fallbackSeed) {
  if (wardName) {
    const needle = wardName.trim().toLowerCase();
    const match = WARD_SEED.find((w) =>
      w.localities.some((loc) => {
        const locLower = loc.toLowerCase();
        return locLower === needle || locLower.includes(needle) || needle.includes(locLower);
      })
    );
    if (match) return match.wardNo;
  }
  // Deterministic fallback so records without a recognizable ward name still
  // land in a stable ward bucket instead of being dropped.
  let hash = 0;
  const str = String(fallbackSeed ?? wardName ?? "");
  for (let i = 0; i < str.length; i++) hash = (hash * 31 + str.charCodeAt(i)) >>> 0;
  return (hash % 89) + 1;
}

function mapApiPothole(record) {
  const wardNo = resolveWardNo(record.ward, record.id);
  const zone = zoneOf(wardNo);
  const severity = SEVERITY_LABELS[record.severity] || "Medium";
  const status = STATUS_LABELS[record.status] || "New";
  const createdAt = record.created_at ? new Date(record.created_at) : new Date();
  const ageDays = Math.max(0, Math.floor((Date.now() - createdAt.getTime()) / 86400000));
  return {
    id: record.id,
    ward: wardNo,
    zone: zone ? zone.id : 1,
    locality: record.ward || `Ward ${wardNo}`,
    location: record.ward ? `${record.ward}, Ward ${wardNo}` : `Ward ${wardNo}`,
    severity,
    status,
    reportedDate: createdAt.toISOString().slice(0, 10),
    ageDays,
    hazardScore: HAZARD_BASELINE[severity] ?? 40,
  };
}

/* ---------------------------------------------------------
   LIGHT GOVERNMENT DESIGN TOKENS
--------------------------------------------------------- */
const INK = "#12212F";
const INK_LO = "#5B6B7C";
const LINE = "#D8E0EA";
const GOV = "#12518A";
const AMBER = "#B07800";
const GREEN = "#1B7F5A";
const RED = "#C0392B";

const FONT_LINK = "https://fonts.googleapis.com/css2?family=Barlow+Condensed:wght@600;700;800&family=Inter:wght@400;500;600&family=JetBrains+Mono:wght@500;600&display=swap";

function useFonts() {
  useEffect(() => {
    if (document.getElementById("pothole-fonts")) return;
    const link = document.createElement("link");
    link.id = "pothole-fonts";
    link.rel = "stylesheet";
    link.href = FONT_LINK;
    document.head.appendChild(link);
  }, []);
}

const display = { fontFamily: "'Barlow Condensed',sans-serif", fontWeight: 700 };
const mono = { fontFamily: "'JetBrains Mono',monospace" };
const inter = { fontFamily: "Inter, sans-serif" };

function CrackDivider({ className = "" }) {
  return (
    <svg viewBox="0 0 400 12" preserveAspectRatio="none" className={`w-full h-3 ${className}`}>
      <path
        d="M0,6 L28,6 L34,2 L42,9 L50,4 L58,6 L90,6 L96,10 L104,3 L112,6 L160,6 L168,2 L174,8 L182,5 L190,6 L240,6 L246,9 L254,2 L262,6 L310,6 L316,3 L324,9 L332,5 L340,6 L400,6"
        fill="none" stroke={LINE} strokeWidth="1.5"
      />
    </svg>
  );
}

function HazardScore({ score, size = 52 }) {
  const tier = score >= 70 ? RED : score >= 35 ? AMBER : GREEN;
  return (
    <div className="relative flex items-center justify-center shrink-0" style={{ width: size, height: size }}>
      <svg viewBox="0 0 100 100" width={size} height={size}>
        <polygon points="50,6 96,90 4,90" fill="#FFFFFF" stroke={tier} strokeWidth="6" strokeLinejoin="round" />
      </svg>
      <span className="absolute font-semibold"
        style={{ ...mono, color: tier, fontSize: size * 0.28, top: size * 0.42 }}>
        {score}
      </span>
    </div>
  );
}

function Pill({ children, tone = "muted" }) {
  const tones = {
    muted: "bg-[#EEF2F7] text-[#5B6B7C] border-[#D8E0EA]",
    verified: "bg-[#E6F4EE] text-[#166B4C] border-[#B9E0D0]",
    pending: "bg-[#FDF3DC] text-[#8A5D00] border-[#EFD79B]",
    priority: "bg-[#FBEAE7] text-[#A62F22] border-[#F1C3BB]",
    blue: "bg-[#E7F0FA] text-[#12518A] border-[#BFD6EE]",
  };
  return (
    <span className={`inline-flex items-center gap-1 text-[11px] font-medium px-2 py-1 rounded-full border ${tones[tone]}`} style={inter}>
      {children}
    </span>
  );
}

const sevTone = { High: "priority", Medium: "pending", Low: "verified" };
const statusTone = { New: "priority", "In Progress": "pending", Resolved: "verified" };

function Card({ children, className = "" }) {
  return (
    <div className={`rounded-xl border border-[#D8E0EA] bg-white shadow-[0_1px_2px_rgba(18,33,47,0.05)] ${className}`}>
      {children}
    </div>
  );
}

function Stat({ icon: Icon, label, value, tone, sub }) {
  return (
    <Card className="p-4">
      <Icon size={16} color={tone} />
      <p className="mt-2 text-[24px] font-semibold" style={{ color: tone, ...mono }}>{value}</p>
      <p className="text-[11.5px] mt-0.5 font-medium" style={{ ...inter, color: INK }}>{label}</p>
      {sub && <p className="text-[10.5px]" style={{ ...inter, color: INK_LO }}>{sub}</p>}
    </Card>
  );
}

/* =========================================================
   ROLE 1 — FIELD OFFICER: live ward-level data
========================================================= */
function FieldOfficer({ potholes, resolve }) {
  const [wardNo, setWardNo] = useState(59);
  const [sevFilter, setSevFilter] = useState("All");
  const [sortBySeverity, setSortBySeverity] = useState(true);
  const [tick, setTick] = useState(0);

  useEffect(() => {
    const t = setInterval(() => setTick((v) => v + 1), 5000);
    return () => clearInterval(t);
  }, []);

  const ward = WARD_SEED.find((w) => w.wardNo === wardNo);
  const wardPotholes = useMemo(
    () => potholes.filter((p) => p.ward === wardNo),
    [potholes, wardNo]
  );

  const counts = useMemo(() => ({
    High: wardPotholes.filter((p) => p.severity === "High" && p.status !== "Resolved").length,
    Medium: wardPotholes.filter((p) => p.severity === "Medium" && p.status !== "Resolved").length,
    Low: wardPotholes.filter((p) => p.severity === "Low" && p.status !== "Resolved").length,
  }), [wardPotholes]);

  const rank = { High: 0, Medium: 1, Low: 2 };
  const visible = useMemo(() => {
    let list = wardPotholes.filter((p) => sevFilter === "All" || p.severity === sevFilter);
    list = [...list].sort((a, b) =>
      sortBySeverity ? rank[a.severity] - rank[b.severity] || b.ageDays - a.ageDays : b.ageDays - a.ageDays
    );
    return list;
  }, [wardPotholes, sevFilter, sortBySeverity]);

  return (
    <div className="w-full max-w-6xl">
      <div className="flex flex-wrap items-end justify-between gap-3 mb-5">
        <div>
          <p className="text-[11px] tracking-wide" style={{ ...inter, color: INK_LO }}>GVMC ROADS &amp; BUILDINGS — FIELD OFFICER</p>
          <h2 style={{ ...display, color: INK, fontSize: 28 }}>WARD LIVE DEFECT BOARD</h2>
          <p className="text-[12.5px] mt-1" style={{ ...inter, color: INK_LO }}>
            Live ward-level pothole data — act before it escalates into a complaint or incident.
          </p>
        </div>
        <div className="flex items-center gap-2 text-[12px]" style={{ ...inter, color: GREEN }}>
          <Radio size={14} className="animate-pulse" /> Live · refreshed {tick * 5}s ago
        </div>
      </div>

      {/* Ward selector */}
      <Card className="p-4 mb-4">
        <div className="flex flex-wrap items-center gap-4">
          <div>
            <label className="text-[11px] block mb-1" style={{ ...inter, color: INK_LO }}>Assigned ward</label>
            <select
              value={wardNo}
              onChange={(e) => setWardNo(Number(e.target.value))}
              className="rounded-lg border px-3 py-2 text-[13px] bg-white"
              style={{ ...inter, borderColor: LINE, color: INK }}
            >
              {WARD_SEED.map((w) => (
                <option key={w.wardNo} value={w.wardNo}>
                  Ward {w.wardNo} — {w.localities[0]}
                </option>
              ))}
            </select>
          </div>
          <div className="min-w-[240px]">
            <p className="text-[11px]" style={{ ...inter, color: INK_LO }}>Zone</p>
            <p className="text-[14px] font-semibold" style={{ ...inter, color: GOV }}>
              {ward.zoneName} · {ward.zoneArea}
            </p>
            <p className="text-[11.5px] mt-1" style={{ ...inter, color: INK_LO }}>
              Localities: {ward.localities.join(", ")}
            </p>
          </div>
        </div>
      </Card>

      {/* Severity breakdown */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-3 mb-4">
        <Stat icon={Siren} label="High severity (open)" value={counts.High} tone={RED} sub="escalation risk" />
        <Stat icon={AlertTriangle} label="Medium severity (open)" value={counts.Medium} tone={AMBER} sub="monitor closely" />
        <Stat icon={CheckCircle2} label="Low severity (open)" value={counts.Low} tone={GREEN} sub="routine repair" />
        <Stat icon={ClipboardList} label="Total in ward" value={wardPotholes.length} tone={GOV} sub={`Ward ${ward.wardNo}`} />
      </div>

      <CrackDivider className="mb-4" />

      {/* Filters */}
      <Card className="p-4">
        <div className="flex flex-wrap items-center justify-between gap-2 mb-3">
          <p className="text-[13px] font-semibold" style={{ ...inter, color: INK }}>
            Live potholes — Ward {ward.wardNo}
          </p>
          <div className="flex items-center gap-1 text-[11px]" style={{ ...inter, color: INK_LO }}>
            Severity:
            {["All", "High", "Medium", "Low"].map((s) => (
              <button key={s} onClick={() => setSevFilter(s)}
                className="px-2 py-1 rounded-md border transition-colors"
                style={sevFilter === s
                  ? { background: "#E7F0FA", borderColor: "#BFD6EE", color: GOV }
                  : { borderColor: LINE, color: INK_LO }}>
                {s}
              </button>
            ))}
            <button onClick={() => setSortBySeverity((v) => !v)}
              className="ml-2 px-2 py-1 rounded-md border inline-flex items-center gap-1"
              style={{ borderColor: LINE, color: INK_LO }}>
              <ArrowUpDown size={11} /> {sortBySeverity ? "Severity first" : "Oldest first"}
            </button>
          </div>
        </div>

        <div className="space-y-2">
          {visible.map((p) => (
            <div key={p.id}
              className="flex flex-wrap items-center gap-3 rounded-lg border p-3"
              style={{ borderColor: "#E3E9F1", background: p.status === "Resolved" ? "#F7FAF9" : "white" }}>
              <HazardScore score={p.hazardScore} size={46} />
              <div className="flex-1 min-w-[180px]">
                <p className="text-[13px] font-semibold" style={{ ...inter, color: INK }}>
                  <MapPin size={12} className="inline mr-1" />{p.location}
                </p>
                <p className="text-[11px]" style={{ ...inter, color: INK_LO }}>
                  {p.id} · reported {p.reportedDate}
                </p>
              </div>
              <Pill tone={sevTone[p.severity]}>{p.severity}</Pill>
              <Pill tone={statusTone[p.status]}>{p.status}</Pill>
              <span className="text-[12px] inline-flex items-center gap-1"
                style={{ ...inter, color: p.ageDays > 14 && p.status !== "Resolved" ? RED : INK_LO }}>
                <Clock size={12} /> open {p.ageDays}d
              </span>
              {p.status !== "Resolved" ? (
                <button onClick={() => resolve(p.id)}
                  className="text-[12px] font-semibold px-3 py-2 rounded-lg text-white inline-flex items-center gap-1"
                  style={{ ...inter, background: GOV }}>
                  <Wrench size={12} /> Mark resolved
                </button>
              ) : (
                <span className="text-[12px] inline-flex items-center gap-1" style={{ ...inter, color: GREEN }}>
                  <CheckCircle2 size={13} /> Closed
                </span>
              )}
            </div>
          ))}
          {visible.length === 0 && (
            <p className="text-[12.5px] py-6 text-center" style={{ ...inter, color: INK_LO }}>
              No potholes match this filter in Ward {ward.wardNo}.
            </p>
          )}
        </div>
      </Card>
    </div>
  );
}

/* =========================================================
   ROLE 2 — COMMISSIONER'S OFFICE ANALYST: cross-ward dashboard
========================================================= */
function AnalystDashboard({ potholes, budgets, setBudget }) {
  const [sortKey, setSortKey] = useState("high");
  const [zoneFilter, setZoneFilter] = useState(0);

  const wards = useMemo(() => WARD_SEED.map((w) => {
    const items = potholes.filter((p) => p.ward === w.wardNo);
    return {
      ...w,
      total: items.length,
      high: items.filter((p) => p.severity === "High").length,
      medium: items.filter((p) => p.severity === "Medium").length,
      low: items.filter((p) => p.severity === "Low").length,
      budget: budgets[w.wardNo],
    };
  }), [potholes, budgets]);

  const filtered = useMemo(
    () => wards.filter((w) => zoneFilter === 0 || w.zoneId === zoneFilter),
    [wards, zoneFilter]
  );

  const ranked = useMemo(
    () => [...filtered].sort((a, b) => b[sortKey] - a[sortKey]),
    [filtered, sortKey]
  );

  const rollup = useMemo(() => zoneRollup(wards), [wards]);

  const totals = useMemo(() => ({
    potholes: wards.reduce((s, w) => s + w.total, 0),
    high: wards.reduce((s, w) => s + w.high, 0),
    budget: wards.reduce((s, w) => s + w.budget, 0).toFixed(1),
  }), [wards]);

  return (
    <div className="w-full max-w-6xl">
      <div className="flex flex-wrap items-end justify-between gap-3 mb-5">
        <div>
          <p className="text-[11px] tracking-wide" style={{ ...inter, color: INK_LO }}>GVMC COMMISSIONER'S OFFICE</p>
          <h2 style={{ ...display, color: INK, fontSize: 28 }}>ALL-WARDS PRIORITISATION DASHBOARD</h2>
          <p className="text-[12.5px] mt-1" style={{ ...inter, color: INK_LO }}>
            89 wards · 8 zones — one view to prioritise budget and staff deployment.
          </p>
        </div>
        <div className="flex items-center gap-2 text-[12px]" style={{ ...inter, color: INK_LO }}>
          <LayoutDashboard size={15} /> City-wide sync
        </div>
      </div>

      <div className="grid grid-cols-2 md:grid-cols-4 gap-3 mb-4">
        <Stat icon={ClipboardList} label="Total potholes" value={totals.potholes} tone={GOV} sub="across 89 wards" />
        <Stat icon={Siren} label="High severity" value={totals.high} tone={RED} sub="needs budget priority" />
        <Stat icon={IndianRupee} label="Budget allocated" value={`₹${totals.budget}L`} tone={AMBER} sub="current cycle" />
        <Stat icon={Users} label="Zones" value={ZONES.length} tone={GREEN} sub="zonal commissioners" />
      </div>

      {/* Zone rollup */}
      <Card className="p-4 mb-4">
        <div className="flex items-center gap-2 mb-3">
          <Layers size={14} color={GOV} />
          <p className="text-[13px] font-semibold" style={{ ...inter, color: INK }}>Zone-level rollup</p>
        </div>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
          {rollup.map((z) => (
            <button key={z.id} onClick={() => setZoneFilter(zoneFilter === z.id ? 0 : z.id)}
              className="text-left rounded-lg border p-3 transition-colors"
              style={{ borderColor: zoneFilter === z.id ? "#BFD6EE" : "#E3E9F1", background: zoneFilter === z.id ? "#F2F7FC" : "white" }}>
              <p className="text-[12.5px] font-semibold" style={{ ...inter, color: INK }}>{z.name}</p>
              <p className="text-[10.5px] mb-2" style={{ ...inter, color: INK_LO }}>{z.area} · wards {z.from}–{z.to}</p>
              <p className="text-[18px] font-semibold" style={{ ...mono, color: GOV }}>{z.total}</p>
              <p className="text-[10.5px]" style={{ ...inter, color: INK_LO }}>potholes</p>
              <div className="flex gap-1 mt-2 flex-wrap">
                <Pill tone="priority">H {z.high}</Pill>
                <Pill tone="pending">M {z.medium}</Pill>
                <Pill tone="verified">L {z.low}</Pill>
              </div>
              <p className="text-[11.5px] mt-2 font-medium" style={{ ...inter, color: AMBER }}>₹{z.budget}L allocated</p>
            </button>
          ))}
        </div>
      </Card>

      <CrackDivider className="mb-4" />

      {/* All wards table */}
      <Card className="p-4">
        <div className="flex flex-wrap items-center justify-between gap-2 mb-3">
          <p className="text-[13px] font-semibold" style={{ ...inter, color: INK }}>
            All wards {zoneFilter ? `· Zone ${zoneFilter} only` : ""} ({ranked.length})
          </p>
          <div className="flex items-center gap-1 text-[11px]" style={{ ...inter, color: INK_LO }}>
            Sort by:
            {[["high", "High severity"], ["total", "Total"], ["budget", "Budget"]].map(([k, l]) => (
              <button key={k} onClick={() => setSortKey(k)}
                className="px-2 py-1 rounded-md border transition-colors"
                style={sortKey === k
                  ? { background: "#E7F0FA", borderColor: "#BFD6EE", color: GOV }
                  : { borderColor: LINE, color: INK_LO }}>
                {l}
              </button>
            ))}
            {zoneFilter !== 0 && (
              <button onClick={() => setZoneFilter(0)} className="px-2 py-1 rounded-md border" style={{ borderColor: LINE, color: GOV }}>
                Clear zone
              </button>
            )}
          </div>
        </div>

        <div className="overflow-auto rounded-lg border border-[#E3E9F1] max-h-[620px]">
          <table className="w-full text-left" style={inter}>
            <thead className="sticky top-0">
              <tr className="bg-[#F4F6F9] text-[11px]" style={{ color: INK_LO }}>
                <th className="py-2 px-3 font-medium">Ward</th>
                <th className="py-2 px-2 font-medium">Zone</th>
                <th className="py-2 px-2 font-medium">Total</th>
                <th className="py-2 px-2 font-medium">High</th>
                <th className="py-2 px-2 font-medium">Med</th>
                <th className="py-2 px-2 font-medium">Low</th>
                <th className="py-2 px-2 font-medium">Budget (₹L)</th>
                <th className="py-2 px-2 font-medium">₹L per pothole</th>
              </tr>
            </thead>
            <tbody>
              {ranked.map((w) => {
                const perPothole = w.total ? (w.budget / w.total) : 0;
                const underFunded = w.high >= 3 && perPothole < 1;
                return (
                  <tr key={w.wardNo} className="border-t border-[#E3E9F1]"
                    style={{ background: underFunded ? "#FDF6F5" : "white" }}>
                    <td className="py-2.5 px-3">
                      <p className="text-[12.5px] font-medium" style={{ color: INK }}>Ward {w.wardNo}</p>
                      <p className="text-[10.5px]" style={{ color: INK_LO }}>{w.localities.slice(0, 2).join(", ")}</p>
                    </td>
                    <td className="px-2 text-[11.5px]" style={{ color: INK_LO }}>{w.zoneName}</td>
                    <td className="px-2 text-[12.5px]" style={{ ...mono, color: INK }}>{w.total}</td>
                    <td className="px-2 text-[12.5px] font-semibold" style={{ ...mono, color: w.high >= 3 ? RED : INK }}>{w.high}</td>
                    <td className="px-2 text-[12.5px]" style={{ ...mono, color: AMBER }}>{w.medium}</td>
                    <td className="px-2 text-[12.5px]" style={{ ...mono, color: GREEN }}>{w.low}</td>
                    <td className="px-2">
                      <input
                        type="number" step="0.5" min="0" value={w.budget}
                        onChange={(e) => setBudget(w.wardNo, Number(e.target.value))}
                        className="w-24 rounded-md border px-2 py-1 text-[12px]"
                        style={{ ...mono, borderColor: LINE, color: INK }}
                      />
                    </td>
                    <td className="px-2 text-[12px]" style={{ ...mono, color: underFunded ? RED : INK_LO }}>
                      {perPothole.toFixed(2)}{underFunded ? " ⚠" : ""}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
        <p className="text-[11px] mt-2" style={{ ...inter, color: INK_LO }}>
          Rows highlighted in red are under-funded relative to their high-severity load (≥3 high-severity defects and under ₹1L per pothole).
        </p>
      </Card>
    </div>
  );
}

/* =========================================================
   SHELL
========================================================= */
export default function RoadWatchApp() {
  useFonts();
  const [role, setRole] = useState("officer");
  const [potholes, setPotholes] = useState([]);
  const [budgets, setBudgets] = useState(() =>
    Object.fromEntries(WARD_SEED.map((w) => [w.wardNo, w.budget]))
  );

  useEffect(() => {
    let cancelled = false;
    async function load() {
      try {
        const data = await getPotholes();
        if (!cancelled) setPotholes(data.map(mapApiPothole));
      } catch (err) {
        console.error("Failed to load potholes", err);
      }
    }
    load();
    const interval = setInterval(load, REFRESH_INTERVAL_MS);
    return () => {
      cancelled = true;
      clearInterval(interval);
    };
  }, []);

  const resolve = (id) => {
    setPotholes((list) => list.map((p) => (p.id === id ? { ...p, status: "Resolved" } : p)));
    updatePotholeStatus(id, "fixed").catch((err) =>
      console.error("Failed to update pothole status", err)
    );
  };
  const setBudget = (wardNo, value) =>
    setBudgets((b) => ({ ...b, [wardNo]: value }));

  return (
    <div className="min-h-screen w-full" style={{ background: "#F4F6F9" }}>
      <header className="border-b bg-white" style={{ borderColor: LINE }}>
        <div className="max-w-6xl mx-auto px-4 py-3 flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-2">
            <img src="/logo.png" alt="S.P.O.T-V logo" className="w-8 h-8 rounded-md object-contain" />
            <div>
              <p style={{ ...display, color: INK, fontSize: 20, lineHeight: 1 }}>S.P.O.T-V</p>
              <p className="text-[10.5px]" style={{ ...inter, color: INK_LO }}>Smart Pothole Observation Technology of Visakhapatnam</p>
            </div>
          </div>
          <div className="flex gap-1 p-1 rounded-lg" style={{ background: "#EEF2F7" }}>
            {[["officer", "Field Officer"], ["analyst", "Commissioner's Office"]].map(([k, l]) => (
              <button key={k} onClick={() => setRole(k)}
                className="px-3 py-1.5 rounded-md text-[12.5px] font-medium transition-colors"
                style={role === k ? { background: "white", color: GOV, boxShadow: "0 1px 2px rgba(18,33,47,.08)" } : { color: INK_LO }}>
                {l}
              </button>
            ))}
          </div>
        </div>
      </header>

      <main className="max-w-6xl mx-auto px-4 py-6 flex justify-center">
        {role === "officer"
          ? <FieldOfficer potholes={potholes} resolve={resolve} />
          : <AnalystDashboard potholes={potholes} budgets={budgets} setBudget={setBudget} />}
      </main>
    </div>
  );
}
