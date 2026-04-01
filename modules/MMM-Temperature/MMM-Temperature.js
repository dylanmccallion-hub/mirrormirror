Module.register("MMM-Temperature", {
  defaults: {
    temp: "--"
  },

  start() {
    this.loaded = false;
    console.log("✅ Starting MMM-Temperature frontend");
    this.sendSocketNotification("START_DHT11");
  },

  socketNotificationReceived(notification, payload) {
    if (notification === "DHT11_UPDATE") {
      if (!this.loaded) return; // don't update if not loaded yet
      this.tempValue.textContent = `${payload.temperature}℃`;
    }
  },

  getDom() {
    if (!this.wrapper) {
      this.wrapper = document.createElement("div");
      this.wrapper.className = "MMM-Temperature";

      const icon = document.createElement("img");
      icon.src = this.file("temp.png");
      icon.style.width = "32px";
      icon.style.height = "32px";
      icon.style.verticalAlign = "middle";
      icon.style.marginRight = "6px";

      this.tempValue = document.createElement("span");
      this.tempValue.textContent = this.config.temp;

      this.wrapper.appendChild(icon);
      this.wrapper.appendChild(this.tempValue);

      this.loaded = true;
    }

    return this.wrapper;
  },

  getStyles() {
    return ["MMM-Temperature.css"];
  }
});
