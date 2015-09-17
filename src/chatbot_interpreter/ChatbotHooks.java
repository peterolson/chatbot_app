package chatbot_interpreter;

import java.util.Calendar;
import java.util.Random;

import weather.Weather;
import weather.Weather.Forecast;
import weather.Weather.ForecastItem;
import weather.Weather.WeatherItem;

import android.app.Activity;
import android.graphics.Color;
import android.view.View;

public class ChatbotHooks {
	public static View view;
	public static Activity activity;
	
	public static enum Hook {
		changeColor {
			public String run(String[] arguments) {
				activity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Random r = new Random();
						view.setBackgroundColor(Color.rgb(r.nextInt(256), r.nextInt(256), r.nextInt(256)));
					}
				});
				return "Changing background to a random color...";
			}
		},
		location {
			public String run(String[] arguments) {
				return "Qingdao";
			}
		},
		weather {
			public String run(String[] arguments) {
				String city = "";
				try {
					if(arguments.length < 1 || (city = arguments[0]).trim().isEmpty()) { 
						city = Hook.location.run(arguments);
					}
					Forecast f = new Weather(city).getForecast();
					StringBuilder descriptionBuilder = new StringBuilder();
					descriptionBuilder.append("Weather forecast for "
							+ f.city.name + ", " + f.city.country + ":\n");
					String[] days = new String[] { "Sunday", "Monday",
							"Tuesday", "Wednesday", "Thursday", "Friday", "Saturday" };
					int dayIndex = Calendar.getInstance().get(
							Calendar.DAY_OF_WEEK) - 1;
					for (int i = 0; i < f.list.size(); i++) {
						ForecastItem forecastItem = f.list.get(i);
						String dayName = i == 0 ? "Today" : i == 1 ? "Tomorrow"
								: days[(dayIndex + i) % 7];
						WeatherItem weatherItem = forecastItem.weather.get(0);
						descriptionBuilder.append("\n" + dayName + ": \n"
								+ weatherItem.description
								+ "\nMin: " + forecastItem.temp.min
								+ "°C, Max: " + forecastItem.temp.max + "°C\n");
					}
					return descriptionBuilder.toString();
				} catch (Exception e) {
					return "Could not get weather in '" + city + "'.";
				}

			}
		};
		abstract public String run(String[] arguments);
	}
}
