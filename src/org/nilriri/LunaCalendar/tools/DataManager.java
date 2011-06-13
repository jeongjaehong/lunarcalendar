package org.nilriri.LunaCalendar.tools;

import org.nilriri.LunaCalendar.dao.ScheduleDaoImpl;

import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

public class DataManager {
    public static ScheduleDaoImpl daoSource;
    public static ScheduleDaoImpl daoTarget;
    public static ProgressDialog pd;
    public static String Error = "";

    public static Context mContext;

    public static void StartCopy(Context context, boolean work) {
        mContext = context;

        // work = true  : 내부메모리 => 외부메모리
        //        false : 외부메모리 => 내부메모리  
        Log.d(Common.TAG, "==========StartCopy=========" + work);
        daoSource = new ScheduleDaoImpl(mContext, null, !work);
        daoTarget = new ScheduleDaoImpl(mContext, null, work);

        pd = new ProgressDialog(mContext);

        pd.setTitle("Move!");
        pd.setMessage("Move to external storage...");

        //pd.setCancelable(true);
        pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        //pd.setIndeterminate(true);
        pd.show();

        new Thread(new Runnable() {
            public void run() {
                Cursor cursor = daoSource.queryAll();
                try {

                    if (!daoSource.exportdata(cursor, pd)) {
                        Error = "백업이 실패하였습니다.";
                    } else {

                        if (cursor.getCount() > 0) {
                            if (!daoTarget.copy(cursor))
                                Error = "저장위치 변경 실패!";
                        }
                    }

                    handler.sendEmptyMessage(0);
                } catch (Exception e) {
                    handler.sendEmptyMessage(0);
                    Error = e.getMessage();

                } finally {
                    cursor.close();
                    daoSource.close();
                    daoTarget.close();
                }
            }
        }).start();
    }

    public static void StartRestore(Context context, final String backupFile) {
        mContext = context;

        daoTarget = new ScheduleDaoImpl(mContext, null, Prefs.getSDCardUse(context));

        pd = new ProgressDialog(mContext);

        pd.setTitle("Restore!");
        pd.setMessage("Restore from backup file...");

        pd.show();
        new Thread(new Runnable() {
            public void run() {
                try {
                    if (!daoTarget.importdata(backupFile)) {
                        Error = "복구 실패!";
                    } else {
                        Error = "";
                    }
                    daoTarget.close();

                    handler.sendEmptyMessage(0);

                } catch (Exception e) {
                    handler.sendEmptyMessage(0);
                    Error = e.getMessage();
                }
            }
        }).start();
    }

    public static void StartBackup(Context context) {
        mContext = context;

        daoSource = new ScheduleDaoImpl(mContext, null, Prefs.getSDCardUse(context));

        pd = new ProgressDialog(mContext);

        pd.setTitle("Backup!");
        pd.setMessage("Backup schedule data...");

        //pd.setCancelable(true);
        pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);

        //pd.setIndeterminate(true);
        pd.show();

        new Thread(new Runnable() {
            public void run() {
                try {
                    Cursor cursor = daoSource.queryAll();
                    if (!daoSource.exportdata(cursor, pd)) {
                        Error = "백업이 실패하였습니다.";
                    } else {
                        Error = "";
                    }
                    cursor.close();
                    daoSource.close();
                    handler.sendEmptyMessage(0);

                } catch (Exception e) {
                    handler.sendEmptyMessage(0);
                    Error = e.getMessage();
                }
            }
        }).start();
    }

    public static Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            pd.dismiss();

            if ("".equals(Error)) {
                Toast.makeText(mContext, "작업이 정상적으로 완료 되었습니다.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(mContext, Error, Toast.LENGTH_LONG).show();
            }
        }
    };

}