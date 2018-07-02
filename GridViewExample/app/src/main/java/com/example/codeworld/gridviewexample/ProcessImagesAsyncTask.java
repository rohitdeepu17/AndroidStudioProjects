package com.example.codeworld.gridviewexample;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.util.ArrayList;


/**
 * Created by rohitsingla on 13/05/18.
 */

public class ProcessImagesAsyncTask extends AsyncTask< ArrayList<Uri>, Integer,ArrayList<Integer> > {
    private static final String TAG = "RDEEPU";
    private TextRecognizer textRecognizer;
    private FaceDetector faceDetector;

    Utils.DetectionType detectionType;

    private ArrayList<String> imageInfo;

    private Context mContext;

    // reference to mainHandler from the mainThread
    private Handler parentHandler;

    public ProcessImagesAsyncTask (Context context, Handler handler, Utils.DetectionType detectionType){
        mContext = context;
        this.parentHandler = handler;
        this.detectionType = detectionType;
        imageInfo = new ArrayList<>();
    }


    @Override
    protected ArrayList<Integer> doInBackground(ArrayList<Uri>... uris) {
        textRecognizer = new TextRecognizer.Builder(mContext).build();
        faceDetector = new FaceDetector.Builder(mContext).setTrackingEnabled(false).build();

        int n = uris[0].size();
        ArrayList<Integer> mSelectedImages = new ArrayList<>();
        Log.d(TAG,"Inside async task and processing "+n+" images");
        switch (detectionType){
            case TEXT_ONLY:
                for(int i=0;i<n;i++){
                    if(detectText(mContext, uris[0].get(i))){
                        mSelectedImages.add(i);
                    }
                    publishProgress(i+1);
                }
                break;
            case FACE_ONLY:
                for(int i=0;i<n;i++){
                    if(detectFace(mContext, uris[0].get(i))){
                        mSelectedImages.add(i);
                    }
                    publishProgress(i+1);
                }
                break;
            case TEXT_AND_FACE:
                for(int i=0;i<n;i++){
                    if(detectFaceAndText(mContext, uris[0].get(i))){
                        mSelectedImages.add(i);
                    }
                    publishProgress(i+1);
                }
                break;
            default:
                break;
        }
        /*for(int i=0;i<n;i++){

            if(detectText(mContext, uris[0].get(i))){
                mSelectedImages.add(i);
            }
            //just for debugging at this point
            if(detectFace(mContext, uris[0].get(i))){
                Log.d(TAG, "Image #"+i+" contains a face");
            }else{
                Log.d(TAG, "Image #"+i+" does not contain a face");
            }
            publishProgress(i+1);
        }*/

        return mSelectedImages;
    }

    private boolean detectFace(Context context, Uri imageUri) {
        boolean hasFace = false;
        JSONObject mImageInfoJsonObj = new JSONObject();
        try {
            Bitmap bitmap = decodeBitmapUri(context, imageUri);
            if (faceDetector.isOperational() && bitmap != null) {
                Frame frame = new Frame.Builder().setBitmap(bitmap).build();
                SparseArray<Face> faces = faceDetector.detect(frame);
                if(faces.size()>0){
                    hasFace = true;
                    mImageInfoJsonObj.put("faceCount",faces.size());
                }
                else{
                    mImageInfoJsonObj.put("faceCount",0);
                }
                for(int i=0; i<faces.size(); i++) {
                    Face thisFace = faces.valueAt(i);
                    float x1 = thisFace.getPosition().x;
                    float y1 = thisFace.getPosition().y;
                    float x2 = x1 + thisFace.getWidth();
                    float y2 = y1 + thisFace.getHeight();
                    JSONObject faceObject = new JSONObject();
                    faceObject.put("x1",x1);
                    faceObject.put("y1",y1);
                    faceObject.put("x2",x2);
                    faceObject.put("y2",y2);
                    mImageInfoJsonObj.put("face"+i,faceObject);
                    Log.d(TAG, "Face from (" + x1 + "," + y1 + ") -> (" + x2 + "," + y2 + ")");
                }
            } else {
                Log.d(TAG, "Could not set up the textRecognizer!");
                mImageInfoJsonObj.put("faceCount",0);
            }
        } catch (Exception e) {
            Toast.makeText(context, "Failed to load Image", Toast.LENGTH_SHORT)
                    .show();
            Log.e(TAG, e.toString());
        }
        imageInfo.add(mImageInfoJsonObj.toString());
        return hasFace;
    }

