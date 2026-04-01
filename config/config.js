let config = {
    address: "localhost",
    port: 8080,
    basePath: "/",
    ipWhitelist: ["127.0.0.1", "::ffff:127.0.0.1", "::1"],
    useHttps: false,
    httpsPrivateKey: "",
    httpsCertificate: "",

    language: "en",
    locale: "en-US",
    logLevel: ["INFO", "LOG", "WARN", "ERROR"],
    timeFormat: 24,
    units: "metric",

    modules: [
        // Fixed modules (always visible)
        { module: "clock", position: "top_left" },
        { module: "compliments", position: "lower_third" },
        { 
            module: "newsfeed", 
            position: "bottom_center", 
            config: { 
                feeds: [
                    { title: "New York Times", url: "https://rss.nytimes.com/services/xml/rss/nyt/HomePage.xml" }
                ],
                showSourceTitle: true,
                showPublishDate: true,
                broadcastNewsFeeds: true,
                broadcastNewsUpdates: true
            } 
        },
	{
  	    module: "MMM-Temperature",
  	    position: "bottom_right"
	},
	{
	    module: "MMM-Humidity",
	    position: "bottom_left"
	},

        // Page 0 modules
        {
            module: "calendar",
            header: "US Holidays",
            position: "top_left"
        },
        {
            module: "weather",
            position: "top_right",
            config: { weatherProvider: "openmeteo", type: "current", lat: 40.776676, lon: -73.971321 }
        },
        {
            module: "weather",
            position: "top_right",
            header: "Weather Forecast",
            config: { weatherProvider: "openmeteo", type: "forecast", lat: 40.776676, lon: -73.971321 }
        },

        // Page 1 modules
        { module: "MMM-ToDoList", position: "top_left", config: {} },
        { module: "MMM-RoutePlanner", position: "top_left", config: {} },
	{ module: "MMM-SmartCommute", position: "top_right", config: {} },
        { module: "MMM-Health", position: "top_right", config: {} },

        // Hidden bridges (can stay here; not on any page)
        { module: "MMM-WeatherBridge" },
        { module: "MMM-ComplimentsBridge" },

        // MMM-Pages controller
        {
            module: "MMM-pages",
            config: {
                modules: [
                    ["calendar", "weather", "weather"], // Page 0
                    ["MMM-ToDoList", "MMM-RoutePlanner", "MMM-Health", "MMM-SmartCommute"] // Page 1
                ],
                fixed: ["MMM-page-indicator","clock", "compliments", "newsfeed", "MMM-Temperature", 
			"MMM-Humidity"],
		homepage: 0
            }
        },

	{
  	  module: "MMM-Remote-Control",
  	  position: "bottom_left",
  	  config: {
    	  customCommand: {}
  	  }
	},

        {
            module: 'MMM-page-indicator',
            position: 'bottom_center',
            config: {
            	activeBright: true,
		style: "dots",
		maxPages: 10
            }
    	},
    ]
};

/*************** DO NOT EDIT THE LINE BELOW ***************/
if (typeof module !== "undefined") { module.exports = config; }
