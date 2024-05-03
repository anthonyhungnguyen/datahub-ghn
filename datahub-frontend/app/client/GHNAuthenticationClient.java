package client;
import auth.CookieConfigs;
import auth.sso.SsoManager;
import auth.sso.oidc.OidcCallbackLogic;
import com.google.gson.Gson;
import com.linkedin.common.AuditStamp;
import com.linkedin.common.url.Url;
import com.linkedin.common.urn.CorpuserUrn;
import com.linkedin.common.urn.Urn;
import com.linkedin.data.template.SetMode;
import com.linkedin.entity.client.SystemEntityClient;
import com.linkedin.identity.CorpUserEditableInfo;
import com.linkedin.identity.CorpUserInfo;
import com.linkedin.identity.CorpUserStatus;
import com.linkedin.metadata.Constants;
import com.linkedin.metadata.aspect.CorpUserAspect;
import com.linkedin.metadata.aspect.CorpUserAspectArray;
import com.linkedin.metadata.snapshot.CorpUserSnapshot;
import io.datahubproject.metadata.context.OperationContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import okhttp3.*;
import okhttp3.logging.HttpLoggingInterceptor;
import org.pac4j.core.config.Config;
import utils.ConfigUtil;
import utils.OkHttpRequester;
import lombok.extern.slf4j.Slf4j;


