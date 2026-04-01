const NodeHelper = require("node_helper");
const sensor = require("node-dht-sensor").promises;

module.exports = NodeHelper.create({
  start() {
    console.log("🔗 DHT11 helper started");
  },

  socketNotificationReceived(notification, payload) {
    if (notification === "START_DHT11") {
      if (!this.looping) {
        this.looping = true;
        this.loopSensor();
      }
    }
  },

  async loopSensor() {
    while (true) {
      try {
        const res = await sensor.read(11, 17); // DHT11, GPIO17
        this.sendSocketNotification("DHT11_UPDATE", {
          temperature: res.temperature,
          humidity: res.humidity
        });
      } catch (err) {
        console.error("Failed to read DHT11 sensor:", err);
      }
      await new Promise(r => setTimeout(r, 5000));
    }
  }
});
