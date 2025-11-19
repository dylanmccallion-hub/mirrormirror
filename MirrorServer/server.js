const express = require("express");
const http = require("http");
const { Server } = require("socket.io");

const app = express();
app.use(express.json());

const server = http.createServer(app);
const io = new Server(server, {
  cors: { origin: "*" }
});

let todoList = [];

io.on("connection", (socket) => {
  console.log("MagicMirror connected to server.js");
});

// Receive new list from Android
app.post("/todolist", (req, res) => {
  todoList = req.body.list || [];
  console.log("Updated todo list:", todoList);

  // PUSH update to MagicMirror
  io.emit("TODO_PUSH_UPDATE", todoList);

  res.json({ success: true });
});

// MagicMirror fetches full list once (on startup)
app.get("/todolist", (req, res) => {
  res.json(todoList);
});

server.listen(8081, () => console.log("Central API running on port 8081"));
