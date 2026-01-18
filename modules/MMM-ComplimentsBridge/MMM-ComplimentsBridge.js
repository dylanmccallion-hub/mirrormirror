Module.register("MMM-ComplimentsBridge", {
    start() {
        console.log("🌉 MMM-ComplimentsBridge frontend starting...");

        this.ready = false;
        this.queue = [];

        this.sendSocketNotification("PING_HELPER", null);

        // Give MM a few seconds to load all modules
        setTimeout(() => {
            this.ready = true;
            console.log("✅ ComplimentsBridge frontend ready, flushing queue");

            this.queue.forEach((item) => {
                this._handleNotification(item.notification, item.payload);
            });
            this.queue = [];
        }, 3000);
    },

    socketNotificationReceived(notification, payload) {
        if (!this.ready) {
            this.queue.push({ notification, payload });
            return;
        }
        this._handleNotification(notification, payload);
    },

    _handleNotification(notification, payload) {
        if (notification === "COMPLIMENTS_PUSH_UPDATE") {
            console.log("📡 Rebroadcasting compliments:", payload);
            this.sendNotification("CUSTOM_COMPLIMENTS_UPDATE", payload);
        }
    }
});
