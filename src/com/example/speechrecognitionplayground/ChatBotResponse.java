package com.example.speechrecognitionplayground;

import android.os.AsyncTask;
import android.widget.ProgressBar;
import android.widget.TextView;

public class ChatBotResponse extends AsyncTask<String, Void, String> {
	@Override
	protected String doInBackground(String... args) {
		String text = args[0];
		String response;
		try {
			response = MainActivity.chatbot.replyTo(text);
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
		MainActivity.txtChatbotResult.setText(response);
		MainActivity.txtChatbotResult.setVisibility(TextView.VISIBLE);
		MainActivity.chatbotProgress.setVisibility(ProgressBar.INVISIBLE);
	}
}
