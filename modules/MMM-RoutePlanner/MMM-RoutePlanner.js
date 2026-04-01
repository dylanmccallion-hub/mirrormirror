Module.register("MMM-RoutePlanner", {
  defaults: {
    header: "Route Planner",
    fade: true,
    fadePoint: 0.25
  },

  start() {
    this.route = {};
    this.loaded = false;
    console.log("✅ Starting MMM-RoutePlanner frontend");
    this.sendSocketNotification("GET_ROUTE_INFO");
  },

  socketNotificationReceived(notification, payload) {
    if (notification === "ROUTE_INFO_UPDATE") {
      console.log("📡 Frontend received ROUTE_INFO_UPDATE:", payload);
      this.route = payload;
      this.loaded = true;

      if (this.listContainer) {
        this.updateRouteList();
      }
    }
  },

  getDom() {
    if (!this.wrapper) {
      this.wrapper = document.createElement("div");
      this.wrapper.className = "MMM-RoutePlanner";

      // Header
      const header = document.createElement("div");
      header.className = "module-header";
      header.textContent = this.config.header;
      this.wrapper.appendChild(header);

      // List container
      this.listContainer = document.createElement("ul");
      this.listContainer.className = "route-list";
      this.wrapper.appendChild(this.listContainer);
    }

    if (this.loaded) {
      this.updateRouteList();
    } else {
      // Show waiting message
      this.listContainer.innerHTML = "";
      const loading = document.createElement("li");
      loading.className = "dimmed light small";
      loading.textContent = "Waiting for route ...";
      this.listContainer.appendChild(loading);
    }

    return this.wrapper;
  },

  updateRouteList() {
    this.listContainer.innerHTML = "";

    const rows = [
      { label: "Route", value: this.route.routeTitle || "N/A" },
      { label: "Distance", value: this.route.distance || "N/A" },
      { label: "Travel Time", value: this.route.travelTime || "N/A" }
    ];

    rows.forEach((row, index) => {
      const li = document.createElement("li");

      // Fade like other MagicMirror modules (optional)
      if (this.config.fade && rows.length > 0) {
        const startFade = rows.length * this.config.fadePoint;
        if (index >= startFade) {
          const fadeSteps = rows.length - startFade;
          li.style.opacity = 1 - (index - startFade) / fadeSteps;
        }
      }

      const label = document.createElement("span");
      label.className = "dimmed";
      label.textContent = `${row.label}: `;

      const value = document.createElement("span");
      value.textContent = row.value;

      li.appendChild(label);
      li.appendChild(value);
      this.listContainer.appendChild(li);
    });
  },

  getStyles() {
    return ["MMM-RoutePlanner.css"];
  }
});
