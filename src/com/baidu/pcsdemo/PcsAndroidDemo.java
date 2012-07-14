package com.baidu.pcsdemo;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.pcs.PcsClient;
import com.baidu.pcs.PcsFileEntry;
import com.baidu.pcs.oauth.OAuthSession;

public class PcsAndroidDemo extends Activity {
	final static private String TAG = "PcsAndroidDemo";

	final static private String CHANGE_ME = "CHANGE-ME";

	final static private String APP_KEY = "L6g70tBRRIXLsY0Z3HwKqlRE";
	final static private String APP_ROOT = "/apps/pcstest_oauth/"; // must
																	// endswith
																	// /
	private Button linkBtn;
	private Button upBtn;

	private Button captureBtn;
	private ImageView image;
	private TextView pwdText;

	private ListView fileListView;

	LinearLayout beforeLoggInDisplay;
	LinearLayout afterLoginDisplay;

	String cameraFilePath = "";
	private PcsClient pcsClient = null;
	private String pwd = APP_ROOT;
	List<PcsFileEntry> fileList = new ArrayList<PcsFileEntry>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		oauthSession = new OAuthSession(getApplicationContext());

		setContentView(R.layout.main);
		checkAppKeySetup();
		initUI();
		if (savedInstanceState != null) {
			cameraFilePath = savedInstanceState.getString("camera_file_path");
		}
		updateLoggedInStatus();
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.d(TAG, "PcsAndroidDemo onResume");
		if ((pcsClient == null) && (oauthSession.getAccessToken() != null)) {
			Log.d(TAG, "setToken:" + oauthSession.getAccessToken());
			String access_token = oauthSession.getAccessToken();
			storeAccessTokenToPreferences(access_token);
			updateLoggedInStatus();
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putString("camera_file_path", cameraFilePath);
		super.onSaveInstanceState(outState);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			boolean success = doUp();
			Log.d(TAG, "back back back back back back , success: " + success);
			if (!success) { // 如果当前是root, 直接退出.
				Log.d(TAG, "duUp return false");
				return super.onKeyDown(keyCode, event);
			} else {
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	private void setPwdAndUpdateList(String toDir) {
		pwd = toDir;
		if (!pwd.endsWith("/"))
			pwd = pwd + "/";
		Log.d(TAG, "set pwd :" + pwd);
		pwdText.setText(pwd);

		PcsFileEntry entry = new PcsFileEntry();
		entry.setPath(toDir);
		entry.setDir(true);
		new Downloader(this, pcsClient, entry).execute();
	}

	public void updateFileList(List<PcsFileEntry> lst) {
		this.fileList = lst;
		fileListAdapter.notifyDataSetChanged();
	}

	private String getParentFolder() {
		if (pwd.length() <= APP_KEY.length()) {
			return APP_ROOT;
		}
		String tmp = null;
		tmp = pwd.substring(0, pwd.length() - 1);
		return pwd.substring(0, tmp.lastIndexOf('/'));
	}

	private boolean doUp() {
		if (pwd.equals(APP_ROOT)) {
			Toast.makeText(getApplicationContext(), "already root, no Up !", Toast.LENGTH_LONG).show();
			return false;
		} else {
			setPwdAndUpdateList(getParentFolder());
			return true;
		}
	}

	public void refresh() {
		setPwdAndUpdateList(pwd);
	}

	private class FileListAdapter extends BaseAdapter {
		@Override
		public int getCount() {
			return fileList.size();
		}

		@Override
		public Object getItem(int position) {
			return fileList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = null;
			if (convertView == null) {
				v = createView(position, convertView, parent);
			} else {
				v = convertView;
			}

			TextView tv = (TextView) v.findViewById(R.id.list_item_text);
			tv.setText(fileList.get(position).getServerFilename());

			ImageView iv = (ImageView) v.findViewById(R.id.list_item_image);
			if (fileList.get(position).isDir()) {
				iv.setImageResource(R.drawable.list_icon_folder);
			} else {
				iv.setImageResource(R.drawable.list_icon_file);
			}
			return v;
		}

		private View createView(int position, View convertView, ViewGroup parent) {
			Context context = getApplicationContext();
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View v = inflater.inflate(R.layout.list_item, parent, false);
			// TextView tv = (TextView) v.findViewById(android.R.id.text1);
			return v;
		}
	}

	FileListAdapter fileListAdapter;
	OAuthSession oauthSession = null;

	private void initUI() {
		linkBtn = (Button) findViewById(R.id.link_button);
		linkBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				// mApi.getSession().startAuthentication(DBRoulette.this);
				oauthSession.startAuth(APP_KEY);
			}
		});

