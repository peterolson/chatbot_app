package com.example.speechrecognitionplayground;

import android.os.AsyncTask;
import android.text.Html;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.wolfram.alpha.WAEngine;
import com.wolfram.alpha.WAException;
import com.wolfram.alpha.WAImage;
import com.wolfram.alpha.WAPlainText;
import com.wolfram.alpha.WAPod;
import com.wolfram.alpha.WAQuery;
import com.wolfram.alpha.WAQueryResult;
import com.wolfram.alpha.WASubpod;

public class WolframAlphaResponse extends AsyncTask<String, Void, String> {
	private static String APP_ID = "GP2JJR-JYHKU592H2";
	
	@Override
	protected String doInBackground(String... params) {
		WAEngine engine = new WAEngine();
		engine.setAppID(APP_ID);
		engine.addFormat("plaintext");
		// engine.addFormat("image");
		
		WAQuery query = engine.createQuery();
		query.setInput(params[0]);
		try {
			WAQueryResult queryResult = engine.performQuery(query);
			StringBuilder html = new StringBuilder();
			for (WAPod pod : queryResult.getPods()) {
				if (pod.isError())
					continue;
				html.append("<b>" + pod.getTitle() + "</b><br>");
				for (WASubpod subpod : pod.getSubpods()) {
					for (Object o : subpod.getContents()) {
						if (o instanceof WAImage) {
							WAImage img = (WAImage) o;
							html.append("<img src=\"" + img.getURL() + "\">");
						}
						if (o instanceof WAPlainText) {
							WAPlainText txt = (WAPlainText) o;
							html.append(txt.getText() + "<br>");
						}
					}
				}
			}
			return html.toString();
		} catch (WAException e) {
			return "";
		}
	}

	@Override
	protected void onPostExecute(String result) {
		MainActivity.txtWolframAlpha.setText(Html.fromHtml(result, null, null));
		MainActivity.wolframProgress.setVisibility(ProgressBar.INVISIBLE);
		MainActivity.txtWolframAlpha.setVisibility(TextView.VISIBLE);
	}

}