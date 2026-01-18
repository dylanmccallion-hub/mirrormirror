const NodeHelper = require("node_helper");
const io = require("socket.io-client");

module.exports = NodeHelper.create({
  start() {
    console.log("🔗 MMM-Health helper connected to central server");

    // Connect to central API websocket
    this.socket = io("http://localhost:8081");

    this.socket.on("connect", () => {
      console.log("🟢 Connected to central server via websocket");
    });

    // Receive health push updates
    this.socket.on("HEALTH_PUSH_UPDATE", (data) => {
      console.log("📥 PUSH UPDATE from server:", data);
      this.sendSocketNotification("HEALTH_UPDATE", data);
    });
  },

  socketNotificationReceived(notification, payload) {
    if (notification === "GET_HEALTH_DATA") {
      this.loadHealthData();
    }
  },

  async loadHealthData() {
    try {
      const res = await fetch("http://localhost:8081/health");
      const data = await res.json();
      this.sendSocketNotification("HEALTH_UPDATE", data);
    } catch (err) {
      console.error("Error loading health data:", err);
    }
  }
});
