package com.mtech.note;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.google.android.material.appbar.MaterialToolbar;

import java.io.File;

public class AttachmentViewerActivity extends AppCompatActivity {
    private static final String TAG = "AttachmentViewer";

    public static final String EXTRA_TYPE = "type";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_BODY = "body";
    public static final String EXTRA_URL = "url";

    ImageView image;
    WebView web;
    ScrollView textContainer;
    TextView title;
    TextView body;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attachment_viewer);

        MaterialToolbar toolbar = findViewById(R.id.viewer_toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        image = findViewById(R.id.viewer_image);
        web = findViewById(R.id.viewer_web);
        textContainer = findViewById(R.id.viewer_text_container);
        title = findViewById(R.id.viewer_title);
        body = findViewById(R.id.viewer_body);

        String type = getIntent().getStringExtra(EXTRA_TYPE);
        Log.d(TAG, "onCreate type=" + type);
        if (NoteAttachment.TYPE_IMAGE.equals(type)) {
            toolbar.setTitle("Image");
            web.setVisibility(android.view.View.GONE);
            textContainer.setVisibility(android.view.View.GONE);
            image.setVisibility(android.view.View.VISIBLE);
            String url = getIntent().getStringExtra(EXTRA_URL);
            Log.d(TAG, "IMAGE url from intent: " + (url != null ? url : "null"));
            if (url != null && !url.isEmpty()) {
                loadImageIntoView(url);
            } else {
                Log.w(TAG, "IMAGE url is null or empty, cannot load");
            }
        } else if (NoteAttachment.TYPE_URL.equals(type)) {
            toolbar.setTitle("URL");
            web.setVisibility(android.view.View.VISIBLE);
            WebSettings settings = web.getSettings();
            settings.setJavaScriptEnabled(false);
            settings.setDomStorageEnabled(false);
            String url = getIntent().getStringExtra(EXTRA_URL);
            if (url != null) web.loadUrl(url);
        } else {
            toolbar.setTitle("Note");
            textContainer.setVisibility(android.view.View.VISIBLE);
            title.setText(getIntent().getStringExtra(EXTRA_TITLE));
            body.setText(getIntent().getStringExtra(EXTRA_BODY));
        }
    }

    private void loadImageIntoView(String url) {
        Object model;
        if (url.startsWith("file:")) {
            String path = Uri.parse(url).getPath();
            Log.d(TAG, "loadImage local path=" + path);
            if (path != null) {
                File file = new File(path);
                boolean exists = file.exists();
                Log.d(TAG, "loadImage file exists=" + exists + " path=" + file.getAbsolutePath());
                model = exists ? file : Uri.parse(url);
            } else {
                Log.w(TAG, "loadImage file:// url had null path");
                model = Uri.parse(url);
            }
        } else {
            Log.d(TAG, "loadImage remote url=" + url);
            model = Uri.parse(url);
        }
        Glide.with(this)
                .load(model)
                .transition(DrawableTransitionOptions.withCrossFade())
                .listener(new RequestListener<android.graphics.drawable.Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object o, @NonNull com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean b) {
                        Log.e(TAG, "Glide load FAILED: " + (e != null ? e.getMessage() : "null"), e);
                        return false;
                    }
                    @Override
                    public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object o, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, DataSource dataSource, boolean b) {
                        Log.d(TAG, "Glide load SUCCESS");
                        return false;
                    }
                })
                .into(image);
    }
}

