package weather;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import org.apache.commons.io.IOUtils;

import com.google.gson.Gson;

public class Weather {

	private Forecast forecast;

	public Weather(String location) throws IOException, URISyntaxException {
		URI uri = new URI("http", "api.openweathermap.org", "/data/2.5/forecast/daily", "q="
						+ location + "&units=metric&cnt=5", null);
		URL url = uri.toURL();
		URLConnection con = url.openConnection();
		InputStream in = con.getInputStream();
		String encoding = con.getContentEncoding();
		encoding = encoding == null ? "UTF-8" : encoding;
		String body = IOUtils.toString(in, encoding);

		forecast = new Gson().fromJson(body, Forecast.class);
	}

	public Forecast getForecast() {
		return forecast;
	}

	public class Forecast {
		public City city;
		public List<ForecastItem> list;
	}

	public class City {
		public int id;
		public String name;
		public String country;
		public int population;
	}

	public class ForecastItem {
		public int dt;
		public List<WeatherItem> weather;
		public Temperature temp;
	}

	public class WeatherItem {
		public int id;
		public String main;
		public String description;
		public String icon;
	}

	public class Temperature {
		public double day;
		public double min;
		public double max;
		public double night;
		public double eve;
		public double morn;
	}
}
