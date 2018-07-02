package com.example.codeworld.gridviewexample;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.FileNotFoundException;
import java.util.ArrayList;

/**
 * Created by rohitsingla on 28/06/18.
 */

public class DeleteImagesAsyncTask extends AsyncTask< ArrayList<Uri>, Integer,Integer > {
    private static final String TAG = "RDEEPU";
    private TextRecognizer detector;

    private Context mContext;

    // reference to mainHandler from the mainThread
    private Handler parentHandler;

    public DeleteImagesAsyncTask (Context context, Handler handler){
        mContext = context;
        this.parentHandler = handler;
    }


    @Override
    protected Integer doInBackground(ArrayList<Uri>... uris) {
        detector = new TextRecognizer.Builder(mContext).build();

        int n = uris[0].size();
        ArrayList<Integer> mSelectedImages = new ArrayList<>();
        Log.d(TAG,"Inside async task and processing "+n+" images");
        for(int i=0;i<n;i++){
            String path;
            if(android.os.Build.VERSION.SDK_INT<11)
                path = RealPathUtil.getRealPathFromURI_BelowAPI11(mContext, uris[0].get(i));
            else if(Build.VERSION.SDK_INT<=18)
                path = RealPathUtil.getRealPathFromURI_API11to18(mContext, uris[0].get(i));
            else {
                String brandName = Build.BRAND;
                Log.d(TAG, "brandName = "+brandName);
                if(brandName.equalsIgnoreCase("redmi") || brandName.equalsIgnoreCase("xiaomi")){
                    //For Redmi note 4
                    path = uris[0].get(i).getPath();
                    Log.d(TAG, "uri path: "+path);
                    RealPathUtil.deleteFileFromPathFromAPI19(mContext, path.substring(5));
                }else {
                    path = RealPathUtil.getRealPathFromURI_API19(mContext, uris[0].get(i));
                    Log.d(TAG, "path = "+path);
                    RealPathUtil.deleteFileFromPathFromAPI19(mContext, path);
                }
            }
            publishProgress(i+1);
        }

        return n;
    }

    @Override
    protected void onPostExecute(Integer count){
        super.onPostExecute(count);
        Log.d(TAG, "Inside on post execute and number of deleted images : "+count);

        Message messageToParent = new Message();
        messageToParent.what = 2;

        Bundle messageData = new Bundle();
        messageData.putInt("countDeletedImages", count);
        messageToParent.setData(messageData);

        // send message to mainThread
        parentHandler.sendMessage(messageToParent);
    }

    @Override
    protected void onProgressUpdate(Integer... values){
        Log.d(TAG, "deleted "+values[0]+" images");
        Message messageToParent = new Message();
        messageToParent.what = 3;

        Bundle messageData = new Bundle();
        messageData.putInt("progressCount",values[0]);
        messageToParent.setData(messageData);

        // send message to mainThread
        parentHandler.sendMessage(messageToParent);
    }
}

