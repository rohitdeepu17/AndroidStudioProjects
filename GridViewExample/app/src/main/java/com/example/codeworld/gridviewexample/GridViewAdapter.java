package com.example.codeworld.gridviewexample;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.squareup.picasso.Picasso;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by rohitsingla on 12/05/18.
 */

public class GridViewAdapter extends ArrayAdapter {
    private static final String TAG = "RDEEPU";
    private Context context;
    private int layoutResourceId;
    private ArrayList<ImageItem> data = new ArrayList();

    List<Integer> selectedPositions = new ArrayList<>();

    private TextRecognizer detector;


    public GridViewAdapter(Context context, int layoutResourceId, ArrayList data) {
        super(context, layoutResourceId, data);
        this.layoutResourceId = layoutResourceId;
        this.context = context;
        this.data = data;

        detector = new TextRecognizer.Builder(context).build();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        ViewHolder holder = null;

        if (row == null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);
            holder = new ViewHolder();
            holder.imageTitle = (TextView) row.findViewById(R.id.text);
            holder.image = (ImageView) row.findViewById(R.id.image);
            row.setTag(holder);
        } else {
            holder = (ViewHolder) row.getTag();
        }

        ImageItem item = data.get(position);
        holder.imageTitle.setText(item.getTitle());

        /*if(detectText(context, item.getImageUri()) && !selectedPositions.contains(position))
            selectedPositions.add(position);*/

        //set background according to selection even if recycled
        if(!selectedPositions.contains(position))
            row.setBackgroundColor(context.getResources().getColor(R.color.grey));
        else
            row.setBackgroundColor(context.getResources().getColor(R.color.brown));

        //holder.image.setImageBitmap(item.getImage());
        Picasso
                .with(context)
                .load(item.getImageUri())
                .resize(100,100)
                .into((ImageView) holder.image);
        return row;
    }

    public String getImageInfo(int i) {
        return data.get(i).getInfo();
    }

    static class ViewHolder {
        TextView imageTitle;
        ImageView image;
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
        try {
            Bitmap bitmap = decodeBitmapUri(context, imageUri);
            if (detector.isOperational() && bitmap != null) {
                Frame frame = new Frame.Builder().setBitmap(bitmap).build();
                SparseArray<TextBlock> textBlocks = detector.detect(frame);
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
                } else {
                    hasText = true;
                    msg += "Blocks: " + "\n"
                            + blocks + "\n"
                            + "---------" + "\n"
                            + "Lines: " + "\n"
                            + lines + "\n"
                            + "---------" + "\n"
                            + "Words: " + "\n"
                            + words + "\n"
                            + "---------" + "\n";
                }
                Log.d(TAG, "Msg : "+msg);
            } else {
                Log.d(TAG, "Could not set up the detector!");
            }
        } catch (Exception e) {
            Toast.makeText(context, "Failed to load Image", Toast.LENGTH_SHORT)
                    .show();
            Log.e(TAG, e.toString());
        }
        return hasText;
    }

    public void setImagesInfo(ArrayList<String> imageInfo){
        for(int i=0;i<imageInfo.size();i++){
            data.get(i).setInfo(imageInfo.get(i));
        }
    }
}
