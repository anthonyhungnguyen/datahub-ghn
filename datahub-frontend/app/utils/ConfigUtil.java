package utils;

import com.linkedin.util.Configuration;
import com.typesafe.config.Config;

public class ConfigUtil {

  private ConfigUtil() {}

  // New configurations, provided via application.conf file.
  public static final String METADATA_SERVICE_HOST_CONFIG_PATH = "metadataService.host";
  public static final String METADATA_SERVICE_PORT_CONFIG_PATH = "metadataService.port";
  public static final String METADATA_SERVICE_USE_SSL_CONFIG_PATH = "metadataService.useSsl";
  public static final String METADATA_SERVICE_SSL_PROTOCOL_CONFIG_PATH =
      "metadataService.sslProtocol";

  // GHN SSO
  public static final String GHN_SSO_LOGIN_URL = "GHN_SSO_LOGIN_URL";
  public static final String GHN_SSO_REDIRECT_URL = "GHN_SSO_REDIRECT_URL";
  public static final String GHN_SSO_GEN_ACCESS_TOKEN_URL = "GHN_SSO_GEN_ACCESS_TOKEN_URL";
  public static final String GHN_SSO_VERIFY_ACCESS_TOKEN_URL = "GHN_SSO_VERIFY_ACCESS_TOKEN_URL";
  public static final String GHN_SSO_APP_KEY = "GHN_SSO_APP_KEY";
  public static final String GHN_SSO_APP_SECRET = "GHN_SSO_APP_SECRET";
  public static final String GHN_EMPLOYEE_INFO_URL = "GHN_EMPLOYEE_INFO_URL";
  public static final String GHN_EMPLOYEE_INFO_AUTH = "GHN_EMPLOYEE_INFO_AUTH";

  // Legacy env-var based config values, for backwards compatibility:
  public static final String GMS_HOST_ENV_VAR = "DATAHUB_GMS_HOST";
  public static final String GMS_PORT_ENV_VAR = "DATAHUB_GMS_PORT";
  public static final String GMS_USE_SSL_ENV_VAR = "DATAHUB_GMS_USE_SSL";
  public static final String GMS_SSL_PROTOCOL_VAR = "DATAHUB_GMS_SSL_PROTOCOL";

  // Default values
  public static final String DEFAULT_GMS_HOST = "localhost";
  public static final String DEFAULT_GMS_PORT = "8080";
  public static final String DEFAULT_GMS_USE_SSL = "False";

  public static final String DEFAULT_METADATA_SERVICE_HOST =
      Configuration.getEnvironmentVariable(GMS_HOST_ENV_VAR, "localhost");
  public static final Integer DEFAULT_METADATA_SERVICE_PORT =
      Integer.parseInt(Configuration.getEnvironmentVariable(GMS_PORT_ENV_VAR, "8080"));
  public static final Boolean DEFAULT_METADATA_SERVICE_USE_SSL =
      Boolean.parseBoolean(Configuration.getEnvironmentVariable(GMS_USE_SSL_ENV_VAR, "False"));
  public static final String DEFAULT_METADATA_SERVICE_SSL_PROTOCOL =
      Configuration.getEnvironmentVariable(GMS_SSL_PROTOCOL_VAR);

  public static final String DEFAULT_GHN_SSO_LOGIN_URL = Configuration.getEnvironmentVariable(GHN_SSO_LOGIN_URL, "https://sso-v2.ghn.vn/internal/login");
  public static final String DEFAULT_GHN_SSO_REDIRECT_URL = Configuration.getEnvironmentVariable(GHN_SSO_REDIRECT_URL, "http://localhost:3000");
  public static final String DEFAULT_GHN_SSO_GEN_ACCESS_TOKEN_URL = Configuration.getEnvironmentVariable(GHN_SSO_GEN_ACCESS_TOKEN_URL, "https://online-gateway.ghn.vn/sso-v2/public-api/staff/gen-access-token");
  public static final String DEFAULT_GHN_SSO_VERIFY_ACCESS_TOKEN_URL = Configuration.getEnvironmentVariable(GHN_SSO_VERIFY_ACCESS_TOKEN_URL, "https://online-gateway.ghn.vn/sso-v2/public-api/staff/verify-access-token");
  public static final String DEFAULT_GHN_SSO_APP_KEY = Configuration.getEnvironmentVariable(GHN_SSO_APP_KEY, "62fb5476-3ed5-4431-9250-0aaa0bdda05f");
  public static final String DEFAULT_GHN_SSO_APP_SECRET = Configuration.getEnvironmentVariable(GHN_SSO_APP_SECRET, "f19f0e2e-b03b-4bf2-9ac2-10dd65f7355d");
  public static final String DEFAULT_GHN_EMPLOYEE_INFO_URL = Configuration.getEnvironmentVariable(GHN_EMPLOYEE_INFO_URL, "https://online-gateway.ghn.vn/hrm/core/api/employee/detail");
  public static final String DEFAULT_GHN_EMPLOYEE_INFO_AUTHORIZATION = Configuration.getEnvironmentVariable(GHN_EMPLOYEE_INFO_AUTH, "Basic b25saW5lOkk1U3tnfn51Mjs=");

  public static boolean getBoolean(Config config, String key) {
    return config.hasPath(key) && config.getBoolean(key);
  }

  public static boolean getBoolean(Config config, String key, boolean defaultValue) {
    return config.hasPath(key) ? config.getBoolean(key) : defaultValue;
  }

  public static int getInt(Config config, String key, int defaultValue) {
    return config.hasPath(key) ? config.getInt(key) : defaultValue;
  }

  public static String getString(Config config, String key, String defaultValue) {
    return config.hasPath(key) ? config.getString(key) : defaultValue;
  }
}
