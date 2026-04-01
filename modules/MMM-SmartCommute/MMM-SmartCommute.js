Module.register("MMM-SmartCommute", {
  defaults: {
    header: "Smart Commute",
    fade: false,
    fadePoint: 0.25,
    animationSpeed: 500
  },

  start() {
    this.dataLoaded = false;
    this.smartCommute = {};
    this.localLeaveMinutes = null;
    console.log("✅ Starting MMM-SmartCommute frontend");

    // Live countdown interval
    this.countdownInterval = setInterval(() => {
      if (this.localLeaveMinutes !== null && this.listContainer) {
        this.localLeaveMinutes = Math.max(0, this.localLeaveMinutes - 1);
        this.updateLeaveTime();
      }
    }, 60000); // every 1 min

    this.sendSocketNotification("GET_SMART_COMMUTE");
  },

  socketNotificationReceived(notification, payload) {
    if (notification === "SMARTCOMMUTE_UPDATE") {
      console.log("📡 Frontend received SMARTCOMMUTE_UPDATE:", payload);

      this.smartCommute = payload;
      this.dataLoaded = true;

      // Compute minutes elapsed since last update
      const lastUpdate = new Date(payload.lastUpdate);
      const now = new Date();
      const minsElapsed = Math.floor((now - lastUpdate) / 60000);

      // Adjust leaveInMinutes for elapsed time
      const rawMins = Number(payload.leaveInMinutes);
      this.localLeaveMinutes = Math.max(0, rawMins - minsElapsed);

      if (this.listContainer) {
        this.updateList();
      }
    }
  },

  getDom() {
    if (!this.wrapper) {
      this.wrapper = document.createElement("div");
      this.wrapper.className = "MMM-SmartCommute";

      const header = document.createElement("div");
      header.className = "module-header";
      header.textContent = this.config.header;
      this.wrapper.appendChild(header);

      this.listContainer = document.createElement("ul");
      this.listContainer.className = "smart-commute-list";
      this.wrapper.appendChild(this.listContainer);
    }

    if (this.dataLoaded) {
      this.updateList();
    } else {
      this.listContainer.innerHTML = "";
      const loading = document.createElement("li");
      loading.className = "dimmed light small";
      loading.textContent = "Loading ...";
      this.listContainer.appendChild(loading);
    }

    return this.wrapper;
  },

  updateList() {
    this.listContainer.innerHTML = "";

    const fields = [
      { label: "Next Event", key: "eventTitle" },
      { label: "Location", key: "location" },
      { label: "Distance", key: "distance" },
      { label: "Travel Time", key: "travelTime" },
      { label: "Leave Time", key: "leaveInMinutes" }
    ];

    fields.forEach((f, index) => {
      const li = document.createElement("li");

      // Fade
      if (this.config.fade && fields.length > 0) {
        const startFade = fields.length * this.config.fadePoint;
        if (index >= startFade) {
          const fadeSteps = fields.length - startFade;
          li.style.opacity = 1 - (index - startFade) / fadeSteps;
        }
      }

      const label = document.createElement("strong");
      label.textContent = f.label + ": ";
      li.appendChild(label);

      const value = document.createElement("span");

      if (f.key === "leaveInMinutes") {
        let mins = this.localLeaveMinutes;
        let text = "N/A";

        if (mins !== null && !isNaN(mins)) {
          if (mins <= 0) text = "Leave now";
          else if (mins < 60) text = `in ${mins} mins`;
          else {
            const hours = Math.floor(mins / 60);
            const remainder = mins % 60;
            text = `in ${hours}h ${remainder}m`;
          }
        }

        value.textContent = text;
        value.classList.remove("urgent-green", "urgent-orange", "urgent-red");

        if (mins !== null && !isNaN(mins)) {
          if (mins <= 0) value.classList.add("urgent-red");
          else if (mins <= 15) value.classList.add("urgent-orange");
          else value.classList.add("urgent-green");
        }
      } else {
        value.textContent = this.smartCommute[f.key] || "N/A";
      }

      li.appendChild(value);
      this.listContainer.appendChild(li);
    });
  },

  updateLeaveTime() {
    if (!this.listContainer) return;
    const leaveLi = this.listContainer.querySelectorAll("li")[4]; // 5th item
    if (!leaveLi) return;

    let mins = this.localLeaveMinutes;
    let text = "N/A";

    if (mins !== null && !isNaN(mins)) {
      if (mins <= 0) text = "Leave now";
      else if (mins < 60) text = `in ${mins} mins`;
      else {
        const hours = Math.floor(mins / 60);
        const remainder = mins % 60;
        text = `in ${hours}h ${remainder}m`;
      }
    }

    leaveLi.querySelector("span").textContent = text;
    leaveLi.querySelector("span").classList.remove("urgent-green", "urgent-orange", "urgent-red");
    if (mins !== null && !isNaN(mins)) {
      if (mins <= 0) leaveLi.querySelector("span").classList.add("urgent-red");
      else if (mins <= 15) leaveLi.querySelector("span").classList.add("urgent-orange");
      else leaveLi.querySelector("span").classList.add("urgent-green");
    }
  },

  getStyles() {
    return ["MMM-SmartCommute.css"];
  },

  stop() {
    if (this.countdownInterval) clearInterval(this.countdownInterval);
  }
});
