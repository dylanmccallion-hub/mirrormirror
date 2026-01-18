Module.register("MMM-RoutePlanner", {
  defaults: {
    header: "Route Planner",
    fade: true,
    fadePoint: 0.25,
    animationSpeed: 500
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
      this.updateDom(this.config.animationSpeed);
    }
  },

  getDom() {
    const wrapper = document.createElement("div");
    wrapper.className = "MMM-RoutePlanner";

    // Header (MagicMirror style)
    const header = document.createElement("div");
    header.className = "module-header";
    header.textContent = this.config.header;
    wrapper.appendChild(header);

    if (!this.loaded) {
      const loading = document.createElement("div");
      loading.className = "dimmed light small";
      loading.innerHTML = "Waiting for route ...";
      wrapper.appendChild(loading);
      return wrapper;
    }

    const ul = document.createElement("ul");
    ul.className = "route-list";

    const rows = [
      { label: "Route", value: this.route.routeTitle || "N/A" },
      { label: "Distance", value: this.route.distance || "N/A" },
      { label: "Travel Time", value: this.route.travelTime || "N/A" }
    ];

    rows.forEach((row, index) => {
      const li = document.createElement("li");

      // Fade like other MagicMirror list modules
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
      ul.appendChild(li);
    });

    wrapper.appendChild(ul);
    return wrapper;
  },

  getStyles() {
    return ["MMM-RoutePlanner.css"];
  }
});
