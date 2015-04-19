package com.example.objectrecognisation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.speech.tts.TextToSpeech;

public class MainActivity extends Activity implements 
			OnClickListener,TextToSpeech.OnInitListener {
	
	AlertDialogManager alert = new AlertDialogManager();
	private TextToSpeech textToSpeech;
	private static final String TAG = "upload";
	private ProgressDialog dialog = null;
	Bitmap bitmapsend;
	String responseBody="",mCurrentPhotoPath;;
	HttpResponse response = null;
	static final int REQUEST_TAKE_PHOTO = 1;
	File photoFile = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	    textToSpeech = new TextToSpeech(this,this);
		dispatchTakePictureIntent();
}
	
	public void onClick(View v) {
		int id = v.getId();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.i(TAG, "onActivityResult: " + this);
		if (requestCode == REQUEST_TAKE_PHOTO && resultCode == Activity.RESULT_OK) {
			Log.d("TAG","Here in ativity result");
			setPic();
		} else {
			finish();
		}
	}
	
	private class UploadTask extends AsyncTask<Bitmap, Void, Void> {
		protected Void doInBackground(Bitmap... bitmaps) {
			if (bitmaps[0] == null)
				return null;
			setProgress(0);
			
			Bitmap bitmap = bitmaps[0];
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream); // convert Bitmap to ByteArrayOutputStream
			InputStream in = new ByteArrayInputStream(stream.toByteArray()); // convert ByteArrayOutputStream to ByteArrayInputStream

			DefaultHttpClient httpclient = new DefaultHttpClient();
			try {
				HttpPost httppost = new HttpPost(
						"http://192.168.2.10:8081/MeterReadingServer/ImageUploadServlet"); // server

				MultipartEntity reqEntity = new MultipartEntity();
				reqEntity.addPart("main_image",
						System.currentTimeMillis() + ".jpg", in);
				httppost.setEntity(reqEntity);

				Log.i(TAG, "request " + httppost.getRequestLine());
				try {
					response = httpclient.execute(httppost);
					responseBody = EntityUtils.toString(response.getEntity());
				} catch (ClientProtocolException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				try {
					if (response != null)
						Log.i(TAG, "response " + response.getStatusLine().toString());
				} finally {

				}
			} finally {

			}

			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			return null;
		}
		
		@Override
		protected void onProgressUpdate(Void... values) {
			super.onProgressUpdate(values);
		}
		
		@Override
		protected void onPostExecute(Void result) {
			dialog.dismiss();
			super.onPostExecute(result);
			String str = responseBody;
			
			convertTextToSpeech();
			if(response!= null){
				//alert.showAlertDialog(MainActivity.this,"Detail is!","Sucessfull",false);
				dispatchTakePictureIntent();
			}
			else {
				//alert.showAlertDialog(MainActivity.this,"Detail is!","Fail",false);
				dispatchTakePictureIntent();
			}
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.i(TAG, "onResume: " + this);
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		Log.i(TAG, "onSaveInstanceState");
	}

	private void dispatchTakePictureIntent() {
	    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
	    // Ensure that there's a camera activity to handle the intent
	    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
	        // Create the File where the photo should go
	        File photoFile = null;
	        try {
	            photoFile = createImageFile();
	        } catch (IOException ex) {
	            // Error occurred while creating the File
	        	ex.printStackTrace();
	        }
	        // Continue only if the File was successfully created
	        if (photoFile != null) {
	            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
	            		Uri.fromFile(photoFile));
	            startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
	        }
	    }
	}

	private File createImageFile() throws IOException {
	    // Create an image file name
	    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
	    String imageFileName = "IMG_" + timeStamp + "_";
	    String storageDir = Environment.getExternalStorageDirectory() + "/picupload";
	    File dir = new File(storageDir);
	    if (!dir.exists())
	    	dir.mkdir();
	    
	    File image = new File(storageDir + "/" + imageFileName + ".jpg");

	    // Save a file: path for use with ACTION_VIEW intents
	    mCurrentPhotoPath = image.getAbsolutePath();
	    return image;
	}
	
	private void setPic() {


	    BitmapFactory.Options bmOptions = new BitmapFactory.Options();
	    bmOptions.inJustDecodeBounds = true;
	    BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);

	    int scaleFactor = 1;

	    // Decode the image file into a Bitmap sized to fill the View
	    bmOptions.inJustDecodeBounds = false;
	    bmOptions.inSampleSize = scaleFactor << 1;
	    bmOptions.inPurgeable = true;

	    bitmapsend = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
	    
	    if(bitmapsend == null){
	    	Log.d("Bitmap","Bitmap is null");
	    }
	    dialog = ProgressDialog.show(MainActivity.this, "", "Getting Detail...", true);
		new UploadTask().execute(bitmapsend);
}
	

	public void onInit(int status) {
		if (status == TextToSpeech.SUCCESS) {
			int result = textToSpeech.setLanguage(Locale.US);
			if (result == TextToSpeech.LANG_MISSING_DATA
					|| result == TextToSpeech.LANG_NOT_SUPPORTED) {
				Log.e("error", "This Language is not supported");
			} else {
				//convertTextToSpeech();
				Log.e("error", "Successfull!");
			}
		} else {
			Log.e("error", "Initilization Failed!");
		}
	}

	private void convertTextToSpeech() {
		String text = "Hello Vivek sir ";
		if (null == text || "".equals(text)) {
			text = "Take Pic.";
		}
		try {
			textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
