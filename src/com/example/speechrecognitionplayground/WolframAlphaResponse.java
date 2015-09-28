package com.example.speechrecognitionplayground;

import android.os.AsyncTask;
import android.text.Html;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.koushikdutta.ion.Ion;
import com.wolfram.alpha.WAEngine;
import com.wolfram.alpha.WAException;
import com.wolfram.alpha.WAImage;
import com.wolfram.alpha.WAPlainText;
import com.wolfram.alpha.WAPod;
import com.wolfram.alpha.WAQuery;
import com.wolfram.alpha.WAQueryResult;
import com.wolfram.alpha.WASubpod;

public class WolframAlphaResponse
		extends
			AsyncTask<String, Void, WAQueryResult> {
	private static String APP_ID = "GP2JJR-JYHKU592H2";

	@Override
	protected WAQueryResult doInBackground(String... params) {
		WAEngine engine = new WAEngine();
		engine.setAppID(APP_ID);
		//engine.addFormat("plaintext");
		engine.addFormat("image");

		WAQuery query = engine.createQuery();
		query.setMagnification(1.5);
		query.setWidth(600);
		query.setInput(params[0]);
		try {
			return engine.performQuery(query);

		} catch (WAException e) {
			return null;
		}
	}

	@Override
	protected void onPostExecute(WAQueryResult result) {
		MainActivity.wolframProgress.setVisibility(ProgressBar.INVISIBLE);
		for (WAPod pod : result.getPods()) {
			if (pod.isError())
				continue;
			String title = pod.getTitle();
			TextView titleView = new TextView(MainActivity.context);
			titleView.setText(Html.fromHtml("<b>" + title + "</b>"));
			MainActivity.wolframContainer.addView(titleView);
			for (WASubpod subpod : pod.getSubpods()) {
				for (Object obj : subpod.getContents()) {
					if (obj instanceof WAImage) {
						WAImage img = (WAImage) obj;
						ImageView imageView = new ImageView(
								MainActivity.context);
						MainActivity.wolframContainer.addView(imageView);
						Ion.with(imageView)
							.fadeIn(true)
							.placeholder(R.drawable.loader)
							.load(img.getURL());
					} else if (obj instanceof WAPlainText) {
						WAPlainText txt = (WAPlainText) obj;
						TextView textView = new TextView(MainActivity.context);
						textView.setText(txt.getText());
						MainActivity.wolframProgress
								.setVisibility(ProgressBar.INVISIBLE);
						MainActivity.wolframContainer.addView(textView);
					}
				}
			}
		}
	}

}