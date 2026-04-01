Module.register("MMM-ToDoList", {
  defaults: {
    header: "To-Do List"
  },

  start() {
    this.list = [];
    this.loaded = false;
    console.log("✅ Starting MMM-ToDoList frontend");
    this.sendSocketNotification("GET_TODO_LIST");
  },

  socketNotificationReceived(notification, payload) {
    if (notification === "TODO_LIST_UPDATE") {
      this.list = Array.isArray(payload) ? payload : [];
      this.loaded = true;

      if (this.listContainer) {
        // Update only the list items
        this.updateList();
      }
    }
  },

  getDom() {
    if (!this.wrapper) {
      this.wrapper = document.createElement("div");
      this.wrapper.className = "MMM-ToDoList";

      // Header
      const header = document.createElement("div");
      header.className = "module-header";
      header.textContent = this.config.header;
      this.wrapper.appendChild(header);

      // List container
      this.listContainer = document.createElement("ul");
      this.listContainer.className = "todo-list";
      this.wrapper.appendChild(this.listContainer);

      this.loaded = true;
    }

    if (this.loaded) {
      this.updateList();
    }

    return this.wrapper;
  },

  updateList() {
    // Clear current list
    this.listContainer.innerHTML = "";

    // Render new items
    this.list.forEach((item, index) => {
      const li = document.createElement("li");

      const title = document.createElement("span");
      title.textContent = item.title;
      li.appendChild(title);

      if (item.dueDate) {
        const due = document.createElement("span");
        due.className = "dueDate";
        due.textContent = `Due: ${item.dueDate}`;

        const now = new Date();
        const [day, month, year] = item.dueDate.split("/").map(Number);
        const dueDateObj = new Date(year, month - 1, day);
        const diffDays = Math.floor((dueDateObj - now) / (1000 * 60 * 60 * 24));

        if (diffDays < 0) due.classList.add("overdue");
        else if (diffDays < 1) due.classList.add("due-soon");

        li.appendChild(document.createElement("br"));
        li.appendChild(due);
      }

      this.listContainer.appendChild(li);
    });
  },

  getStyles() {
    return ["MMM-ToDoList.css"];
  }
});
