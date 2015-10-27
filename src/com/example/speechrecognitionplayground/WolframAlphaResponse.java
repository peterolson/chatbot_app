package com.example.speechrecognitionplayground;

import movie_search.MatchScore;
import android.os.AsyncTask;
import android.text.Html;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.koushikdutta.async.future.FutureCallback;
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
	private static final double THRESHOLD = 0.3;
	private String input;
	@Override
	protected WAQueryResult doInBackground(String... params) {
		WAEngine engine = new WAEngine();
		engine.setAppID(APP_ID);
		engine.addFormat("plaintext");
		engine.addFormat("image");
		//engine.setAsync(0);

		WAQuery query = engine.createQuery();
		query.setMagnification(1.5);
		query.setWidth(600);
		input = params[0];
		query.setInput(input);
		try {
			return engine.performQuery(query);

		} catch (WAException e) {
			return null;
		}
	}

	@Override
	protected void onPostExecute(WAQueryResult result) {
		MainActivity.wolframProgress.setVisibility(ProgressBar.INVISIBLE);
		if(!canShowResult(result)) {
			MainActivity.showChatResponse(input);
			return;
		}
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
						final ProgressBar progressBar = new ProgressBar(MainActivity.context, null, android.R.attr.progressBarStyleSmall);
						MainActivity.wolframContainer.addView(imageView);
						MainActivity.wolframContainer.addView(progressBar);
						Ion.with(imageView)
							.fadeIn(true)
							.placeholder(R.drawable.loader)
							.load(img.getURL())
							.setCallback(new FutureCallback<ImageView>() {
								@Override
								public void onCompleted(Exception arg0, ImageView arg1) {
									MainActivity.wolframContainer.removeView(progressBar);
								}
							});
					}
				}
			}
		}
	}
	
	private boolean canShowResult(WAQueryResult result) {
		for(WAPod pod : result.getPods()) {
			if(pod.isError()) {
				continue;
			}
			if(pod.getID().equals("Input")) {
				for(WASubpod subpod : pod.getSubpods()) {
					for(Object obj : subpod.getContents()) {
						if(obj instanceof WAPlainText) {
							String inputInterpretation = ((WAPlainText)obj).getText();
							double score = MatchScore.Calculate(input, inputInterpretation);
							if(score >= THRESHOLD) {
								return true;
							}
							MainActivity.txtChatbotResult.setText(inputInterpretation + " " + score);
							return false;
						}
					}
				}
				break;
			}
		}
		return false;
	}

}