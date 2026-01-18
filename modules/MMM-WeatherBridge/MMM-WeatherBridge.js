Module.register("MMM-WeatherBridge", {
  start() {
    console.log("🌉 MMM-WeatherBridge frontend starting...");

    this.ready = false;
    this.queue = [];

    this.sendSocketNotification("PING_HELPER", null);


    // Give MM a few seconds to load all modules
    setTimeout(() => {
      this.ready = true;
      console.log("✅ WeatherBridge frontend ready, flushing queue");

      this.queue.forEach((item) => {
        this._handleNotification(item.notification, item.payload);
      });
      this.queue = [];
    }, 3000);
  },

   socketNotificationReceived(notification, payload) {
       console.log("🔥 FRONTEND received socketNotification:", notification, payload);

       // Keep your existing handling
       if (!this.ready) {
           console.log("⏳ Frontend not ready, queueing payload:", payload);
           this.queue.push({ notification, payload });
           return;
       }
       this._handleNotification(notification, payload);
   },

  _handleNotification(notification, payload) {
    console.log("📡 Frontend handling socketNotification:", notification, payload);

    if (notification === "WEATHER_LOCATION_UPDATE") {
      console.log("🌍 Rebroadcasting WEATHER_LOCATION_UPDATE to modules:", payload);

      const forwardPayload = {};
      if (payload.lat != null && payload.lon != null) forwardPayload.lat = payload.lat;
      if (payload.lat != null && payload.lon != null) forwardPayload.lon = payload.lon;
      if (payload.location) forwardPayload.location = payload.location;

      console.log("📡 Sending WEATHER_LOCATION_UPDATE payload:", forwardPayload);
      this.sendNotification("WEATHER_LOCATION_UPDATE", forwardPayload);
    }
  }
});
