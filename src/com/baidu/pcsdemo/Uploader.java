package com.baidu.pcsdemo;

import java.util.List;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.baidu.pcs.PcsClient;
import com.baidu.pcs.PcsFileEntry;
import com.baidu.pcs.exception.PcsException;

public class Uploader extends AsyncTask<Void, Long, Boolean> {
	final static private String TAG = "PcsAndroidDemo";
	private PcsAndroidDemo instance;
	private final ProgressDialog mDialog;
	private PcsClient pcsClient;

	private String localPath;
	private String remoteDir;
	private String remoteName;

	private List<PcsFileEntry> fileList;

	public Uploader(PcsAndroidDemo context, PcsClient pcsClient, String localPath, String remoteDir, String remoteName) {
		instance = context;
		this.pcsClient = pcsClient;
		this.localPath = localPath;
		this.remoteDir = remoteDir;
		this.remoteName = remoteName;
		mDialog = new ProgressDialog(context);
		mDialog.setMessage("uploading ..");
		mDialog.show();
	}

	String errmsg = "";

	@Override
	protected Boolean doInBackground(Void... params) {
		try {
			if (null != localPath) {
				Log.i(TAG, "Upload " + localPath + " to " + remoteDir + "/" + remoteName);
				pcsClient.uploadFile(localPath, remoteDir, remoteName);
				return true;
			}else {
				Log.i(TAG, "mkdir " + remoteName);
				pcsClient.mkdir(remoteName);
				return true;
			}
		} catch (PcsException e) {
			e.printStackTrace();
			errmsg = "error on upload " + e.toString();
			return false;
		}
	}

	@Override
	protected void onPostExecute(Boolean result) {
		mDialog.dismiss();
		if (!result) {
			Toast.makeText(instance, errmsg, Toast.LENGTH_LONG).show();
			return;
		}
		instance.refresh();
	}
}
