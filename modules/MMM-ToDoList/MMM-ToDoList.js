Module.register("MMM-ToDoList", {
  defaults: {
    header: "To-Do List",
    fade: true,
    fadePoint: 0.25, // start fading after 25%
    animationSpeed: 500
  },

  start() {
    this.list = [];
    this.loaded = false;
    console.log("✅ Starting MMM-ToDoList frontend");
    this.sendSocketNotification("GET_TODO_LIST");
  },

  socketNotificationReceived(notification, payload) {
    if (notification === "TODO_LIST_UPDATE") {
      console.log("📡 Frontend received TODO_LIST_UPDATE:", payload);
      this.list = payload;
      this.loaded = true;
      this.updateDom(this.config.animationSpeed);
    }
  },


getDom() {
  const wrapper = document.createElement("div");
  wrapper.className = "MMM-ToDoList";

  // Header (MagicMirror style)
  const header = document.createElement("div");
  header.className = "module-header";
  header.textContent = this.config.header;
  wrapper.appendChild(header);

  if (!this.loaded) {
    const loading = document.createElement("div");
    loading.className = "dimmed light small";
    loading.innerHTML = "Loading ...";
    wrapper.appendChild(loading);
    return wrapper;
  }

  const ul = document.createElement("ul");
  ul.className = "todo-list";

  this.list.forEach((item, index) => {
    const li = document.createElement("li");

    // Fade like other modules
    if (this.config.fade && this.list.length > 0) {
      const startFade = this.list.length * this.config.fadePoint;
      if (index >= startFade) {
        const fadeSteps = this.list.length - startFade;
        li.style.opacity = 1 - (index - startFade) / fadeSteps;
      }
    }

    const title = document.createElement("span");
    title.textContent = item.title;
    li.appendChild(title);

    if (item.dueDate) {
      const due = document.createElement("span");
      due.className = "dueDate";
      due.textContent = `Due: ${item.dueDate}`;

      // 🔥 Determine urgency based on due date
      const now = new Date();
      const [day, month, year] = item.dueDate.split("/").map(Number);
      const dueDateObj = new Date(year, month - 1, day);
      const diffDays = Math.floor((dueDateObj - now) / (1000 * 60 * 60 * 24));

      if (diffDays < 0) due.classList.add("overdue");
      else if (diffDays < 1) due.classList.add("due-soon");

      li.appendChild(document.createElement("br"));
      li.appendChild(due);
    }

    ul.appendChild(li);
  });

  wrapper.appendChild(ul);
  return wrapper;
},


  getStyles() {
    return ["MMM-ToDoList.css"];
  }
});
