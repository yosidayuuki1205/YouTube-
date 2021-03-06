package download.utils;

import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.koushikdutta.ion.Ion;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Random;

import app.exploitr.nsg.youp3.R;

import static utils.L.getMimeType;

/**
 * Created by exploitr on 14-08-2017.
 * <p>
 * TODO
 */

class UpdateTask extends AsyncTask<String, String, String> {

    private Context mContext;
    private File file;
    private NotificationManager mNotifyManager;
    private String responseString, link;
    private JSONObject response;
    private int id;
    private Cursor cursor;
    private DownloadManager manager;
    private BroadcastReceiver afterDownload;


    UpdateTask(Context context) {
        this.mContext = context;
    }

    @Override
    protected void onPreExecute() {
        mNotifyManager = (NotificationManager) mContext.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(mContext);

        mBuilder.setContentTitle("Connecting..........")
                .setContentText("Obtaining url to update..")
                .setAutoCancel(false)
                .setDefaults(Notification.DEFAULT_ALL)
                .setSmallIcon(R.drawable.ic_file_download);
        mBuilder.setProgress(100, 0, false);

        PendingIntent notifyPIntent = PendingIntent.getActivity(mContext.getApplicationContext(), 0, new Intent(), 0);
        mBuilder.setContentIntent(notifyPIntent).setAutoCancel(true);
        mNotifyManager.notify(id, mBuilder.build());
        super.onPreExecute();
    }

    @Override
    protected String doInBackground(String... params) {

        id = new Random(111 - 999).nextInt();
        file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + "/YouP3" + System.currentTimeMillis() + ".apk");

        try {
            responseString = Ion.with(mContext).load("https://api.github.com/repos/exploitr/youp3/releases/latest").asString().get();
        } catch (Exception e) {
            e.printStackTrace();
            e.getSuppressed();

        }

        try {
            response = new JSONObject(responseString);
        } catch (JSONException e) {
            e.printStackTrace();
            e.getSuppressed();
        }

        try {
            JSONArray mainArray = response.getJSONArray("assets");
            response = mainArray.getJSONObject(0);
            link = response.getString("browser_download_url");
        } catch (Exception e) {
            e.printStackTrace();
            e.getSuppressed();

        }
        return link;
    }

    @Override
    protected void onPostExecute(String downloadLink) {

        mNotifyManager.cancelAll();

        afterDownload = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                    long enqueue = intent.getLongExtra(
                            DownloadManager.EXTRA_DOWNLOAD_ID, 0);
                    DownloadManager.Query query = new DownloadManager.Query();
                    query.setFilterById(enqueue);
                    cursor = manager.query(query);
                    if (cursor.moveToFirst()) {
                        int columnIndex = cursor
                                .getColumnIndex(DownloadManager.COLUMN_STATUS);
                        if (DownloadManager.STATUS_SUCCESSFUL == cursor.getInt(columnIndex)) {
                            try {
                                final Uri path = Uri.parse(cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)));
                                Intent indent = new Intent(Intent.ACTION_VIEW);
                                indent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                indent.setDataAndType(path, getMimeType(path.toString()));

                                mContext.startActivity(indent);
                            } catch (Exception ex) {
                                ex.getSuppressed();
                                ex.printStackTrace();
                                Crashlytics.logException(ex.getCause());
                                Toast.makeText(mContext, "Error While Launching Update Package, Please check Downloads folder manually", Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }
                    }
                }

                mContext.unregisterReceiver(afterDownload);
            }
        };

        mContext.registerReceiver(afterDownload, new IntentFilter(
                DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        manager = (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(link))
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
                .setDestinationUri(Uri.fromFile(file))
                .setTitle("Downloading Update");
        manager.enqueue(request);
    }
}
