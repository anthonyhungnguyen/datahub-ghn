package utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;

@Slf4j
public class OkHttpRequester {
    private final OkHttpClient okHttpClient = new OkHttpClient();

    public <T> T callSync(Request request, Class<T> clazz) throws IOException {
        Call call = okHttpClient.newCall(request);
        try (Response response = call.execute()) {
            // Print error message and body
            if (!response.isSuccessful()) {
                log.error("Error when calling request: {}", request);
                ResponseBody responseBody = response.body();
                if (responseBody != null) {
                    log.error("Response body: {}", responseBody.string());
                }
                throw new IOException(String.valueOf(response.code()));
            }


            if (!response.isSuccessful()) {
                throw new IOException(String.valueOf(response.code()));
            }
            ResponseBody responseBody = response.body();
            if (responseBody != null) {
                String bodyString = responseBody.string();
                if (!StringUtils.isEmpty(bodyString)) {
                    return GsonUtils.parse(bodyString, clazz);
                }
            }
            return GsonUtils.parse(null, clazz);
        }
    }
}