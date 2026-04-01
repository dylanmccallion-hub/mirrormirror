const NodeHelper = require("node_helper");
const io = require("socket.io-client");
const PersistedStateHelper = require("../helpers/persistedStateHelper"); // Node-only

module.exports = NodeHelper.create({
  start() {
    console.log("🔗 MMM-Health helper connected to central server");

    // Initialize persistence
    this.stateHelper = new PersistedStateHelper(__dirname, "healthData.json");

    // Connect to central API websocket
    this.socket = io("http://localhost:8081");

    this.socket.on("connect", () => {
      console.log("🟢 Connected to central server via websocket");
    });

    // Listen for push updates from the server
    this.socket.on("HEALTH_PUSH_UPDATE", (data) => {
      const payload = {
        steps: data.steps,
        calories: data.calories,
        height: data.height,
        weight: data.weight
      };

      // Persist and send
      this.stateHelper.set("healthData", payload);
      this.sendSocketNotification("HEALTH_UPDATE", payload);
    });
  },

  socketNotificationReceived(notification, payload) {
    if (notification === "PING_HELPER") {
      console.log("🟢 Frontend connected, PING received");

      // Send persisted data only now
      const persisted = this.stateHelper.get("healthData");
      if (persisted) {
        console.log("📂 Sending persisted health data to frontend:", persisted);
        this.sendSocketNotification("HEALTH_UPDATE", persisted);
      }
    }
  }
});
