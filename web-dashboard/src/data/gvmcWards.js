/* ---------------------------------------------------------
   GVMC ward reference data — 89 wards across 8 zones.
   Zone boundaries and locality names sourced from the GVMC
   elected-council ward/locality list.

   Note: the mock POTHOLES seed array from the original
   frontend-previewer-main version of this file has been removed —
   pothole data now comes from the live backend (see
   src/components/RoadWatchApp.jsx). Everything below is static
   ward/zone reference data, unrelated to pothole reports.
--------------------------------------------------------- */

export const ZONES = [
  { id: 1, name: "Zone 1", area: "Bheemili", from: 1, to: 4 },
  { id: 2, name: "Zone 2", area: "Madhurawada / Arilova", from: 5, to: 13 },
  { id: 3, name: "Zone 3", area: "Seethammadhara / MVP / Maddilapalem", from: 14, to: 27 },
  { id: 4, name: "Zone 4", area: "Old Town / Allipuram / Dabagardens", from: 28, to: 39 },
  { id: 5, name: "Zone 5", area: "Malkapuram / Gajuwaka", from: 40, to: 63 },
  { id: 6, name: "Zone 6", area: "Gajuwaka / Pedagantyada / Duvvada", from: 64, to: 79 },
  { id: 7, name: "Zone 7", area: "Anakapalli", from: 80, to: 84 },
  { id: 8, name: "Zone 8", area: "Gopalapatnam / Pendurthi / Simhachalam", from: 85, to: 89 },
];

export function zoneOf(wardNo) {
  return ZONES.find((z) => wardNo >= z.from && wardNo <= z.to);
}

