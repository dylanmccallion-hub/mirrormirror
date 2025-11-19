const NodeHelper = require("node_helper");
const io = require("socket.io-client");

module.exports = NodeHelper.create({
  start() {
    console.log("🔗 MMM-ToDoList helper connected to central server");

    // Connect to central API websocket
    this.socket = io("http://localhost:8081");

    this.socket.on("connect", () => {
      console.log("🟢 Connected to central server via websocket");
    });

    // Receive push update
    this.socket.on("TODO_PUSH_UPDATE", (updatedList) => {
      console.log("📥 PUSH UPDATE from server:", updatedList);

      // Send to frontend
      this.sendSocketNotification("TODO_LIST_UPDATE", updatedList);
    });
  },

  socketNotificationReceived(notification, payload) {
    if (notification === "GET_TODO_LIST") {
      this.loadList();
    }
  },

  async loadList() {
    try {
      const res = await fetch("http://localhost:8081/todolist");
      const list = await res.json();
      this.sendSocketNotification("TODO_LIST_UPDATE", list);
    } catch (err) {
      console.error("Error loading list:", err);
    }
  }
});
