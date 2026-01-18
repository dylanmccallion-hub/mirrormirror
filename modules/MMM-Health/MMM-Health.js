Module.register("MMM-Health", {
    defaults: {
        header: "Health",
        fade: true,
        fadePoint: 0.25,
        animationSpeed: 500
    },

    start() {
        this.dataLoaded = false;
        this.healthData = { steps: 0, calories: 0 };
        console.log("✅ Starting MMM-Health frontend");
        this.sendSocketNotification("GET_HEALTH_DATA");
    },

    socketNotificationReceived(notification, payload) {
        if (notification === "HEALTH_UPDATE") {
            console.log("📡 Frontend received HEALTH_UPDATE:", payload);
            this.healthData = payload;
            this.dataLoaded = true;
            this.updateDom(this.config.animationSpeed);
        }
    },

    getDom() {
        const wrapper = document.createElement("div");
        wrapper.className = "MMM-Health";

        // Header
        const header = document.createElement("div");
        header.className = "module-header";
        header.textContent = this.config.header || "Health";
        wrapper.appendChild(header);

        if (!this.dataLoaded) {
            const loading = document.createElement("div");
            loading.className = "dimmed light small";
            loading.innerHTML = "Loading ...";
            wrapper.appendChild(loading);
            return wrapper;
        }

        const ul = document.createElement("ul");

        const items = [
            `Steps: ${this.healthData.steps || 0}`,
            `Calories: ${this.healthData.calories || 0}`
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
