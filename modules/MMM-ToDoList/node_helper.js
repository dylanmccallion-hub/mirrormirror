const NodeHelper = require("node_helper");
const express = require("express");

module.exports = NodeHelper.create({
  list: [], // store the todo list

  start() {
    console.log("✅ Starting MMM-ToDoList Node Helper");

    const app = express();
    app.use(express.json());

    // --- REST endpoint for your Android app ---
    app.post("/todolist", (req, res) => {
      const newList = req.body.list || [];
      console.log("📩 Received POST /todolist:", newList);

      // Save and send to frontend
      this.list = newList;
      console.log("➡️ Sending TODO_LIST_UPDATE to frontend:", this.list);
      this.sendSocketNotification("TODO_LIST_UPDATE", this.list);

      res.json({ success: true });
    });

    app.listen(8081, () => console.log("🚀 ToDo REST server running on port 8081"));
  },

  socketNotificationReceived(notification, payload) {
    console.log("Node Helper received:", notification, payload);

    if (notification === "GET_TODO_LIST") {
      // Send the current list to frontend
      console.log("➡️ Responding with current list:", this.list);
      this.sendSocketNotification("TODO_LIST_UPDATE", this.list);
    }

    if (notification === "ADD_TODO") {
      this.list.push(payload);
      console.log("➡️ Added new todo, sending updated list:", this.list);
      this.sendSocketNotification("TODO_LIST_UPDATE", this.list);
    }
  }
});
