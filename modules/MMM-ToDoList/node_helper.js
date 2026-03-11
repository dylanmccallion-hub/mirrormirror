const NodeHelper = require("node_helper");
const io = require("socket.io-client");
const PersistedStateHelper = require("../helpers/persistedStateHelper"); // Node-only

module.exports = NodeHelper.create({
  start() {
    console.log("🔗 MMM-ToDoList helper connected to central server");

    // Initialize persistence helper
    this.stateHelper = new PersistedStateHelper(__dirname, "todoList.json");

    this.socket = io("http://localhost:8081");

    this.socket.on("connect", () => {
      console.log("🟢 Connected to central server via websocket");
    });

    this.socket.on("TODO_PUSH_UPDATE", (updatedList) => {
      console.log("📥 PUSH UPDATE from server:", updatedList);

      // Persist the updated list
      this.stateHelper.set("todoList", updatedList);

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
    // Load persisted list first
    const persisted = this.stateHelper.get("todoList");
    if (persisted && persisted.length > 0) {
      console.log("📂 Loaded persisted ToDoList:", persisted);
      this.sendSocketNotification("TODO_LIST_UPDATE", persisted);
    }

    // Then try fetching fresh from server
    try {
      const res = await fetch("http://localhost:8081/todolist");
      const list = await res.json();

      if (Array.isArray(list) && list.length > 0) {
        this.stateHelper.set("todoList", list);
        this.sendSocketNotification("TODO_LIST_UPDATE", list);
      } else {
        console.log("⚠️ Server returned empty ToDo list, keeping persisted list");
      }
    } catch (err) {
      console.error("Error loading ToDo list:", err);
    }
  }
});
