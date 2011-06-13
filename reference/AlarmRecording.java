package org.nilriri.LunaCalendar.tools;

import android.content.ContentValues;
import android.media.MediaRecorder;
import android.provider.MediaStore;

public class AlarmRecording {

    private MediaRecorder recorder;

    public void recording(String fname) {
        /* Create a MediaRecorder */
        recorder = new MediaRecorder();

        /* WhereClause2 include title, timestamp, mime type */
        ContentValues values = new ContentValues(3);
        values.put(MediaStore.MediaColumns.TITLE, fname);
        values.put(MediaStore.MediaColumns.DATE_ADDED, System.currentTimeMillis());

        //values.put(MediaStore.MediaColumns.MIME_TYPE,  recorder.get.getMimeContentType());   

        /* Create entry in the content database */
        //ContentResolver contentResolver = new ContentResolver();   
        //Uri base = MediaStore.Audio.INTERNAL_CONTENT_URI;   
        //Uri newUri = contentResolver.insert(base, values);   

        //if (newUri == null) {   
        /* Handle exception here - not able to create a new content entry */
        //}   

        /* Receive the real path as String */
        //String path = contentResolver.getDataFilePath(newUri);   

        /* Set Audio Source, Format, Encode and File */
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        //recorder.setOutputFile(path);   

        /* Prepare the Recorder */
        //recorder.prepare();   

        /* Start Recording */
        recorder.start();

        /* ... */

        /* Stop Recording Again */
        recorder.stop();
        recorder.release();

    }

}
