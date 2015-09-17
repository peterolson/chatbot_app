package com.example.speechrecognitionplayground;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;

import org.apache.commons.io.IOUtils;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import chatbot_interpreter.Chatbot;
import chatbot_interpreter.ChatbotHooks;

import com.baidu.voicerecognition.android.VoiceRecognitionConfig;
import com.baidu.voicerecognition.android.ui.BaiduASRDigitalDialog;
import com.baidu.voicerecognition.android.ui.DialogRecognitionListener;

public class MainActivity extends Activity {

	private BaiduASRDigitalDialog mDialog;
	private DialogRecognitionListener mRecognitionListener;
	public static TextView txtChatbotResult, txtRecognized, txtWolframAlpha;
	public static ProgressBar chatbotProgress, wolframProgress;
	private Chatbot chatbot;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Toast.makeText(this, "Loading chatbot...", Toast.LENGTH_LONG).show();

		txtChatbotResult = (TextView) findViewById(R.id.txtChatbotResult);
		txtRecognized = (TextView) findViewById(R.id.txtRecognized);
		txtWolframAlpha = (TextView) findViewById(R.id.txtWolframResult);
		
		chatbotProgress = (ProgressBar) findViewById(R.id.chatbotProgress);
		chatbotProgress.setVisibility(ProgressBar.INVISIBLE);
		wolframProgress = (ProgressBar) findViewById(R.id.wolframProgress);
		wolframProgress.setVisibility(ProgressBar.INVISIBLE);

		Resources res = this.getResources();
		InputStream stream = res.openRawResource(R.raw.data);
		ChatbotHooks.view = findViewById(R.id.lytMain);
		ChatbotHooks.activity = this;
		try {
			String s = IOUtils.toString(stream);
			chatbot = new Chatbot(s);
		} catch (Exception e) {
			e.printStackTrace();
		}

		Bundle params = new Bundle();
		params.putString(BaiduASRDigitalDialog.PARAM_API_KEY,
				"iEpfSpqMCDeOuZZVDEYik07M");
		params.putString(BaiduASRDigitalDialog.PARAM_SECRET_KEY,
				"a2409e668117a89c270b61cbb8de2aec");
		params.putInt(BaiduASRDigitalDialog.PARAM_PROP,
				VoiceRecognitionConfig.PROP_INPUT);
		params.putString(BaiduASRDigitalDialog.PARAM_LANGUAGE,
				VoiceRecognitionConfig.LANGUAGE_ENGLISH);
		params.putInt(BaiduASRDigitalDialog.PARAM_DIALOG_THEME,
				BaiduASRDigitalDialog.THEME_RED_DEEPBG);
		mDialog = new BaiduASRDigitalDialog(this, params);

//		Intent checkIntent = new Intent();
//		checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
//		startActivityForResult(checkIntent, MY_DATA_CHECK_CODE);

		init();
	}

//	private TextToSpeech tts;
//	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//		if (requestCode == MY_DATA_CHECK_CODE) {
//			if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
//				// success, create the TTS instance
//				tts = new TextToSpeech(this, new OnInitListener() {
//					@Override
//					public void onInit(int arg0) {
//						tts.setLanguage(Locale.US);
//						init();
//					}
//				});
//			} else {
//				// missing data, install it
//				Intent installIntent = new Intent();
//				installIntent
//						.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
//				startActivity(installIntent);
//			}
//		}
//	}

	private ChatBotResponse lastChatResponse = null;
	private WolframAlphaResponse lastWolframResponse = null;

	private void init() {
		mRecognitionListener = new DialogRecognitionListener() {
			@Override
			public void onResults(Bundle results) {
				ArrayList<String> rs = results != null ? results
						.getStringArrayList(RESULTS_RECOGNITION) : null;
				if (rs == null || rs.size() < 1) {
					return;
				}
				String text = rs.get(0);
				txtRecognized.setText(text);
				
				chatbotProgress.setVisibility(ProgressBar.VISIBLE);
				txtChatbotResult.setVisibility(TextView.INVISIBLE);
				
				if (lastChatResponse != null) {
					lastChatResponse.cancel(true);
				}
				lastChatResponse = new ChatBotResponse();
				lastChatResponse.execute(text);

				wolframProgress.setVisibility(ProgressBar.VISIBLE);
				txtWolframAlpha.setVisibility(TextView.INVISIBLE);
				
				if (lastWolframResponse != null) {
					lastWolframResponse.cancel(true);
				}
				lastWolframResponse = new WolframAlphaResponse();
				lastWolframResponse.execute(text);
			}
		};
		mDialog.setDialogRecognitionListener(mRecognitionListener);
		Toast.makeText(this, "Loaded!", Toast.LENGTH_LONG).show();
	}

	private class ChatBotResponse extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... args) {
			String text = args[0];
			String response;
			try {
				response = chatbot.replyTo(text);
				return response;
			} catch (Exception e) {
				e.getStackTrace();
			}
			return "Could not get response. :(";
		}

		@Override
		protected void onPostExecute(String response) {
			// tts.stop(); 
			// tts.speak(response, TextToSpeech.QUEUE_FLUSH, null);
			txtChatbotResult.setText(response);
			txtChatbotResult.setVisibility(TextView.VISIBLE);
			chatbotProgress.setVisibility(ProgressBar.INVISIBLE);
		}
	}

	public void btnSpeak_click(View v) {

		mDialog.show();
	}
}