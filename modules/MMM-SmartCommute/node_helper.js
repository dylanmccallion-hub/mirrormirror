const NodeHelper = require("node_helper");
const io = require("socket.io-client");
const PersistedStateHelper = require("../helpers/persistedStateHelper"); // Node can require fs

module.exports = NodeHelper.create({
  start() {
    console.log("🔗 MMM-SmartCommute helper connecting to central server");

    // Initialize persistence helper in the module folder
    this.stateHelper = new PersistedStateHelper(__dirname, "smartCommute.json");

    // Connect to central server
    this.socket = io("http://localhost:8081");

    this.socket.on("connect", () => {
      console.log("🟢 Connected to central server via websocket");
    });

    // Listen for server push updates
    this.socket.on("SMARTCOMMUTE_PUSH_UPDATE", (payload) => {
      console.log("📥 Received SMARTCOMMUTE_PUSH_UPDATE:", payload);

    // Ensure payload has a timestamp
      payload.lastUpdate = payload.lastUpdate || new Date().toISOString();

      // Persist payload
      this.stateHelper.set("smartCommute", payload);

      // Send to frontend
      this.sendSocketNotification("SMARTCOMMUTE_UPDATE", payload);
    });
  },

  socketNotificationReceived(notification, payload) {
    if (notification === "GET_SMART_COMMUTE") {
      this.loadCommute();
    }
  },

  async loadCommute() {
    // First try persisted local copy
    const persisted = this.stateHelper.get("smartCommute");
    if (persisted) {
      console.log("📂 Loaded persisted SmartCommute:", persisted);
      this.sendSocketNotification("SMARTCOMMUTE_UPDATE", persisted);
    }

    // Then optionally fetch fresh data from server
    try {
      const res = await fetch("http://localhost:8081/smartcommute");
      const data = await res.json();

      // Only update if data is valid
      if (data && Object.keys(data).length > 0) {
        this.stateHelper.set("smartCommute", data);
        this.sendSocketNotification("SMARTCOMMUTE_UPDATE", data);
      }
    } catch (err) {
      console.error("Error loading Smart Commute:", err);
    }
  }
});
