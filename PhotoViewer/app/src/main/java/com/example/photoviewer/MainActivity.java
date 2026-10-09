package com.example.photoviewer;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String POST_URL =
            "http://10.0.2.2:8000/api_root/Post/";
     
    
    private TextView textView;
    private Button btnLoad;
    private Button btnSave;

    private final ActivityResultLauncher<String> localNetworkPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    granted -> {
                        if (granted) {
                            startImageSync();
                        } else {
                            btnLoad.setEnabled(true);
                            textView.setText(
                                    "PC 서버 연결을 위한 로컬 네트워크 권한이 필요합니다."
                            );
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.textView);
        btnLoad = findViewById(R.id.btn_load);
        btnSave = findViewById(R.id.btn_save);

        btnLoad.setOnClickListener(v -> {

            if (Build.VERSION.SDK_INT >= 37
                    && ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_LOCAL_NETWORK
            ) != PackageManager.PERMISSION_GRANTED) {

                btnLoad.setEnabled(false);
                textView.setText("로컬 네트워크 권한을 허용해주세요.");

                localNetworkPermissionLauncher.launch(
                        Manifest.permission.ACCESS_LOCAL_NETWORK
                );

            } else {
                startImageSync();
            }
        });

        btnSave.setOnClickListener(v ->
                Toast.makeText(
                        MainActivity.this,
                        "업로드 기능은 다음 단계에서 구현합니다.",
                        Toast.LENGTH_SHORT
                ).show()
        );
    }

    private void startImageSync() {
        btnLoad.setEnabled(false);
        textView.setText("이미지 동기화 중...");

        new ImageSyncTask().execute();
    }

    private class ImageSyncTask
            extends AsyncTask<Void, Void, List<Bitmap>> {

        @Override
        protected List<Bitmap> doInBackground(Void... voids) {

            List<Bitmap> bitmapList = new ArrayList<>();
            HttpURLConnection conn = null;

            try {
                URL url = new URL(POST_URL);

                conn = (HttpURLConnection) url.openConnection();

                conn.setRequestProperty(
                        "Authorization",
                        "Token " + BuildConfig.AUTH_TOKEN
                );

                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                int responseCode = conn.getResponseCode();

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    return bitmapList;
                }

                InputStream inputStream = conn.getInputStream();

                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(inputStream)
                        );

                StringBuilder result = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }

                reader.close();
                inputStream.close();

                JSONArray jsonArray =
                        new JSONArray(result.toString());

                for (int i = 0; i < jsonArray.length(); i++) {

                    JSONObject post =
                            jsonArray.getJSONObject(i);

                    String imageUrl =
                            post.getString("image");

                    /*
                     * Django가 이미지 URL을
                     * http://127.0.0.1:8000/... 형태로 반환하므로
                     * 에뮬레이터에서 Host PC로 접근할 수 있도록 변경
                     */
                    imageUrl = imageUrl.replace(
                            "127.0.0.1",
                            "10.0.2.2"
                    );

                    URL imageURL = new URL(imageUrl);

                    HttpURLConnection imageConn =
                            (HttpURLConnection)
                                    imageURL.openConnection();

                    imageConn.setConnectTimeout(5000);
                    imageConn.setReadTimeout(5000);

                    InputStream imageStream =
                            imageConn.getInputStream();

                    Bitmap bitmap =
                            BitmapFactory.decodeStream(
                                    imageStream
                            );

                    if (bitmap != null) {
                        bitmapList.add(bitmap);
                    }

                    imageStream.close();
                    imageConn.disconnect();
                }

            } catch (Exception e) {
                e.printStackTrace();

            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }

            return bitmapList;
        }

        @Override
        protected void onPostExecute(List<Bitmap> images) {

            btnLoad.setEnabled(true);

            if (images.isEmpty()) {
                textView.setText("불러올 이미지가 없습니다.");
                return;
            }

            textView.setText(
                    "이미지 로드 성공! (" + images.size() + "개)"
            );

            RecyclerView recyclerView =
                    findViewById(R.id.recyclerView);

            recyclerView.setLayoutManager(
                    new LinearLayoutManager(
                            MainActivity.this
                    )
            );

            ImageAdapter adapter =
                    new ImageAdapter(images);

            recyclerView.setAdapter(adapter);
        }
    }
}