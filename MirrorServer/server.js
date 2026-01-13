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
let todoList = [];

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

// --- Receive new list from Android ---
app.post("/todolist", (req, res) => {
  todoList = req.body.list || [];
  console.log("Updated todo list:", todoList);

  // PUSH update to MagicMirror
  io.emit("TODO_PUSH_UPDATE", todoList);

  res.json({ success: true });
});

// --- MagicMirror fetches full list once ---
app.get("/todolist", (req, res) => {
  res.json(todoList);
});

// --- Status endpoint for Android app ---
app.get("/status", (req, res) => {
  const uptimeSeconds = Math.floor((Date.now() - serverStartTime) / 1000);
  const hours = Math.floor(uptimeSeconds / 3600);
  const minutes = Math.floor((uptimeSeconds % 3600) / 60);

  const status = {
    id: MIRROR_ID,
    ip: localIp,
    uptime: `${hours}h ${minutes}m`,
    modules: ["MMM-ToDoList", "MMM-RoutePlanner"], // you can add more module names here
    lastUpdate: new Date().toISOString()
  };

  res.json(status);
});

// server.js
let routeInfo = {}; // store the latest route info

// Android sends route info
// server.js — updated POST /route
app.post("/route", (req, res) => {
  const { distance, travelTime, route } = req.body; // <— get the origin->destination string

  if (!distance || !travelTime || !route) {
    return res.status(400).json({ success: false, message: "Missing distance, travelTime or route" });
  }

  // Save the full info including origin->destination
  routeInfo = {
    distance,
    travelTime,
    route, // <— this is the origin -> destination string
    lastUpdate: new Date().toISOString()
  };

  console.log("Updated route info:", routeInfo);

  // PUSH update to MagicMirror
  io.emit("ROUTE_PUSH_UPDATE", routeInfo);

  res.json({ success: true });
});


// --- Start HTTP server ---
server.listen(8081, () => {
  console.log(`Central API running on port 8081`);
  console.log(`Local IP: ${localIp}`);

  // 🔥 Advertise the MagicMirror service AFTER server is listening
  bonjour.publish({
    name: MIRROR_ID,
    type: "magicmirror",
    port: 8081
  });
  console.log(`Broadcasting mDNS service: ${MIRROR_ID}._magicmirror.local`);
});
