const fs = require("fs");
const path = require("path");

class PersistedStateHelper {
  /**
   * @param {string} modulePath - module folder path (__dirname from module)
   * @param {string} fileName - optional, defaults to "state.json"
   */
  constructor(modulePath, fileName = "state.json") {
    this.filePath = path.join(modulePath, fileName);
    this.state = {};

    // Load existing state if file exists
    if (fs.existsSync(this.filePath)) {
      try {
        const saved = fs.readFileSync(this.filePath, "utf8");
        this.state = JSON.parse(saved);
      } catch (e) {
        console.error("Failed to load persisted state:", e);
        this.state = {};
      }
    }
  }

  get(key) {
    return key ? this.state[key] : this.state;
  }

  set(key, value) {
    this.state[key] = value;
    this.save();
  }

  merge(payload) {
    this.state = { ...this.state, ...payload };
    this.save();
  }

  save() {
    try {
      fs.writeFileSync(this.filePath, JSON.stringify(this.state, null, 2));
    } catch (e) {
      console.error("Failed to persist state:", e);
    }
  }
}

module.exports = PersistedStateHelper;
