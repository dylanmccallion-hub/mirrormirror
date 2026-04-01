Module.register("MMM-Humidity", {
  defaults: {
    hum: "--"
  },

  start() {
    this.loaded = false;
    console.log("✅ Starting MMM-Humidity frontend");
    this.sendSocketNotification("START_DHT11");
  },

  socketNotificationReceived(notification, payload) {
    if (notification === "DHT11_UPDATE") {
      if (!this.loaded) return;
      this.humValue.textContent = `${payload.humidity}%`;
    }
  },

  getDom() {
    if (!this.wrapper) {
      this.wrapper = document.createElement("div");
      this.wrapper.className = "MMM-Humidity";

      const icon = document.createElement("img");
      icon.src = this.file("humidity.png");
      icon.style.width = "32px";
      icon.style.height = "32px";
      icon.style.verticalAlign = "middle";
      icon.style.marginRight = "6px";

      this.humValue = document.createElement("span");
      this.humValue.textContent = this.config.hum;

      this.wrapper.appendChild(icon);
      this.wrapper.appendChild(this.humValue);

      this.loaded = true;
    }

    return this.wrapper;
  },

  getStyles() {
    return ["MMM-Humidity.css"];
  }
});
