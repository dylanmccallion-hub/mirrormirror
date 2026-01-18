const NodeHelper = require("node_helper");
const io = require("socket.io-client");

module.exports = NodeHelper.create({
    start() {
        console.log("🌉 MMM-ComplimentsBridge helper starting...");

        this.socket = io("http://localhost:8081");

        this.socket.on("connect", () => {
            console.log("🟢 Connected to central server via websocket");
        });

        this.socket.on("COMPLIMENTS_PUSH_UPDATE", (payload) => {
            console.log("📡 Received compliments from server:", payload);
            this.sendSocketNotification("COMPLIMENTS_PUSH_UPDATE", payload);
        });
    },

    socketNotificationReceived(notification, payload) {
        if (notification === "PING_HELPER") {
            console.log("🟢 Frontend connected, PING received");
        }
    }
});