/* Locality names per ward (from the GVMC ward list) */
export const WARD_LOCALITIES = {
  1: ["Bheemili", "Sangivalasa", "Kondapeta", "Wilsonpeta"],
  2: ["Chinnabazar", "Jeerupeta", "Thagarapuvalasa Road", "Mamidipalem"],
  3: ["Yeguvapeta", "Burma Colony", "Kothapeta", "Nerellavalasa"],
  4: ["Pedda Uppada", "Chepaluppada", "Kapuluppada", "JV Agraharam"],
  5: ["Bottavanipalem", "Kommadi", "Paradesipalem", "Marikavalasa"],
  6: ["Bakkannapalem", "Chandrampalem", "Madhurawada Junction", "FCI Layout"],
  7: ["Madhurawada", "Vambay Colony", "Mallayyapalem", "Timmapuram"],
  8: ["Yendada", "Sagar Nagar", "PM Palem", "Rushikonda"],
  9: ["Visalakshi Nagar", "Jodugullapalem", "Sanjay Nagar", "Dairy Farm"],
  10: ["Vivekananda Nagar", "Rajeev Nagar", "Thotagaruvu", "SIG Nagar"],
  11: ["Arilova", "Chinna Waltair", "Sector-4 Arilova", "Yendada Hill"],
  12: ["Sangivalasa", "Kothavalasa Road", "Adarsh Nagar", "Anandapuram Link"],
  13: ["Simhapuri Colony", "Pandurangapuram", "Lawsons Bay", "Kirlampudi Layout"],
  14: ["Seethammadhara", "NGGO's Colony", "Ram Nagar", "Balaji Nagar"],
  15: ["MVP Colony Sector-1", "MVP Double Road", "Vasanth Nagar", "Sector-9"],
  16: ["MVP Sector-11", "Rednam Gardens", "Krishna Nagar", "Beach Road"],
  17: ["Maddilapalem", "Isukathota", "HB Colony", "Chinna Gadili"],
  18: ["Gopalapatnam Link Road", "Kobbari Thota", "Sriharipuram Colony", "Ganesh Nagar"],
  19: ["Siripuram", "Waltair Uplands", "Daspalla Hills", "AU Campus"],
  20: ["Dwaraka Nagar", "Jail Road", "Bus Complex Area", "Ashok Nagar"],
  21: ["Ram Nagar", "Ganesh Nagar", "Peda Waltair", "Boddavara Junction"],
  22: ["Kancharapalem", "Gnanapuram", "Marripalem Road", "Railway Colony"],
  23: ["Marripalem", "NAD Kotha Road", "Vayu Nagar", "Ushodaya Junction"],
  24: ["Akkayyapalem Main Road", "Kailasapuram", "Kancharapalem Cross", "Chinna Waltair Road"],
  25: ["Seethampeta", "Madhura Nagar", "Chakali Gedda", "Shantipuram"],
  26: ["Akkayapalem", "Rajendra Nagar", "80 Feet Road", "Apoorva Hospital Road"],
  27: ["Resapuvanipalem", "Prakash Nagar", "Sriram Nagar", "Bhupesh Nagar"],
  28: ["Old Town", "Poorna Market", "Kotha Road", "Burujupeta"],
  29: ["Maharanipeta", "Jagadamba Junction", "Ram Talkies Road", "Town Kotha Road"],
  30: ["Allipuram", "Kobbarithota", "Sivaji Palem", "Kancharapalem Road"],
  31: ["Dabagardens", "SRMT Road", "Leela Mahal Road", "Sai Baba Street"],
  32: ["Soldierpeta", "Peda Jalaripeta", "Fishing Harbour Road", "Kobbari Thota"],
  33: ["Bangaramma Metta", "South Jail Road", "Harijana Veedhi", "Venkateswara Metta"],
  34: ["Netaji Nagar", "Atchiyamma Peta", "Chaluvathota", "Taraka Rama Colony"],
  35: ["One Town", "Kotha Jalaripeta", "Nakkavanipalem", "Salipeta"],
  36: ["Pithapuram Colony", "Ganganagar", "Kancharapalem Bypass", "Turner's Choultry"],
  37: ["Sriharipuram Old Town", "Malkapuram Road", "Bapuji Nagar", "Sivaji Nagar"],
  38: ["Kancharapalem Gate", "Sanjeeva Nagar", "Railway Quarters", "Gnanapuram Colony"],
  39: ["Bheemunipatnam Road Junction", "Butchirajupalem", "Vepagunta Link", "Yellamma Thota"],
  40: ["Malkapuram", "Chinna Mushidiwada", "Naval Colony", "Bhanoji Thota"],
  41: ["Thatichetlapalem", "Railway New Colony", "Jaganadhapuram", "Chakalipeta"],
  42: ["Ganesh Street", "Gas Godown Area", "Ramalayam Street", "Sai Baba Temple Road"],
  43: ["Kancharapalem North", "Simhachalam Road", "Mudasarlova", "Sujatha Nagar"],
  44: ["Madhavadhara", "Vidya Nagar", "Murali Nagar", "Kalinga Nagar"],
  45: ["Seethanna Gardens", "Industrial Estate", "Auto Nagar Road", "Tenneti Nagar"],
  46: ["Kancharapalem Cross Road", "Singaraya Metta", "Manchu Konda Thota", "Estate Colony"],
  47: ["NAD Junction", "Vayupuri", "Sector-5 NAD", "Kotha Road NAD"],
  48: ["Simhapuri", "Gopalapatnam Road", "Mindi", "Chinna Mushidiwada"],
  49: ["Mulagada", "Old Ramalayam", "Kunchumamba Colony", "Gollapalem"],
  50: ["Madhavadhara Manyam", "Sai Ram Nagar", "Murali Nagar Estate", "Kalinga Nagar Extn"],
  51: ["Scindia", "Port Quarters", "Dockyard Road", "Sriharipuram"],
  52: ["Yerrukonda", "Kunchalamma Veedhi", "Annapurna Nagar", "Ajantha Colony"],
  53: ["Malkapuram Bazar", "Ganesh Vani Palem", "Kodipandela Dibba", "Annamma Colony"],
  54: ["Salagramapuram", "Pilakavani Palem", "Sriharipuram Extn", "Netaji Colony"],
  55: ["Gajuwaka Old Post Office", "BC Road", "Azeemabad", "Indira Colony"],
  56: ["Kurmannapalem", "Aganampudi Road", "Sriram Nagar", "Vadlapudi"],
  57: ["Duvvada", "Sanathnagar", "Sloka School Road", "VUDA Colony"],
  58: ["Sriharipuram", "Mulagada Main Road", "Kunchumamba Colony", "Old Ramalayam"],
  59: ["Gajuwaka", "Gajuwaka Main Road", "Kailash Nagar", "Seetha Ram Nagar"],
  60: ["New Gajuwaka", "Fathima Street", "BC Road Extn", "Pydimamba Colony"],
  61: ["Adavivaram", "Simhachalam Foot Hills", "Hanumanthawaka", "Kailasapuram"],
  62: ["Chinna Gadili", "Gopalapatnam Junction", "Sujatha Nagar Road", "Bank Colony"],
  63: ["Marripalem VUDA Layout", "Vayu Nagar Extn", "Ushodaya Colony", "NAD Kotha Road"],
  64: ["Pedagantyada", "Dayal Nagar", "Bhanoji Thota", "Vikas Nagar"],
  65: ["Sanjeevagiri Colony", "Netaji Colony", "KL Rao Nagar", "Kotta Dibbapalem"],
  66: ["Azeemabad", "BC Road", "Gajuwaka Main Road", "Kailash Nagar"],
  67: ["Sriram Nagar Gajuwaka", "Old Gajuwaka", "Bhavani Nagar", "Vambay Colony"],
  68: ["Kurmannapalem", "Steel Plant Road", "Ukkunagaram Gate", "Aganampudi"],
  69: ["Duvvada Railway Station Road", "Vadlapudi", "Chinna Nadupuru", "Sanath Nagar"],
  70: ["Autonagar", "Gajuwaka Junction", "Simhagiri Colony", "Pydimamba Colony"],
  71: ["Pedagantyada Rural", "Girija Colony", "Priyadarshini Colony", "Ashok Nagar"],
  72: ["Mindi", "Chinna Mushidiwada", "Naval Coast Battery", "Malkapuram Link"],
  73: ["Sanathnagar", "Gonthinavanipalem", "Kotha Karnavani Palem", "China Nadupuru"],
  74: ["Ukkunagaram Periphery", "Kurmannapalem Bypass", "Vadlapudi Colony", "Sector-11"],
  75: ["Aganampudi", "Duvvada Layout", "Thanam", "Chintalagraharam"],
  76: ["Gajuwaka Bazaar", "Sriram Nagar Extn", "Kanithi Road", "Mulagada Road"],
  77: ["Pedagantyada Industrial", "Bhanoji Thota Extn", "Ravindra Nagar", "Sanjeeva Colony"],
  78: ["Duvvada Village", "Vadlapudi Junction", "Kurmannapalem Colony", "Bapuji Colony"],
  79: ["Gangavaram Road", "Dibbapalem", "Vambay Colony Extn", "Girija Colony"],
  80: ["Anakapalli Town", "Sarpavaram Road", "Nehru Bazaar", "Kanithi Junction"],
  81: ["Anakapalli Bus Stand", "Sugar Factory Road", "Gavarapalem", "Ganesh Nagar"],
  82: ["Kottavuratla Road", "Anakapalli Market", "Balaji Nagar", "Kasimkota Link"],
  83: ["Anakapalli Rural", "Yerravaram Road", "Sanivada", "Thumapala"],
  84: ["Anakapalli North", "Old Town Anakapalli", "Ravanapalem", "Chinna Bazaar"],
  85: ["Simhachalam", "Adavivaram", "Hanumanthawaka", "Temple Road"],
  86: ["Pendurthi", "Kothavalasa Road", "Pinagadi", "Vepagunta"],
  87: ["Vepagunta", "Sujatha Nagar", "Mudasarlova Road", "Chinna Gadili"],
  88: ["Gopalapatnam Bypass", "Sanjeeva Nagar", "Sriharipuram Extn", "Prahaladapuram"],
  89: ["Gopalapatnam", "NAD Kotha Road", "Bhanoji Thota", "Kothavalasa Junction"],
};

