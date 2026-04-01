const express = require("express");
const http = require("http");
const { Server } = require("socket.io");
const bonjour = require("bonjour")();
const os = require("os");

// Mirror ID (configurable)
const MIRROR_ID = "MMM-001";

const app = express();
app.use(express.json());

const server = http.createServer(app);
const io = new Server(server, {
  cors: { origin: "*" }
});

const serverStartTime = Date.now();

// --- Utility to get the server's local IPv4 address ---
function getLocalIp() {
  const interfaces = os.networkInterfaces();
  for (const iface of Object.values(interfaces)) {
    for (const i of iface) {
      if (i.family === "IPv4" && !i.internal) {
        return i.address;
      }
    }
  }
  return "unknown";
}

const localIp = getLocalIp();

// --- Socket.io connections ---
io.on("connection", (socket) => {
  console.log("MagicMirror connected to server.js");
});

// --- ToDo List ---
let todoList = [];
app.post("/todolist", (req, res) => {
  todoList = req.body.list || [];
  console.log("Updated todo list:", todoList);
  io.emit("TODO_PUSH_UPDATE", todoList);
  res.json({ success: true });
});
app.get("/todolist", (req, res) => res.json(todoList));

// --- Smart Commute ---
let smartCommuteData = {};
app.post("/smartcommute", (req, res) => {
  const { eventTitle, location, distance, travelTime, leaveInMinutes } = req.body;

  if (!eventTitle || !location || !distance || !travelTime || leaveInMinutes === undefined) {
    return res.status(400).json({ success: false, message: "Missing commute fields" });
  }

  smartCommuteData = {
    eventTitle,
    location,
    distance,
    travelTime,
    leaveInMinutes,
    lastUpdate: new Date().toISOString()
  };

  console.log("Updated Smart Commute info:", smartCommuteData);
  io.emit("SMARTCOMMUTE_PUSH_UPDATE", smartCommuteData);

  res.json({ success: true });
});
app.get("/smartcommute", (req, res) => res.json(smartCommuteData));

// --- Status endpoint ---
app.get("/status", (req, res) => {
  const uptimeSeconds = Math.floor((Date.now() - serverStartTime) / 1000);
  const hours = Math.floor(uptimeSeconds / 3600);
  const minutes = Math.floor((uptimeSeconds % 3600) / 60);

  res.json({
    id: MIRROR_ID,
    ip: localIp,
    uptime: `${hours}h ${minutes}m`,
    modules: ["Calendar", "Clock", "Compliments", "NewsFeed", "Weather", "ToDoList", "RoutePlanner", "Health", "SmartCommute"],
    lastUpdate: new Date().toISOString()
  });
});

// --- Route Info ---
let routeInfo = {};
app.post("/route", (req, res) => {
  const { distance, travelTime, route } = req.body;
  if (!distance || !travelTime || !route) {
    return res.status(400).json({ success: false, message: "Missing distance, travelTime or route" });
  }

  routeInfo = { distance, travelTime, route, lastUpdate: new Date().toISOString() };
  console.log("Updated route info:", routeInfo);
  io.emit("ROUTE_PUSH_UPDATE", routeInfo);

  res.json({ success: true });
});

// --- Location ---
let selectedLocation = { city: "New York", lat: 40.776676, lon: -73.971321 };
app.post("/location", (req, res) => {
  const { city, lat, lon } = req.body;
  if (typeof lat !== "number" || typeof lon !== "number") {
    return res.status(400).json({ success: false, message: "Missing or invalid lat/lon" });
  }

  selectedLocation = { city: city || selectedLocation.city, lat, lon };
  console.log("Updated location:", selectedLocation);
  io.emit("WEATHER_LOCATION_UPDATE", {
    lat: selectedLocation.lat,
    lon: selectedLocation.lon,
    location: selectedLocation.city
  });

  res.json({ success: true });
});

// --- Compliments ---
let complimentsList = [];
app.post("/compliments", (req, res) => {
  complimentsList = req.body.list || [];
  console.log("Updated compliments list:", complimentsList);
  io.emit("COMPLIMENTS_PUSH_UPDATE", complimentsList);
  res.json({ success: true });
});
app.get("/compliments", (req, res) => res.json(complimentsList));

// --- Health Data ---
let healthData = {};
app.post("/health", (req, res) => {
  healthData = req.body || {};
  console.log("Updated health data:", healthData);
  io.emit("HEALTH_PUSH_UPDATE", healthData);
  res.json({ success: true });
});
app.get("/health", (req, res) => res.json(healthData));

// --- Start HTTP server ---
server.listen(8081, () => {
  console.log(`Central API running on port 8081`);
  console.log(`Local IP: ${localIp}`);

  bonjour.publish({
    name: MIRROR_ID,
    type: "magicmirror",
    port: 8081
  });
  console.log(`Broadcasting mDNS service: ${MIRROR_ID}._magicmirror.local`);
});
