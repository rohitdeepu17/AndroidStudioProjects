package com.example.codeworld.gridviewexample;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Array;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "RDEEPU";
    private GridView gridView;
    private GridViewAdapter gridAdapter;
    private final int PICK_IMAGE_MULTIPLE =1;
    private final int REQUEST_PERMISSIONS = 2;
    Button mBtnDelete, mBtnProcess, mBtnToggle;
    LinearLayout llselected, llnotselected;
    ImageView mAddImages;
    Spinner spinnerCriteria;
    private int permissioncheck;
    private TextRecognizer detector;

    ArrayList<Uri> selectedUris = new ArrayList<>();
    private ProgressBar progressBar;
    private TextView textView;

    ProcessImagesAsyncTask mProcessImageAsyncTask;
    DeleteImagesAsyncTask mDeleteImagesAsyncTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        detector = new TextRecognizer.Builder(this).build();

        spinnerCriteria = (Spinner)findViewById(R.id.spinnerCriteria);
        ArrayList<String> criterias = new ArrayList<String>();
        criterias.add("TEXT ONLY");
        criterias.add("FACE ONLY");
        criterias.add("TEXT AND FACE");

        ArrayAdapter<String> mStringAdapter= new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, criterias);
        spinnerCriteria.setAdapter(mStringAdapter);

        llnotselected = (LinearLayout)findViewById(R.id.linearLayout1);
        llselected = (LinearLayout)findViewById(R.id.linearLayout2);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        textView = (TextView) findViewById(R.id.txtViewProgress);


        mBtnProcess = (Button)findViewById(R.id.btnProcess);
        mBtnProcess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "clicked process button");
                gridAdapter.selectedPositions.clear();
                gridView.setAdapter(gridAdapter);

                switch (spinnerCriteria.getSelectedItemPosition()){
                    case 0:
                        mProcessImageAsyncTask = new ProcessImagesAsyncTask(MainActivity.this, mainHandler, Utils.DetectionType.TEXT_ONLY);
                        mProcessImageAsyncTask.execute(selectedUris, null);
                        break;
                    case 1:
                        mProcessImageAsyncTask = new ProcessImagesAsyncTask(MainActivity.this, mainHandler, Utils.DetectionType.FACE_ONLY);
                        mProcessImageAsyncTask.execute(selectedUris, null);
                        break;
                    case 2:
                        mProcessImageAsyncTask = new ProcessImagesAsyncTask(MainActivity.this, mainHandler, Utils.DetectionType.TEXT_AND_FACE);
                        mProcessImageAsyncTask.execute(selectedUris, null);
                        break;
                    default:
                        break;
                }
                //new ProcessImagesAsyncTask(MainActivity.this, mainHandler).execute(selectedUris, null);
                //Need to do this on separate thread
                /*for(int i=0;i<selectedUris.size();i++){
                    if(detectText(MainActivity.this,selectedUris.get(i)) && !gridAdapter.selectedPositions.contains(i))
                        gridAdapter.selectedPositions.add(i);
                }

                gridView.setAdapter(gridAdapter);*/
                //Thread.holdsLock(gridView);
                //gridView.notifyAll();

                /*if(gridAdapter.selectedPositions.size()>0)
                    mBtnDelete.setClickable(true);*/

            }
        });

        mAddImages = (ImageView)findViewById(R.id.imageView);
        mAddImages.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mBtnDelete = (Button)findViewById(R.id.btndelete);
                mBtnDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Log.d(TAG, "Clicked Delete button");

                        final int n = gridAdapter.selectedPositions.size();
                        Log.d(TAG, "number of files selected : "+n);

                        //show dialog
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

                        builder.setTitle("Confirm");
                        builder.setMessage("Are you sure you want to delete the selected "+n+" image?");

                        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int which) {
                                // Do nothing but close the dialog
                                Log.d(TAG, "Clicked YES");
                                ArrayList<Uri> selectedImagesForDelete = new ArrayList<>();
                                for(int i=0;i<n;i++){
                                    selectedImagesForDelete.add(selectedUris.get(gridAdapter.selectedPositions.get(i)));
                                }
                                mDeleteImagesAsyncTask = new DeleteImagesAsyncTask(MainActivity.this, mainHandler);
                                mDeleteImagesAsyncTask.execute(selectedImagesForDelete, null);
                                dialog.dismiss();
                            }
                        });

                        builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Log.d(TAG, "Clicked NO");
                                // Do nothing
                                dialog.dismiss();
                            }
                        });

                        AlertDialog alert = builder.create();
                        alert.show();






                        /*ArrayList<Uri> selectedImagesForDelete = new ArrayList<>();
                        for(int i=0;i<n;i++){
                            selectedImagesForDelete.add(selectedUris.get(gridAdapter.selectedPositions.get(i)));
                        }
                        mDeleteImagesAsyncTask = new DeleteImagesAsyncTask(MainActivity.this, mainHandler);
                        mDeleteImagesAsyncTask.execute(selectedImagesForDelete, null);*/

                        //refresh
                        /*removeDeletedUris();
                        gridAdapter.selectedPositions.clear();
                        try {
                            gridAdapter = new GridViewAdapter(MainActivity.this, R.layout.grid_item_layout, getData());
                            if(gridAdapter!=null) {
                                gridView.setAdapter(gridAdapter);
                                Log.d(TAG, "gridAdapter is not null");
                            }else
                                Log.d(TAG, "gridAdapter is null");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }*/

                        /*for(int i=0;i<n;i++){
                            //String path = selectedUris.get(gridAdapter.selectedPositions.get(i)).toString();
                            String path;
                            if(android.os.Build.VERSION.SDK_INT<11)
                                path = RealPathUtil.getRealPathFromURI_BelowAPI11(MainActivity.this, selectedUris.get(gridAdapter.selectedPositions.get(i)));
                            else if(Build.VERSION.SDK_INT<=18)
                                path = RealPathUtil.getRealPathFromURI_API11to18(MainActivity.this, selectedUris.get(gridAdapter.selectedPositions.get(i)));
                            else {
                                String brandName = Build.BRAND;
                                Log.d(TAG, "brandName = "+brandName);
                                if(brandName.equalsIgnoreCase("redmi") || brandName.equalsIgnoreCase("xiaomi")){
                                    //For Redmi note 4
                                    path = selectedUris.get(gridAdapter.selectedPositions.get(i)).getPath();
                                    Log.d(TAG, "uri path: "+path);
                                    RealPathUtil.deleteFileFromPathFromAPI19(MainActivity.this, path.substring(5));
                                }else {
                                    path = RealPathUtil.getRealPathFromURI_API19(MainActivity.this, selectedUris.get(gridAdapter.selectedPositions.get(i)));
                                    Log.d(TAG, "path = "+path);
                                    RealPathUtil.deleteFileFromPathFromAPI19(MainActivity.this, path);
                                }

                            }
                            //path = getRealPathFromURI(selectedUris.get(gridAdapter.selectedPositions.get(i)));
                    *//*path = "file:"+path;
                    Log.d(TAG, "path = "+path);
                    File fdelete = null;
                    try {
                        fdelete = new File(new URI(path));
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
                    if(fdelete == null){
                        Log.d(TAG, "fdelete is null");
                    }else if (fdelete.exists()) {
                        if (fdelete.delete()) {
                            Log.d(TAG, "file Deleted :" + path);
                            //TODO :: Need to check for other API levels as well
                            RealPathUtil.deleteFileFromPathFromAPI19(MainActivity.this, path);
                        } else {
                            Log.d(TAG, "file not Deleted :" + path);
                        }
                    }else{
                        Log.d(TAG, "file does not exist");
                    }*//*
                        }*/
                    }
                });

                mBtnToggle = (Button)findViewById(R.id.toggleSelection);
                mBtnToggle.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        toggleSelectedPositions();
                        gridView.setAdapter(gridAdapter);
                    }
                });

                gridView = (GridView) findViewById(R.id.gridView);
                gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        int selectedIndex = gridAdapter.selectedPositions.indexOf(i);
                        if (selectedIndex > -1) {
                            gridAdapter.selectedPositions.remove(selectedIndex);
                            Log.d(TAG,"gridAdapter.selectedPositions size : "+gridAdapter.selectedPositions.size());
                            ((View)view).setBackgroundColor(getResources().getColor(R.color.grey));
                        } else {
                            gridAdapter.selectedPositions.add(i);
                            Log.d(TAG,"gridAdapter.selectedPositions size : "+gridAdapter.selectedPositions.size());
                            ((View)view).setBackgroundColor(getResources().getColor(R.color.brown));
                        }
                    }
                });

                gridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {

                        /*if (mProcessImageAsyncTask != null && mProcessImageAsyncTask.getStatus()!= AsyncTask.Status.FINISHED){
                            Toast.makeText(MainActivity.this, "Image processing in progress. Please wait.", Toast.LENGTH_SHORT).show();
                        }else */if(mDeleteImagesAsyncTask != null && mDeleteImagesAsyncTask.getStatus()!= AsyncTask.Status.FINISHED){
                            Toast.makeText(MainActivity.this, "Image deletion in progress. Please wait.", Toast.LENGTH_SHORT).show();
                        }else{
                            Log.d(TAG, "Text detected from Image : "+gridAdapter.getImageInfo(i));

                            //show dialog
                        /*Dialog.Builder builder = new Dialog.Builder(MainActivity.this);

                        builder.setTitle("Text in this Image");*/
                        /*builder.setMessage(gridAdapter.getImageInfo(i));

                        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int which) {
                                // Do nothing but close the dialog
                                dialog.dismiss();
                            }
                        });*/

                            Dialog alert = new Dialog(MainActivity.this);
                            alert.setTitle("Image Information");
                            alert.setContentView(R.layout.image_info_dialog);
                            ImageView imv = (ImageView)alert.findViewById(R.id.imageViewDialog);
                            TextView txtv = (TextView)alert.findViewById(R.id.textViewDialog);
                            txtv.setText("");
                            final int position = i;

                            try {
                                Bitmap myBitmap = decodeBitmapUri(MainActivity.this, selectedUris.get(i));
                                imv.setImageBitmap(decodeBitmapUri(MainActivity.this, selectedUris.get(i)));
                            /*BitmapDrawable drawable = (BitmapDrawable) imv.getDrawable();
                            Bitmap myBitmap = drawable.getBitmap();*/

                                Paint myRectPaint = new Paint();
                                myRectPaint.setStrokeWidth(5);
                                myRectPaint.setColor(Color.RED);
                                myRectPaint.setStyle(Paint.Style.STROKE);

                                Bitmap tempBitmap = Bitmap.createBitmap(myBitmap.getWidth(), myBitmap.getHeight(), Bitmap.Config.RGB_565);
                                Canvas tempCanvas = new Canvas(tempBitmap);
                                tempCanvas.drawBitmap(myBitmap, 0, 0, null);

                                try {
                                    if(gridAdapter.getImageInfo(position).length()>0) {
                                        JSONObject tempJsonObject = new JSONObject(gridAdapter.getImageInfo(position));
                                        if (tempJsonObject.has("text")) {
                                            Log.d(TAG, tempJsonObject.toString() + " <-json object, text-> : " + tempJsonObject.getString("text"));
                                            txtv.setText(tempJsonObject.getString("text"));
                                        } else {
                                            Log.d(TAG, "else case setting text as empty string.");
                                            txtv.setText("");
                                        }

                                        int faceCount = 0;
                                        if (tempJsonObject.has("faceCount"))
                                            faceCount = tempJsonObject.getInt("faceCount");
                                        Log.d(TAG, "face count : " + faceCount);
                                        for (int j = 0; j < faceCount; j++) {
                                            JSONObject faceObject = tempJsonObject.getJSONObject("face" + j);
                                            double x1 = faceObject.getDouble("x1");
                                            double y1 = faceObject.getDouble("y1");
                                            double x2 = faceObject.getDouble("x2");
                                            double y2 = faceObject.getDouble("y2");
                                            Log.d(TAG, "x1,y1,x2,y2 : " + x1 + "," + y1 + "," + x2 + "," + y2);
                                            tempCanvas.drawRoundRect(new RectF((float) x1, (float) y1, (float) x2, (float) y2), 2, 2, myRectPaint);
                                        }
                                        imv.setImageDrawable(new BitmapDrawable(getResources(), tempBitmap));
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            }
                            alert.show();
                        }
                        return true;
                    }

                });

                Intent intent = new Intent();
                intent.setType("image/*");
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent,"Select Picture"), PICK_IMAGE_MULTIPLE);
            }


        });

        llnotselected.setVisibility(View.VISIBLE);
        llselected.setVisibility(View.GONE);

        permissioncheck = 0;

        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            if (ActivityCompat.shouldShowRequestPermissionRationale
                    (MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                    ActivityCompat.shouldShowRequestPermissionRationale
                            (MainActivity.this, Manifest.permission.READ_CONTACTS)) {
                Snackbar.make(findViewById(android.R.id.content),
                        "Please Grant Permissions",
                        Snackbar.LENGTH_LONG).setAction("ENABLE",
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{Manifest.permission
                                                .WRITE_EXTERNAL_STORAGE},
                                        REQUEST_PERMISSIONS);
                            }
                        }).show();
            } else {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission
                                .WRITE_EXTERNAL_STORAGE},
                        REQUEST_PERMISSIONS);
            }
        }else{
            permissioncheck = 1;
        }


        Log.d(TAG, "case : "+permissioncheck);
        switch (permissioncheck){
            case 0:
            {

                //kill the activity
            }
            case 1:
            {

            }
            default:
                //kill the activity
        }


    }

    private void removeDeletedUris() {
        ArrayList<Uri> updatedUris = new ArrayList<>();
        for(int i=0;i<selectedUris.size();i++){
            if(!gridAdapter.selectedPositions.contains(i))
                updatedUris.add(selectedUris.get(i));
        }
        Log.d(TAG, "size of updatedUris : "+updatedUris.size());
        selectedUris = updatedUris;
    }

    private void toggleSelectedPositions() {
        ArrayList<Integer> updatedUris = new ArrayList<>();
        for(int i=0;i<selectedUris.size();i++){
            if(!gridAdapter.selectedPositions.contains(i))
                updatedUris.add(i);
        }
        Log.d(TAG, "size of updatedUris : "+updatedUris.size());
        gridAdapter.selectedPositions.clear();
        gridAdapter.selectedPositions = updatedUris;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub

        if(requestCode==PICK_IMAGE_MULTIPLE){

            if(resultCode==RESULT_OK){
                //data.getParcelableArrayExtra(name);
                //If Single image selected then it will fetch from Gallery
                if(data.getData()!=null){
                    Log.d(TAG, "single image selected");
                    Uri mImageUri=data.getData();

                }else{
                    if(data.getClipData()!=null){
                        ClipData mClipData=data.getClipData();
                        ArrayList<Uri> mArrayUri=new ArrayList<Uri>();
                        for(int i=0;i<mClipData.getItemCount();i++){

                            ClipData.Item item = mClipData.getItemAt(i);
                            Uri uri = item.getUri();
                            mArrayUri.add(uri);
                            selectedUris.add(uri);

                        }
                        Log.v(TAG, "Selected Images"+ mArrayUri.size());
                    }

                }
                try {
                    gridAdapter = new GridViewAdapter(this, R.layout.grid_item_layout, getData());
                    if(gridAdapter!=null) {
                        gridView.setAdapter(gridAdapter);
                        Log.d(TAG, "gridAdapter is not null");
                        llnotselected.setVisibility(View.GONE);
                        llselected.setVisibility(View.VISIBLE);
                    }else
                        Log.d(TAG, "gridAdapter is null");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


        super.onActivityResult(requestCode, resultCode, data);
    }

    // Prepare some dummy data for gridview
    private ArrayList<ImageItem> getData() throws IOException {
        final ArrayList<ImageItem> imageItems = new ArrayList<>();
        /*TypedArray imgs = getResources().obtainTypedArray(R.array.image_ids);
        for (int i = 0; i < imgs.length(); i++) {
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), imgs.getResourceId(i, -1));
            imageItems.add(new ImageItem(bitmap, "Image#" + i));
        }*/
        for (int i = 0; i < selectedUris.size(); i++) {
            //Bitmap bitmap = decodeUriToBitmap(MainActivity.this, selectedUris.get(i));
            imageItems.add(new ImageItem(selectedUris.get(i), "Image#" + i));
        }
        return imageItems;
    }

    public static Bitmap decodeUriToBitmap(Context mContext, Uri sendUri) {
        Bitmap getBitmap = null;
        try {
            InputStream image_stream;
            try {
                image_stream = mContext.getContentResolver().openInputStream(sendUri);
                getBitmap = BitmapFactory.decodeStream(image_stream);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return getBitmap;
    }

    private String getRealPathFromURI(Uri contentURI) {
        String result;
        Cursor cursor = getContentResolver().query(contentURI, null, null, null, null);
        if (cursor == null) { // Source is Dropbox or other similar local file path
            result = contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
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



    /*public void requestAppPermissions(final String[] requestedPermissions,
                                      final int stringId, final int requestCode) {
        int permissionCheck = PackageManager.PERMISSION_GRANTED;
        boolean shouldShowRequestPermissionRationale = false;
        for (String permission : requestedPermissions) {
            permissionCheck = permissionCheck + ContextCompat.checkSelfPermission(this, permission);
            shouldShowRequestPermissionRationale = shouldShowRequestPermissionRationale || ActivityCompat.shouldShowRequestPermissionRationale(this, permission);
        }
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            if (shouldShowRequestPermissionRationale) {
                Snackbar.make(findViewById(android.R.id.content), stringId,
                        Snackbar.LENGTH_LONG).setAction("GRANT",
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                ActivityCompat.requestPermissions(RuntimePermissionsActivity.this, requestedPermissions, requestCode);
                            }
                        }).show();
            } else {
                ActivityCompat.requestPermissions(this, requestedPermissions, requestCode);
            }
        } else {
            onPermissionsGranted(requestCode);
        }
    }*/


    /** The main handler. */
    public Handler mainHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            //Log.d(TAG,"Message received by handler of MainActivity");
            if (msg.what == 0) {
                ArrayList<Integer> selectedImageIndices = msg.getData().getIntegerArrayList("selectedAfterProcessingImages");
                int n = selectedImageIndices.size();
                ArrayList<String> imagesInfo = msg.getData().getStringArrayList("textInImages");
                gridAdapter.setImagesInfo(imagesInfo);
                for(int i=0;i<n;i++)
                    gridAdapter.selectedPositions.add(selectedImageIndices.get(i));
                gridView.setAdapter(gridAdapter);

                if(!MyApplication.isActivityVisible()){
                    //create a notification
                    Log.d(TAG, "Activity not visible. Create a notification to notify the user of finishing the execution of processing");
                }
                //threadModifiedText.setText(msg.getData().getString("text"));
            }else if(msg.what == 1){
                //show progress
                int cnt = msg.getData().getInt("progressCount");
                progressBar.setProgress((cnt*100)/selectedUris.size());
                textView.setText("Processed "+ cnt +"/"+selectedUris.size()+" images");
            }else if(msg.what == 2){
                int cnt = msg.getData().getInt("countDeletedImages");
                Log.d(TAG, "deleted "+cnt);
                Toast.makeText(MainActivity.this,"Successfully deleted "+cnt+" images",Toast.LENGTH_SHORT).show();
                removeDeletedUris();
                gridAdapter.selectedPositions.clear();
                try {
                    gridAdapter = new GridViewAdapter(MainActivity.this, R.layout.grid_item_layout, getData());
                    if(gridAdapter!=null) {
                        gridView.setAdapter(gridAdapter);
                        Log.d(TAG, "gridAdapter is not null");
                    }else
                        Log.d(TAG, "gridAdapter is null");
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if(!MyApplication.isActivityVisible()){
                    //create a notification
                    Log.d(TAG, "Activity not visible. Create a notification to notify the user of finishing the execution of deleting");
                }
            }else if(msg.what == 3){
                int cnt = msg.getData().getInt("progressCount");
                Log.d(TAG, "selected positions count : "+gridAdapter.selectedPositions.size());
                progressBar.setProgress((cnt*100)/gridAdapter.selectedPositions.size());
                textView.setText("Deleted "+ cnt +"/"+gridAdapter.selectedPositions.size()+" images");
            }
        };
    };

    @Override
    public void onBackPressed()
    {
        if (mProcessImageAsyncTask != null && mProcessImageAsyncTask.getStatus()!= AsyncTask.Status.FINISHED){
            mProcessImageAsyncTask.releaseDetectors();
            mProcessImageAsyncTask.cancel(true);
            Log.d(TAG, "Cancelled async task to process images on back press");
        }
        if (mDeleteImagesAsyncTask != null && mDeleteImagesAsyncTask.getStatus()!= AsyncTask.Status.FINISHED) {
            mDeleteImagesAsyncTask.cancel(true);
            Log.d(TAG, "Cancelled async task to delete images on back press");
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy(){
        if (mProcessImageAsyncTask != null && mProcessImageAsyncTask.getStatus()!= AsyncTask.Status.FINISHED) {
            mProcessImageAsyncTask.releaseDetectors();
            mProcessImageAsyncTask.cancel(true);
            Log.d(TAG, "Cancelled async task to process images on destroy");
        }

        if (mDeleteImagesAsyncTask != null && mDeleteImagesAsyncTask.getStatus()!= AsyncTask.Status.FINISHED) {
            mDeleteImagesAsyncTask.cancel(true);
            Log.d(TAG, "Cancelled async task to delete images on destroy");
        }
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        MyApplication.activityResumed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        MyApplication.activityPaused();
    }

}