    @Override
    protected void onPostExecute(ArrayList<Integer> mSelectedImages){
        super.onPostExecute(mSelectedImages);
        Log.d(TAG, "Inside on post execute and number of selected images : "+mSelectedImages.size());

        Message messageToParent = new Message();
        messageToParent.what = 0;

        Bundle messageData = new Bundle();
        messageData.putIntegerArrayList("selectedAfterProcessingImages", mSelectedImages);
        messageData.putStringArrayList("textInImages", imageInfo);
        messageToParent.setData(messageData);

        // send message to mainThread
        parentHandler.sendMessage(messageToParent);
    }


    private Bitmap decodeBitmapUri(Context ctx, Uri uri) throws FileNotFoundException {
        int targetW = 600;
        int targetH = 600;
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(ctx.getContentResolver().openInputStream(uri), null, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;

        return BitmapFactory.decodeStream(ctx.getContentResolver()
                .openInputStream(uri), null, bmOptions);
    }

    private boolean detectText(Context context, Uri imageUri){
        boolean hasText = false;
        JSONObject mImageInfoJsonObj = new JSONObject();
        try {
            Bitmap bitmap = decodeBitmapUri(context, imageUri);
            if (textRecognizer.isOperational() && bitmap != null) {
                Frame frame = new Frame.Builder().setBitmap(bitmap).build();
                SparseArray<TextBlock> textBlocks = textRecognizer.detect(frame);
                String blocks = "";
                String lines = "";
                String words = "";
                for (int index = 0; index < textBlocks.size(); index++) {
                    //extract scanned text blocks here
                    TextBlock tBlock = textBlocks.valueAt(index);
                    blocks = blocks + tBlock.getValue() + "\n" + "\n";
                    for (Text line : tBlock.getComponents()) {
                        //extract scanned text lines here
                        lines = lines + line.getValue() + "\n";
                        for (Text element : line.getComponents()) {
                            //extract scanned text words here
                            words = words + element.getValue() + ", ";
                        }
                    }
                }
                String msg = "";
                if (textBlocks.size() == 0) {
                    msg = "Scan Failed: Found nothing to scan";
                    //imageInfo.add("");
                    mImageInfoJsonObj.put("text","");
                } else {
                    hasText = true;
                    /*msg += "Blocks: " + "\n"
                            + blocks + "\n"
                            + "---------" + "\n"
                            + "Lines: " + "\n"
                            + lines + "\n"
                            + "---------" + "\n"
                            + "Words: " + "\n"
                            + words + "\n"
                            + "---------" + "\n";*/
                    msg += blocks;
                    //imageInfo.add(msg);
                    mImageInfoJsonObj.put("text",msg);
                }
                Log.d(TAG, "Msg : "+msg);

            } else {
                Log.d(TAG, "Could not set up the textRecognizer!");
                //imageInfo.add("");
                mImageInfoJsonObj.put("text","");
            }
        } catch (Exception e) {
            //Toast.makeText(context, "Failed to load Image", Toast.LENGTH_SHORT).show();
            Log.e(TAG, e.toString());
        }finally {
            imageInfo.add(mImageInfoJsonObj.toString());
        }
        return hasText;
    }

    private boolean detectFaceAndText(Context context, Uri imageUri){
        boolean hasFace = false;
        boolean hasText = false;
        JSONObject mImageInfoJsonObj = new JSONObject();

        try {
            Bitmap bitmap = decodeBitmapUri(context, imageUri);
            if (textRecognizer.isOperational() && bitmap != null) {
                Frame frame = new Frame.Builder().setBitmap(bitmap).build();
                SparseArray<TextBlock> textBlocks = textRecognizer.detect(frame);
                String blocks = "";
                String lines = "";
                String words = "";
                for (int index = 0; index < textBlocks.size(); index++) {
                    //extract scanned text blocks here
                    TextBlock tBlock = textBlocks.valueAt(index);
                    blocks = blocks + tBlock.getValue() + "\n" + "\n";
                    for (Text line : tBlock.getComponents()) {
                        //extract scanned text lines here
                        lines = lines + line.getValue() + "\n";
                        for (Text element : line.getComponents()) {
                            //extract scanned text words here
                            words = words + element.getValue() + ", ";
                        }
                    }
                }
                String msg = "";
                if (textBlocks.size() == 0) {
                    msg = "Scan Failed: Found nothing to scan";
                    //imageInfo.add("");
                    mImageInfoJsonObj.put("text","");
                } else {
                    hasText = true;
                    /*msg += "Blocks: " + "\n"
                            + blocks + "\n"
                            + "---------" + "\n"
                            + "Lines: " + "\n"
                            + lines + "\n"
                            + "---------" + "\n"
                            + "Words: " + "\n"
                            + words + "\n"
                            + "---------" + "\n";*/
                    msg += blocks;
                    //imageInfo.add(msg);
                    mImageInfoJsonObj.put("text",msg);
                }
                Log.d(TAG, "Msg : "+msg);

            } else {
                Log.d(TAG, "Could not set up the textRecognizer!");
                //imageInfo.add("");
                mImageInfoJsonObj.put("text","");
            }

            //for face recognition
            if (faceDetector.isOperational() && bitmap != null) {
                Frame frame = new Frame.Builder().setBitmap(bitmap).build();
                SparseArray<Face> faces = faceDetector.detect(frame);
                if(faces.size()>0){
                    hasFace = true;
                    mImageInfoJsonObj.put("faceCount",faces.size());
                }
                else{
                    mImageInfoJsonObj.put("faceCount",0);
                }
                for(int i=0; i<faces.size(); i++) {
                    Face thisFace = faces.valueAt(i);
                    float x1 = thisFace.getPosition().x;
                    float y1 = thisFace.getPosition().y;
                    float x2 = x1 + thisFace.getWidth();
                    float y2 = y1 + thisFace.getHeight();
                    JSONObject faceObject = new JSONObject();
                    faceObject.put("x1",x1);
                    faceObject.put("y1",y1);
                    faceObject.put("x2",x2);
                    faceObject.put("y2",y2);
                    mImageInfoJsonObj.put("face"+i,faceObject);
                    Log.d(TAG, "Face from (" + x1 + "," + y1 + ") -> (" + x2 + "," + y2 + ")");
                }
            } else {
                Log.d(TAG, "Could not set up the textRecognizer!");
                mImageInfoJsonObj.put("faceCount",0);
            }

        } catch (Exception e) {
            //Toast.makeText(context, "Failed to load Image", Toast.LENGTH_SHORT).show();
            Log.e(TAG, e.toString());
        }finally {
            imageInfo.add(mImageInfoJsonObj.toString());
        }
        return (hasText && hasFace);
    }

    @Override
    protected void onProgressUpdate(Integer... values){
        Log.d(TAG, "processed "+values[0]+" images");
        Message messageToParent = new Message();
        messageToParent.what = 1;

        Bundle messageData = new Bundle();
        messageData.putInt("progressCount",values[0]);
        messageToParent.setData(messageData);

        // send message to mainThread
        parentHandler.sendMessage(messageToParent);
    }

    public void releaseDetectors(){
        faceDetector.release();
        textRecognizer.release();
    }
}
