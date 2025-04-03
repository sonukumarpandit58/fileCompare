package com.ims.bpcluat.utils;

import com.ims.bpcluat.interfaces.NetworkCallback;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class NetworkUtils {

    private static final String URL = "http://82.180.160.162:5000/v1/ocr"; // Replace with your endpoint URL
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private OkHttpClient client = new OkHttpClient();

    public void sendRequest(String content, NetworkCallback callback) {
        // Create JSON object
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("fileUrl", content);
            jsonObject.put("searchText", "");
            jsonObject.put("type", "image");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Create request body
        RequestBody body = RequestBody.create(jsonObject.toString(), JSON);

        // Build request
        Request request = new Request.Builder()
                .url(URL)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();

        // Execute request asynchronously
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    callback.onSuccess(responseData);
                } else {
                    callback.onFailure(new IOException("Unexpected code " + response));
                }
            }
        });
    }
}
