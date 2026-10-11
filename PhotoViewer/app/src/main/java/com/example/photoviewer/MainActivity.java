package com.example.photoviewer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import org.json.JSONArray;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final boolean USE_CLOUD = true; // 로컬 Django: False, PythonAnywhere: True로 설정

    private static final String LOCAL_POST_URL =
            "http://10.0.2.2:8000/api_root/Post/";

    private static final String CLOUD_POST_URL =
            "https://kimjinse.pythonanywhere.com/api_root/Post/";

    private static final String POST_URL =
            USE_CLOUD ? CLOUD_POST_URL : LOCAL_POST_URL;

    private static final String AUTH_TOKEN =
            USE_CLOUD
                    ? BuildConfig.CLOUD_AUTH_TOKEN
                    : BuildConfig.LOCAL_AUTH_TOKEN;
    private static final long MAX_IMAGE_BYTES = 20L * 1024 * 1024;
    private TextView textView;
    private Button btnLoad, btnSave;
    private boolean busy;
    private Runnable pendingNetworkAction;
    private AlertDialog uploadDialog;
    private EditText authorInput, titleInput, bodyInput;
    private TextView imageStatus, uploadError;
    private ImageView preview;
    private Button pickImageButton;
    private Uri selectedImage;
    private String draftAuthor = "1", draftTitle = "", draftBody = "";
    private PostListViewModel postList;
    private EditText searchInput;
    private TextView searchSummary;
    private Button searchClear;
    private RecyclerView postRecycler;

    private final ActivityResultLauncher<String> localNetworkPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                Runnable action = pendingNetworkAction;
                pendingNetworkAction = null;
                setBusy(false);
                if (granted && action != null) {
                    action.run();
                } else if (!granted) {
                    textView.setText(R.string.permission_needed);
                    showUploadError(getString(R.string.permission_needed));
                }
            });

    private final ActivityResultLauncher<String[]> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri == null) return;
                try {
                    getContentResolver().takePersistableUriPermission(uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (SecurityException ignored) {
                    // Temporary access is sufficient for the current upload.
                }
                selectedImage = uri;
                updatePreview();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.textView);
        btnLoad = findViewById(R.id.btn_load);
        btnSave = findViewById(R.id.btn_save);
        postList = new ViewModelProvider(this).get(PostListViewModel.class);
        postRecycler = findViewById(R.id.recyclerView);
        postRecycler.setLayoutManager(new LinearLayoutManager(this));
        searchInput = findViewById(R.id.search_input);
        searchSummary = findViewById(R.id.search_summary);
        searchClear = findViewById(R.id.search_clear);
        searchInput.setText(postList.query);
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                postList.query = s.toString();
                applySearch();
            }
            @Override public void afterTextChanged(Editable s) { }
        });
        searchClear.setOnClickListener(v -> searchInput.setText(""));
        searchInput.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_SEARCH) return false;
            ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(view.getWindowToken(), 0);
            return true;
        });
        applySearch();
        View root = findViewById(R.id.main_root);
        int padding = root.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(padding + insets.left, padding + insets.top,
                    padding + insets.right, padding + insets.bottom);
            return windowInsets;
        });
        btnLoad.setOnClickListener(v -> requestNetworkAction(() -> startImageSync(false)));
        btnSave.setOnClickListener(v -> showUploadDialog());
        if (savedInstanceState != null) {
            draftAuthor = savedInstanceState.getString("draftAuthor", "1");
            draftTitle = savedInstanceState.getString("draftTitle", "");
            draftBody = savedInstanceState.getString("draftBody", "");
            String image = savedInstanceState.getString("draftImage");
            if (image != null) selectedImage = Uri.parse(image);
            if (savedInstanceState.getBoolean("composerOpen")) showUploadDialog();
        }
    }

    private void requestNetworkAction(Runnable action) {
        if (busy) return;
        if (Build.VERSION.SDK_INT >= 37 && ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_LOCAL_NETWORK) != PackageManager.PERMISSION_GRANTED) {
            pendingNetworkAction = action;
            setBusy(true);
            textView.setText(R.string.permission_needed);
            localNetworkPermissionLauncher.launch(Manifest.permission.ACCESS_LOCAL_NETWORK);
        } else {
            action.run();
        }
    }

    private void showUploadDialog() {
        if (uploadDialog != null || busy) return;
        View form = getLayoutInflater().inflate(R.layout.dialog_upload, null);
        authorInput = form.findViewById(R.id.upload_author);
        titleInput = form.findViewById(R.id.upload_title);
        bodyInput = form.findViewById(R.id.upload_text);
        imageStatus = form.findViewById(R.id.upload_image_status);
        uploadError = form.findViewById(R.id.upload_error);
        preview = form.findViewById(R.id.upload_preview);
        pickImageButton = form.findViewById(R.id.upload_pick_image);
        authorInput.setText(draftAuthor);
        titleInput.setText(draftTitle);
        bodyInput.setText(draftBody);
        pickImageButton.setOnClickListener(v -> imagePickerLauncher.launch(new String[]{"image/*"}));
        uploadDialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.upload_open).setView(form)
                .setPositiveButton(R.string.upload_submit, null)
                .setNegativeButton(R.string.upload_cancel, null).create();
        uploadDialog.setOnShowListener(dialog -> uploadDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> validateAndUpload()));
        uploadDialog.setOnDismissListener(dialog -> {
            rememberDraft();
            uploadDialog = null;
            authorInput = titleInput = bodyInput = null;
            preview = null;
            imageStatus = uploadError = null;
            pickImageButton = null;
        });
        uploadDialog.show();
        updatePreview();
    }

    private void updatePreview() {
        if (preview == null || selectedImage == null) return;
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            try (InputStream input = getContentResolver().openInputStream(selectedImage)) {
                if (input == null) throw new IOException();
                BitmapFactory.decodeStream(input, null, options);
            }
            if (options.outWidth <= 0 || options.outHeight <= 0) throw new IOException();
            options.inSampleSize = 1;
            while (options.outWidth / options.inSampleSize > 1000
                    || options.outHeight / options.inSampleSize > 1000) options.inSampleSize *= 2;
            options.inJustDecodeBounds = false;
            try (InputStream input = getContentResolver().openInputStream(selectedImage)) {
                if (input == null) throw new IOException();
                Bitmap image = BitmapFactory.decodeStream(input, null, options);
                if (image == null) throw new IOException();
                preview.setImageBitmap(image);
            }
            preview.setVisibility(View.VISIBLE);
            imageStatus.setText(R.string.upload_image_selected);
            uploadError.setVisibility(View.GONE);
        } catch (IOException | SecurityException e) {
            selectedImage = null;
            preview.setImageDrawable(null);
            preview.setVisibility(View.GONE);
            imageStatus.setText(R.string.upload_image_required);
            showUploadError(getString(R.string.upload_image_unreadable));
        }
    }

    private void validateAndUpload() {
        if (busy) return;
        rememberDraft();
        final int author;
        try {
            author = Integer.parseInt(draftAuthor);
            if (author < 1) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            authorInput.setError(getString(R.string.upload_author_invalid));
            authorInput.requestFocus();
            return;
        }
        if (draftTitle.isEmpty()) {
            titleInput.setError(getString(R.string.upload_title_required));
            titleInput.requestFocus();
            return;
        }
        if (draftBody.isEmpty()) {
            bodyInput.setError(getString(R.string.upload_text_required));
            bodyInput.requestFocus();
            return;
        }
        if (selectedImage == null) {
            showUploadError(getString(R.string.upload_image_required));
            return;
        }
        final Uri image = selectedImage;
        final String title = draftTitle, body = draftBody;
        requestNetworkAction(() -> {
            setBusy(true);
            textView.setText(R.string.upload_loading);
            uploadError.setVisibility(View.GONE);
            new PutPost(author, title, body, image).execute();
        });
    }

    private void rememberDraft() {
        if (authorInput == null) return;
        draftAuthor = authorInput.getText().toString().trim();
        draftTitle = titleInput.getText().toString().trim();
        draftBody = bodyInput.getText().toString().trim();
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        rememberDraft();
        state.putString("draftAuthor", draftAuthor);
        state.putString("draftTitle", draftTitle);
        state.putString("draftBody", draftBody);
        if (selectedImage != null) state.putString("draftImage", selectedImage.toString());
        state.putBoolean("composerOpen", uploadDialog != null);
        super.onSaveInstanceState(state);
    }

    private void setBusy(boolean value) {
        busy = value;
        btnLoad.setEnabled(!value);
        btnSave.setEnabled(!value);
        if (uploadDialog != null) {
            uploadDialog.setCancelable(!value);
            uploadDialog.setCanceledOnTouchOutside(!value);
            uploadDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(!value);
            uploadDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(!value);
            pickImageButton.setEnabled(!value);
            authorInput.setEnabled(!value);
            titleInput.setEnabled(!value);
            bodyInput.setEnabled(!value);
        }
    }

    private void showUploadError(String message) {
        if (uploadError != null) {
            uploadError.setText(message);
            uploadError.setVisibility(View.VISIBLE);
        }
    }

    /** Implements the PutPost task outlined on guide page 19. */
    private class PutPost extends AsyncTask<Void, Void, PostUploader.Response> {
        private final int author;
        private final String title, body;
        private final Uri image;
        private String failure;
        PutPost(int author, String title, String body, Uri image) {
            this.author = author;
            this.title = title;
            this.body = body;
            this.image = image;
        }
        @Override
        protected PostUploader.Response doInBackground(Void... args) {
            File temporaryImage = null;
            try {
                temporaryImage = File.createTempFile("post-upload-", ".img", getCacheDir());
                try (InputStream input = getContentResolver().openInputStream(image);
                     FileOutputStream output = new FileOutputStream(temporaryImage)) {
                    if (input == null) throw new IOException(getString(R.string.upload_image_unreadable));
                    byte[] buffer = new byte[8192];
                    long total = 0;
                    int count;
                    while ((count = input.read(buffer)) != -1) {
                        total += count;
                        if (total > MAX_IMAGE_BYTES)
                            throw new IOException(getString(R.string.upload_image_too_large));
                        output.write(buffer, 0, count);
                    }
                    if (total == 0) throw new IOException(getString(R.string.upload_image_unreadable));
                }
                String now = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
                        .format(new java.util.Date());
                return PostUploader.upload(POST_URL, AUTH_TOKEN, author, title, body,
                        now, temporaryImage, getContentResolver().getType(image));
            } catch (IOException | SecurityException e) {
                failure = e.getMessage() == null ? getString(R.string.upload_image_unreadable) : e.getMessage();
                return null;
            } finally {
                if (temporaryImage != null) temporaryImage.delete();
            }
        }
        @Override
        protected void onPostExecute(PostUploader.Response response) {
            if (isFinishing() || isDestroyed()) return;
            setBusy(false);
            if (response != null && response.code == HttpURLConnection.HTTP_CREATED) {
                if (uploadDialog != null) uploadDialog.dismiss();
                selectedImage = null;
                draftTitle = draftBody = "";
                Toast.makeText(MainActivity.this, R.string.upload_success, Toast.LENGTH_LONG).show();
                startImageSync(true);
            } else {
                String message = response == null ? failure :
                        getString(R.string.http_failure, response.code) + "\n" + response.body;
                textView.setText(getString(R.string.upload_failure, message));
                showUploadError(getString(R.string.upload_failure, message));
            }
        }
    }

    private void startImageSync(boolean afterUpload) {
        setBusy(true);
        textView.setText(afterUpload ? R.string.upload_sync_loading : R.string.sync_loading);
        new ImageSyncTask(afterUpload).execute();
    }

    private void openPost(PostItem post) {
        ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
        startActivity(PostDetailActivity.createIntent(this, post));
    }

    // Replace the synced source only on success; filtering never changes the source list.
    void displayPosts(List<PostItem> posts) {
        postList.posts.clear();
        postList.posts.addAll(posts);
        postList.hasSynced = true;
        applySearch();
    }

    private void applySearch() {
        List<PostItem> results = new ArrayList<>();
        for (PostItem post : postList.posts) {
            if (post.matchesQuery(postList.query)) results.add(post);
        }
        postRecycler.setAdapter(new ImageAdapter(results, this::openPost));
        searchClear.setEnabled(!postList.query.isEmpty());
        if (!postList.hasSynced) searchSummary.setText(R.string.search_sync_first);
        else if (postList.posts.isEmpty()) searchSummary.setText(R.string.sync_empty);
        else if (postList.query.trim().isEmpty())
            searchSummary.setText(getString(R.string.search_all, results.size()));
        else if (results.isEmpty()) searchSummary.setText(R.string.search_empty);
        else searchSummary.setText(getString(R.string.search_count, results.size()));
    }

    private class ImageSyncTask extends AsyncTask<Void, Void, List<PostItem>> {
        private final boolean afterUpload;
        private String failure;
        ImageSyncTask(boolean afterUpload) { this.afterUpload = afterUpload; }
        @Override
        protected List<PostItem> doInBackground(Void... args) {
            List<PostItem> items = new ArrayList<>();
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(POST_URL).openConnection();
                connection.setRequestProperty("Authorization", "Token " + AUTH_TOKEN);
                connection.setRequestProperty("Accept", "application/json");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                int code = connection.getResponseCode();
                if (code != HttpURLConnection.HTTP_OK)
                    throw new IOException(getString(R.string.http_failure, code));
                StringBuilder json = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                        connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) json.append(line);
                }
                JSONArray posts = new JSONArray(json.toString());
                for (int i = 0; i < posts.length(); i++) {
                    PostItem post = new PostItem(posts.getJSONObject(i));
                    items.add(post);
                    String imageUrl = post.imageUrl;
                    if (imageUrl.isEmpty()) continue;

                    if (!USE_CLOUD && imageUrl.contains("127.0.0.1")) {
                        imageUrl = imageUrl.replace(
                                "127.0.0.1",
                                "10.0.2.2"
                        );
                    }
                    HttpURLConnection imageConnection = null;
                    File cachedImage = null;
                    try {
                        imageConnection = (HttpURLConnection) new URL(imageUrl).openConnection();
                        imageConnection.setConnectTimeout(5000);
                        imageConnection.setReadTimeout(5000);
                        cachedImage = File.createTempFile("post-image-", ".img", getCacheDir());
                        try (InputStream input = imageConnection.getInputStream();
                             FileOutputStream output = new FileOutputStream(cachedImage)) {
                            byte[] buffer = new byte[8192];
                            int count;
                            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
                        }
                        post.bitmap = PostDetailActivity.decodeImage(cachedImage.getAbsolutePath());
                        if (post.bitmap == null) throw new IOException();
                        post.imagePath = cachedImage.getAbsolutePath();
                    } catch (IOException e) {
                        // A missing image must not hide the post's title and body.
                        if (cachedImage != null) cachedImage.delete();
                    } finally {
                        if (imageConnection != null) imageConnection.disconnect();
                    }
                }
            } catch (Exception e) {
                failure = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            } finally {
                if (connection != null) connection.disconnect();
            }
            return items;
        }
        @Override
        protected void onPostExecute(List<PostItem> posts) {
            if (isFinishing() || isDestroyed()) return;
            setBusy(false);
            if (failure != null) {
                textView.setText(getString(afterUpload ? R.string.upload_sync_failure : R.string.sync_failure, failure));
                return;
            }
            displayPosts(posts);
            textView.setText(posts.isEmpty() ? getString(R.string.sync_empty) :
                    getString(R.string.sync_success, posts.size()));
        }
    }
}
