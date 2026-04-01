const NodeHelper = require("node_helper");
const io = require("socket.io-client");
const PersistedStateHelper = require("../helpers/persistedStateHelper");

module.exports = NodeHelper.create({
  start() {
    console.log("🌉 MMM-WeatherBridge helper starting...");

    this.stateHelper = new PersistedStateHelper(__dirname, "weatherLocation.json");

    // queue updates if frontend not ready
    this.frontendReady = false;
    this.queue = [];

    this.socket = io("http://localhost:8081");

    this.socket.on("connect", () => {
      console.log("🟢 Connected to central server via websocket");
    });

    this.socket.on("WEATHER_LOCATION_UPDATE", (payload) => {
      console.log("📡 Received location from server:", payload);

      // Persist update
      this.stateHelper.set("weatherLocation", payload);

      // Forward to frontend (queue if not ready)
      this._sendToFrontend(payload);
    });
  },

  socketNotificationReceived(notification, payload) {
    if (notification === "PING_HELPER") {
      console.log("🟢 Frontend connected, PING received");
      this.frontendReady = true;

      // Send persisted location now
      const persisted = this.stateHelper.get("weatherLocation");
      if (persisted) {
        console.log("📂 Sending persisted location to frontend:", persisted);
        this._sendToFrontend(persisted);
      }

      // flush any queued updates
      this.queue.forEach((item) => this._sendToFrontend(item));
      this.queue = [];
    }
  },

  _sendToFrontend(payload) {
    if (this.frontendReady) {
      this.sendSocketNotification("WEATHER_LOCATION_UPDATE", payload);
    } else {
      this.queue.push(payload);
    }
  }
});

