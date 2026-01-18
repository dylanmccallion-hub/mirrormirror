const NodeHelper = require("node_helper");
const io = require("socket.io-client");

module.exports = NodeHelper.create({
  start() {
    console.log("🔗 MMM-RoutePlanner helper connected to central server");

    this.socket = io("http://localhost:8081");

    this.socket.on("connect", () => {
      console.log("🟢 Connected to central server via websocket");
    });
    this.socket.on("ROUTE_PUSH_UPDATE", (updatedRoute) => {
    console.log("📥 ROUTE_PUSH_UPDATE:", updatedRoute);
    const routeTitle = updatedRoute.route || "Unknown route";
    this.sendSocketNotification("ROUTE_INFO_UPDATE", {
      ...updatedRoute,
      routeTitle
    }); 
});


  },

  socketNotificationReceived(notification, payload) {
    if (notification === "GET_ROUTE_INFO") {
      this.loadRoute();
    }
  },

  async loadRoute() {
    try {
      const res = await fetch("http://localhost:8081/route");

      const text = await res.text();

      // If backend returned HTML or non-JSON, ignore it
      if (!res.ok || text.trim().startsWith("<")) {
        console.error("RoutePlanner backend returned non-JSON:", text.slice(0, 200));
        return;
      }

      const route = JSON.parse(text);
      this.sendSocketNotification("ROUTE_INFO_UPDATE", route);

    } catch (err) {
      console.error("Error loading route:", err);
    }
  }
});
