package com.baidu.pcsdemo;

import java.io.File;
import java.util.List;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.baidu.pcs.PcsClient;
import com.baidu.pcs.PcsFileEntry;
import com.baidu.pcs.exception.PcsException;

public class Downloader extends AsyncTask<Void, Long, Boolean> {
	final static private String TAG = "PcsAndroidDemo";
	private PcsAndroidDemo instance;
	private final ProgressDialog mDialog;
	private PcsClient pcsClient;

	private PcsFileEntry pcsFileEntry;
	private String cachePath;

	private List<PcsFileEntry> fileList;

	public Downloader(PcsAndroidDemo context, PcsClient pcsClient, PcsFileEntry pcsFileEntry) {
		instance = context;
		this.pcsClient = pcsClient;
		this.pcsFileEntry = pcsFileEntry;
		mDialog = new ProgressDialog(context);
		if (pcsFileEntry.isDir()){
			mDialog.setMessage("update dir " + pcsFileEntry.getPath());
		}else{
			mDialog.setMessage("Downloading " + pcsFileEntry.getPath());
		}
		mDialog.show();
	}

	private boolean isImage(String path) {
		if (path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".jpeg") || path.endsWith(".bmp"))
			return true;
		return false;
	}

	String errmsg = "";
	@Override
	protected Boolean doInBackground(Void... params) {
		String remotePath = pcsFileEntry.getPath();
		try {
			Log.i(TAG, "Get " + pcsFileEntry.getPath() );
			if (pcsFileEntry.isDir()) {
				fileList = pcsClient.list(pcsFileEntry.getPath());
				return true;
			} else if (!isImage(remotePath)) {
				errmsg = "not a image !";
				return false;
			} else {
				String tmpName = remotePath.replace('/', '_');
				cachePath = "/sdcard/" + tmpName;
				pcsClient.downloadToFile(remotePath, cachePath);
				return true;
			}
		} catch (PcsException e) {
			e.printStackTrace();
			errmsg = "error when get " + remotePath + " " + e.toString();
			return false;
		}
	}

	@Override
	protected void onPostExecute(Boolean result) {
		mDialog.dismiss();
		if (!result) {
			Toast.makeText(instance, errmsg, Toast.LENGTH_SHORT).show();
			return;
		} 
		if (pcsFileEntry.isDir()) {
			instance.updateFileList(fileList);
		} else {
			Intent intent = new Intent();
			intent.setAction(android.content.Intent.ACTION_VIEW);
			File file = new File(cachePath);
			Log.d(TAG, "opening file: " + cachePath);
			intent.setDataAndType(Uri.fromFile(file), "image/jpeg");
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			instance.startActivity(intent);
		}

	}
}
