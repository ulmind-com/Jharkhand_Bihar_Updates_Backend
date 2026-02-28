package com.soumyajit.jharkhand_project.service;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class OneSignalService {

    @Value("${onesignal.app.id}")
    private String appId;

    @Value("${onesignal.rest.api.key}")
    private String restApiKey;

    private static final String ONESIGNAL_API_URL = "https://onesignal.com/api/v1/notifications";
    private final OkHttpClient client = new OkHttpClient();

    @Async
    public void sendNotification(String playerId, String title, String message) {
        if (playerId == null || playerId.isEmpty()) {
            log.warn("Cannot send notification: playerId is null or empty");
            return;
        }

        try {
            JSONObject notification = new JSONObject();
            notification.put("app_id", appId);

            JSONArray playerIds = new JSONArray();
            playerIds.put(playerId);
            notification.put("include_subscription_ids", playerIds);

            JSONObject headings = new JSONObject();
            headings.put("en", title);
            notification.put("headings", headings);

            JSONObject contents = new JSONObject();
            contents.put("en", message);
            notification.put("contents", contents);

            RequestBody body = RequestBody.create(
                    notification.toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(ONESIGNAL_API_URL)
                    .post(body)
                    .addHeader("Authorization", "Key " + restApiKey)
                    .addHeader("Content-Type", "application/json")
                    .build();

            Response response = client.newCall(request).execute();

            if (response.isSuccessful()) {
                log.info("✅ OneSignal notification sent to player: {}", playerId);
            } else {
                log.error("❌ Failed to send OneSignal notification: {}", response.body().string());
            }

            response.close();
        } catch (Exception e) {
            log.error("❌ Error sending OneSignal notification", e);
        }
    }
}