import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.constraints.NotBlank;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class GHNAuthenticationClient {
  private final OkHttpRequester okHttpRequester;
  private final OidcCallbackLogic oidcCallbackLogic;
  private final OperationContext systemOperationContext;

    @Inject
    public GHNAuthenticationClient(      @Nonnull SsoManager ssoManager,
                                         @Named("systemOperationContext") @Nonnull OperationContext systemOperationContext,
                                         @Nonnull SystemEntityClient entityClient,
                                         @Nonnull AuthServiceClient authClient,
                                         @Nonnull Config config,
                                         @Nonnull com.typesafe.config.Config configs) {
        this.okHttpRequester = new OkHttpRequester();
        this.systemOperationContext = systemOperationContext;
        this.oidcCallbackLogic = new OidcCallbackLogic(
                ssoManager,
                systemOperationContext,
                entityClient,
                authClient,
                new CookieConfigs(configs));
    }

    public String getCurrentPublicIpAddress() {
      OkHttpClient client = new OkHttpClient();

      Request request = new Request.Builder()
              .url("https://api.ipify.org?format=text")
              .build();

      try (Response response = client.newCall(request).execute()) {
        return response.body().string();
      } catch (IOException e) {
        log.error("Error when getting current public IP address", e);
        return null;
      }
    }

    public BaseResponse genAccessToken(@NotBlank String serviceToken, @NotBlank String userAgent) {
      try {
        // Create a map of values
        Map<String, String> values = new HashMap<>();
        values.put("app_key", ConfigUtil.DEFAULT_GHN_SSO_APP_KEY);
        values.put("app_secret", ConfigUtil.DEFAULT_GHN_SSO_APP_SECRET);
        values.put("user_agent", userAgent);
        values.put("authorization_code", serviceToken);

        // Convert the map to JSON string
        Gson gson = new Gson();
        String json = gson.toJson(values);
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(json, JSON);
        // Use the RequestBody in the Request
        Request request = new Request.Builder().url(ConfigUtil.DEFAULT_GHN_SSO_GEN_ACCESS_TOKEN_URL).post(body).build();
        return okHttpRequester.callSync(request, BaseResponse.class);
      } catch (IOException e) {
        log.error("Error when calling genAccessToken", e);
        log.error(String.format("Error when calling genAccessToken: %s", e.getMessage()));
        return new BaseResponse(500, "Internal Server Error", null);
      }
  }

  public BaseResponse verifyAccessToken(@NotBlank String accessToken, @NotBlank String userAgent, @NotBlank String remoteIp){
    try {
      Map<String, String> values = new HashMap<>();
      values.put("app_key", ConfigUtil.DEFAULT_GHN_SSO_APP_KEY);
      values.put("app_secret", ConfigUtil.DEFAULT_GHN_SSO_APP_SECRET);
      values.put("user_agent", userAgent);
      values.put("access_token", accessToken);
      values.put("remote_ip", remoteIp);


      // Convert the map to JSON string
      Gson gson = new Gson();
      String json = gson.toJson(values);
      MediaType JSON = MediaType.get("application/json; charset=utf-8");
      RequestBody body = RequestBody.create(json, JSON);

      // Use the RequestBody in the Request
      Request request = new Request.Builder().url(ConfigUtil.DEFAULT_GHN_SSO_VERIFY_ACCESS_TOKEN_URL).post(body).build();
      return okHttpRequester.callSync(request, BaseResponse.class);
    } catch (Exception e) {
      log.error("Error when calling verifyAccessToken", e);
      return new BaseResponse(500, "Internal Server Error", null);
    }
  }

  public EmployeeInfoResponse getEmployeeInfo(@NotBlank Integer employeeId) {
      try {

        Map<String, Integer> values = new HashMap<>();
//        values.put("employee_id", employeeId);
      values.put("employee_id", 245573);
        // Convert the map to JSON string
        Gson gson = new Gson();
        String json = gson.toJson(values);
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(json, JSON);

        // Use the RequestBody in the Request
        Request request = new Request.Builder().url(ConfigUtil.DEFAULT_GHN_EMPLOYEE_INFO_URL).post(body)
                .addHeader("Authorization", ConfigUtil.DEFAULT_GHN_EMPLOYEE_INFO_AUTHORIZATION).build();

        // Call the API
        EmployeeInfoResponse baseResponse =  okHttpRequester.callSync(request, EmployeeInfoResponse.class);
        if (baseResponse.getCode() != 200) {
          return new EmployeeInfoResponse(500, "Internal Server Error", null);
        } else {
            return baseResponse;
        }
      } catch (Exception e) {
        log.error("Error when calling getEmployeeInfo", e);
        return new EmployeeInfoResponse(500, "Internal Server Error", null);
      }

  }

  public Boolean provisionUser(@NotBlank CorpuserUrn urn,
                              @NotBlank String fullName,
                              @NotBlank String email) {
      try {

        log.debug("Just-in-time provisioning is enabled. Beginning provisioning process...");
        final CorpUserInfo userInfo = new CorpUserInfo();
        userInfo.setActive(true);
        userInfo.setFirstName(null, SetMode.IGNORE_NULL);
        userInfo.setLastName(null, SetMode.IGNORE_NULL);
        userInfo.setFullName(fullName, SetMode.IGNORE_NULL);
        userInfo.setEmail(email, SetMode.IGNORE_NULL);
        // If there is a display name, use it. Otherwise fall back to full name.
        userInfo.setDisplayName(fullName, SetMode.IGNORE_NULL);

        final CorpUserEditableInfo editableInfo = new CorpUserEditableInfo();
        final CorpUserSnapshot corpUserSnapshot = new CorpUserSnapshot();
        corpUserSnapshot.setUrn(urn);
        final CorpUserAspectArray aspects = new CorpUserAspectArray();
        aspects.add(CorpUserAspect.create(userInfo));
        aspects.add(CorpUserAspect.create(editableInfo));
        corpUserSnapshot.setAspects(aspects);
        this.oidcCallbackLogic.tryProvisionUser(this.systemOperationContext, corpUserSnapshot);
        this.oidcCallbackLogic.setUserStatus(this.systemOperationContext,
                urn,
                new CorpUserStatus()
                        .setStatus(Constants.CORP_USER_STATUS_ACTIVE)
                        .setLastModified(
                                new AuditStamp()
                                        .setActor(Urn.createFromString(Constants.SYSTEM_ACTOR))
                                        .setTime(System.currentTimeMillis())));
        return true;
      } catch (Exception e) {
        log.error("Error when provisioning user", e);
        return false;
      }
  }

  public String getRedirectUri() {
      return String.format("%s?app_key=%s&redirect_uri=%s", ConfigUtil.DEFAULT_GHN_SSO_LOGIN_URL, ConfigUtil.DEFAULT_GHN_SSO_APP_KEY, ConfigUtil.DEFAULT_GHN_SSO_REDIRECT_URL);
  }

  @AllArgsConstructor
  @Data
  public static class BaseResponse {
    private int code;
    private String message;
    private Map<String, String> data;
  }

  @AllArgsConstructor
  @Data
  public static class EmployeeInfoResponse {
    private int code;
    private String message;
    private Map<String, Object> data;
  }
}


