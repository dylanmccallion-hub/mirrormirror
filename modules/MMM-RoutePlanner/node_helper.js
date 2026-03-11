const NodeHelper = require("node_helper");
const io = require("socket.io-client");
const PersistedStateHelper = require("../helpers/persistedStateHelper");

module.exports = NodeHelper.create({
  start() {
    console.log("🔗 MMM-RoutePlanner helper connected to central server");

    this.stateHelper = new PersistedStateHelper(__dirname, "routeInfo.json");

    this.socket = io("http://localhost:8081");

    this.socket.on("connect", () => {
      console.log("🟢 Connected to central server via websocket");
    });

    this.socket.on("ROUTE_PUSH_UPDATE", (updatedRoute) => {
      console.log("📥 ROUTE_PUSH_UPDATE:", updatedRoute);

      const routeTitle = updatedRoute.route || "Unknown route";
      const payload = { ...updatedRoute, routeTitle };

      // Persist the latest route
      this.stateHelper.set("routeInfo", payload);

      this.sendSocketNotification("ROUTE_INFO_UPDATE", payload);
    });
  },

  socketNotificationReceived(notification, payload) {
    if (notification === "GET_ROUTE_INFO") {
      this.loadRoute();
    }
  },

  async loadRoute() {
    // Send persisted route first
    const persisted = this.stateHelper.get("routeInfo");
    if (persisted) {
      console.log("📂 Loaded persisted route:", persisted);
      this.sendSocketNotification("ROUTE_INFO_UPDATE", persisted);
    }

    // Then fetch fresh from backend
    try {
      const res = await fetch("http://localhost:8081/route");
      const text = await res.text();

      if (!res.ok || text.trim().startsWith("<")) {
        console.error("RoutePlanner backend returned non-JSON:", text.slice(0, 200));
        return;
      }

      const route = JSON.parse(text);
      this.stateHelper.set("routeInfo", route);
      this.sendSocketNotification("ROUTE_INFO_UPDATE", route);

    } catch (err) {
      console.error("Error loading route:", err);
    }
  }
});
