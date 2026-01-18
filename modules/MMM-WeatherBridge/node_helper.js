const NodeHelper = require("node_helper");
const io = require("socket.io-client");

module.exports = NodeHelper.create({
  start() {
    console.log("🌉 MMM-WeatherBridge helper starting...");

    // Connect to your central server
    this.socket = io("http://localhost:8081");

    this.socket.on("connect", () => {
      console.log("🟢 Connected to central server via websocket");
    });

    // Listen for location updates from the server
    this.socket.on("WEATHER_LOCATION_UPDATE", (payload) => {
      console.log("📡 Received location from server:", payload);

      // Forward to MagicMirror frontend
      this.sendSocketNotification("WEATHER_LOCATION_UPDATE", payload);
    });
  },

  socketNotificationReceived(notification, payload) {
    // You could forward commands from frontend to server here if needed
    if (notification === "PING_HELPER") {
        console.log("🟢 Frontend connected, PING received");
    }
  }
});