/* Deterministic pseudo-random helper, kept for the ward budget figures below
   (synthetic allocation data, unrelated to pothole reports). */
function rand(seed) {
  const x = Math.sin(seed * 9973.13) * 43758.5453;
  return x - Math.floor(x);
}

export const SEVERITIES = ["High", "Medium", "Low"];
export const STATUSES = ["New", "In Progress", "Resolved"];

/* Ward records — reference data plus an allocated budget (₹ lakh).
   total/high/medium/low start at 0 here: RoadWatchApp.jsx recomputes them
   from live pothole data on every render, so these are just safe defaults
   for a ward before any live data has loaded. */
export const WARDS = Array.from({ length: 89 }, (_, idx) => {
  const wardNo = idx + 1;
  const zone = zoneOf(wardNo);
  const budget = Number((3 + rand(wardNo * 5) * 17).toFixed(1)); // ₹3L–₹20L
  return {
    wardNo,
    zoneId: zone.id,
    zoneName: zone.name,
    zoneArea: zone.area,
    localities: WARD_LOCALITIES[wardNo] || [zone.area],
    total: 0,
    high: 0,
    medium: 0,
    low: 0,
    budget,
  };
});

export function wardByNo(no) {
  return WARDS.find((w) => w.wardNo === no);
}

export function zoneRollup(wards) {
  return ZONES.map((z) => {
    const inZone = wards.filter((w) => w.zoneId === z.id);
    return {
      ...z,
      wards: inZone.length,
      total: inZone.reduce((s, w) => s + w.total, 0),
      high: inZone.reduce((s, w) => s + w.high, 0),
      medium: inZone.reduce((s, w) => s + w.medium, 0),
      low: inZone.reduce((s, w) => s + w.low, 0),
      budget: Number(inZone.reduce((s, w) => s + w.budget, 0).toFixed(1)),
    };
  });
}
