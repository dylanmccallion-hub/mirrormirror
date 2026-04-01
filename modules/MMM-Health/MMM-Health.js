Module.register("MMM-Health", {
    defaults: {
        header: "Health",
        fade: true,
        fadePoint: 0.25,
        animationSpeed: 500
    },

    start() {
        console.log("✅ Starting MMM-Health frontend");

        this.ready = false;
        this.queue = [];
        this.dataLoaded = false;
        this.healthData = { steps: 0, calories: 0, height: "N/A", weight: "N/A" };

        // Notify helper we're ready
        setTimeout(() => {
            this.ready = true;
            console.log("✅ MMM-Health frontend ready, sending PING_HELPER");
            this.sendSocketNotification("PING_HELPER");

            // Flush queued notifications
            this.queue.forEach(item => this._handleNotification(item.notification, item.payload));
            this.queue = [];
        }, 100);
    },

    socketNotificationReceived(notification, payload) {
        if (!this.ready) {
            this.queue.push({ notification, payload });
            return;
        }
        this._handleNotification(notification, payload);
    },

    _handleNotification(notification, payload) {
        if (notification === "HEALTH_UPDATE") {
            console.log("📡 Frontend handling HEALTH_UPDATE:", payload);
            this.healthData = payload;
            this.dataLoaded = true;
            this.updateDom(this.config.animationSpeed);
        }
    },

    getDom() {
        const wrapper = document.createElement("div");
        wrapper.className = "MMM-Health";

        const header = document.createElement("div");
        header.className = "module-header";
        header.textContent = this.config.header || "Health";
        wrapper.appendChild(header);

        if (!this.dataLoaded) {
            const loading = document.createElement("div");
            loading.className = "dimmed light small";
            loading.textContent = "Loading ...";
            wrapper.appendChild(loading);
            return wrapper;
        }

        const ul = document.createElement("ul");
        const items = [
            `Steps Today: ${this.healthData.steps || 0}`,
            `Calories: ${this.healthData.calories || 0}`,
            `Height: ${this.healthData.height || "N/A"} cm`,
            `Weight: ${this.healthData.weight || "N/A"} kg`
        ];

        items.forEach((text, index) => {
            const li = document.createElement("li");
            li.textContent = text;

            if (this.config.fade && items.length > 0) {
                const startFade = items.length * (this.config.fadePoint || 0.25);
                if (index >= startFade) {
                    const fadeSteps = items.length - startFade;
                    li.style.opacity = 1 - (index - startFade) / fadeSteps;
                }
            }

            ul.appendChild(li);
        });

        wrapper.appendChild(ul);
        return wrapper;
    },

    getStyles() {
        return ["MMM-Health.css"];
    }
});
