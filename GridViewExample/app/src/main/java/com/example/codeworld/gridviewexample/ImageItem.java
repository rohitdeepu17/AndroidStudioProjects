package com.example.codeworld.gridviewexample;

import android.graphics.Bitmap;
import android.net.Uri;

/**
 * Created by rohitsingla on 12/05/18.
 */

public class ImageItem {
    private Bitmap image;
    private Uri imageUri;
    private String title;
    private String info;

    public ImageItem(Uri imageUri, String title) {
        super();
        this.imageUri = imageUri;
        this.title = title;
        this.info = "";             //default empty
    }

    public Bitmap getImage() {
        return image;
    }

    public void setImage(Bitmap image) {
        this.image = image;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Uri getImageUri() {
        return imageUri;
    }

    public void setImageUri(Uri imageUri) {
        this.imageUri = imageUri;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getInfo(){
        return this.info;
    }
}
