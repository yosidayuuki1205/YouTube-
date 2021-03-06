package download.utils;

import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;

import com.koushikdutta.ion.Ion;


public class UpdateTaskChecker extends AsyncTask<Void, Void, Boolean> {

    private Context context;
    private Integer thisId;
    private String response;

    public UpdateTaskChecker(Context mContext) {
        this.context = mContext;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            thisId = pInfo.versionCode;
            System.out.print("version code " + thisId);
        } catch (Exception e) {
            e.printStackTrace();
            e.getSuppressed();
        }

        try {
            Thread.sleep(1000);
        } catch (Exception e) {
            e.printStackTrace();
            e.getSuppressed();
        }

        try {
            response = Ion.with(context).load("https://raw.githubusercontent.com/exploitr/YouP3/master/version.txt").asString().get();
        } catch (Exception e) {
            e.printStackTrace();
            e.getSuppressed();

        }

        try {
            Integer remoteId = Integer.parseInt(response.replaceAll("[^0123456789]", ""));
            return remoteId > thisId;
        } catch (Exception ex) {
            ex.getSuppressed();
            ex.printStackTrace();
        }

        return false;
    }

    @Override
    protected void onPostExecute(Boolean aBoolean) {

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context)
                .setTitle("Update Available")
                .setMessage("Update App?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new UpdateTask(context).execute();
                    }
                })
                .setNegativeButton("Nope", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        AlertDialog dialog = alertDialog.create();

        if (aBoolean) {
            dialog.show();
        }
        super.onPostExecute(aBoolean);
    }
}