		upBtn = (Button) findViewById(R.id.up_button);
		upBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				doUp();
			}
		});
		findViewById(R.id.capture_button).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				beginCapture();
			}
		});
		findViewById(R.id.select_file_button).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				beginSelectFile();
			}
		});
		findViewById(R.id.mkdir_button).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				beginMakedir();
			}
		});

		pwdText = (TextView) findViewById(R.id.pwd_text);

		beforeLoggInDisplay = (LinearLayout) findViewById(R.id.before_login_display);
		afterLoginDisplay = (LinearLayout) findViewById(R.id.after_login_display);

		fileListAdapter = new FileListAdapter();
		fileListView = (ListView) findViewById(R.id.pcs_file_list);
		fileListView.setAdapter(fileListAdapter);
		fileListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> listView, View view, int pos, long id) {
				Log.d(TAG, "click" + pos);

				if (fileList.get(pos).isDir()) {
					pwd = fileList.get(pos).getPath();
					setPwdAndUpdateList(pwd);
				} else {
					new Downloader(PcsAndroidDemo.this, pcsClient, fileList.get(pos)).execute();
				}
			}
		});
	}

	final static private int ACTION_NEW_CAPTURE = 1;
	final static private int ACTION_SELECT_FILE = 2;

	private void beginMakedir() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("mkdir");
		alert.setMessage("please input dir name");

		final EditText inputName = new EditText(this);
		inputName.setText(pwd);
		alert.setView(inputName);

		alert.setPositiveButton("创建", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String gotDirFullPath = inputName.getText().toString();
				Log.d(TAG, "got dir name:" + gotDirFullPath);
				new Uploader(PcsAndroidDemo.this, pcsClient, null, null, gotDirFullPath).execute();
			}
		});
		alert.show();
	}

	private void beginSelectFile() {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("*/*");
		startActivityForResult(Intent.createChooser(intent, "Select Picture"), ACTION_SELECT_FILE);
	}

	private void beginCapture() {
		Intent intent = new Intent();
		// Picture from camera
		intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
		Date date = new Date();
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd-kk-mm-ss");

		String newPicFile = df.format(date) + ".jpg";
		cameraFilePath = "/sdcard/" + newPicFile;
		File outFile = new File(cameraFilePath);

		Uri outuri = Uri.fromFile(outFile);
		intent.putExtra(MediaStore.EXTRA_OUTPUT, outuri);
		Log.i(TAG, "Take a photo : " + cameraFilePath);
		try {
			startActivityForResult(intent, ACTION_NEW_CAPTURE);
		} catch (ActivityNotFoundException e) {
			Toast.makeText(this, "There doesn't seem we have no camera.", Toast.LENGTH_LONG).show();
		}
	}

	// This is what gets called on finishing a media piece to import
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != Activity.RESULT_OK) {
			Log.w(TAG, "Unknown Activity Result : " + resultCode);
			return;
		}
		String gotFilePath = "";
		if (requestCode == ACTION_NEW_CAPTURE) {
			gotFilePath = cameraFilePath;
		} else if (requestCode == ACTION_SELECT_FILE) {
			Uri selectedImageUri = data.getData();
			gotFilePath = selectedImageUri.getPath();
		}
		String remoteName = new File(gotFilePath).getName();
		// pcsClient.uploadFile(cameraFilePath, pwd, remoteName);
		new Uploader(this, pcsClient, gotFilePath, pwd, remoteName).execute();
	}

	private void updateLoggedInStatus() {
		boolean loggedIn = false;
		String access_token = getAccessTokenFromPreferences();
		if (access_token != null) {
			pcsClient = new PcsClient(access_token, APP_ROOT);
			beforeLoggInDisplay.setVisibility(View.GONE);
			afterLoginDisplay.setVisibility(View.VISIBLE);
			setPwdAndUpdateList(APP_ROOT);
		} else {
			beforeLoggInDisplay.setVisibility(View.VISIBLE);
			afterLoginDisplay.setVisibility(View.GONE);
		}
	}

	private void checkAppKeySetup() {
		if (APP_KEY.equals(CHANGE_ME) || APP_ROOT.equals(CHANGE_ME)) {
			Toast.makeText(this, "set APP_KEY, APP_SECRET first", Toast.LENGTH_LONG).show();
			finish();
			return;
		}

		// 测试应用是否设置了正确的 intent-filter, pcs-APP_KEY
		Intent testIntent = new Intent(Intent.ACTION_VIEW);
		String scheme = "pcs-" + APP_KEY.toLowerCase();
		String uri = scheme + "://" + "pcs.baidu.com" + "/test";
		testIntent.setData(Uri.parse(uri));
		PackageManager pm = getPackageManager();
		if (0 == pm.queryIntentActivities(testIntent, 0).size()) {
			Toast.makeText(this, "请在AndroidManifest.xml 中设置 intent-filter " + scheme, Toast.LENGTH_LONG).show();
			finish();
		}
	}

	private static String PREFERENCES_NAME = "PREFERENCES_NAME";
	private static String PREFERENCES_ACCESSTOKEN = "PREFERENCES_ACCESSTOKEN";

	private String getAccessTokenFromPreferences() {
		SharedPreferences prefs = getSharedPreferences(PREFERENCES_NAME, 0);
		String access_token = prefs.getString(PREFERENCES_ACCESSTOKEN, null);
		return access_token;
	}

	private void storeAccessTokenToPreferences(String access_token) {
		SharedPreferences prefs = getSharedPreferences(PREFERENCES_NAME, 0);
		Editor edit = prefs.edit();
		edit.putString(PREFERENCES_ACCESSTOKEN, access_token);
		edit.commit();
	}

	private void clearAccessToken() {
		SharedPreferences prefs = getSharedPreferences(PREFERENCES_NAME, 0);
		Editor edit = prefs.edit();
		edit.clear();
		edit.commit();
	}
}