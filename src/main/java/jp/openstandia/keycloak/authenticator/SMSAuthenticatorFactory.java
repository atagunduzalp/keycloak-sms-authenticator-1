package jp.openstandia.keycloak.authenticator;

import java.util.List;

import org.jboss.logging.Logger;
import org.keycloak.Config.Scope;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.authentication.ConfigurableAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationExecutionModel.Requirement;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

public class SMSAuthenticatorFactory implements AuthenticatorFactory, ConfigurableAuthenticatorFactory {

	public static final String PROVIDER_ID = "sms-authenticator-with-twilio";
	private static final SMSAuthenticator SINGLETON = new SMSAuthenticator();
	private static final Logger logger = Logger.getLogger(SMSAuthenticatorFactory.class.getPackage().getName());

	private static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
			AuthenticationExecutionModel.Requirement.REQUIRED,
			AuthenticationExecutionModel.Requirement.DISABLED
	};

	private static final List<ProviderConfigProperty> configProperties;
	static {
        configProperties = ProviderConfigurationBuilder
                .create()
                .property()
                .name(Contstants.CONFIG_SMS_COUNTRY_CODE)
                .label("国番号")
                .type(ProviderConfigProperty.STRING_TYPE)
                .helpText("国際電気通信連合(ITU)によって割り当てられている国際電話番号(国番号)")
                .add()

                .property()
                .name(Contstants.CONFIG_SMS_API_KEY)
                .label("API-KEY")
                .type(ProviderConfigProperty.STRING_TYPE)
                .helpText("TwilioのSMS送信のためのAPI-KEY.")
                .add()

                .property()
                .name(Contstants.CONFIG_SMS_SEND_URL)
                .label("SMS発信用のURL")
                .type(ProviderConfigProperty.STRING_TYPE)
                .helpText("SMS発信用のURLを入力する")
                .add()

                .property()
                .name(Contstants.CONFIG_SMS_VERIFY_URL)
                .label("認証コード確認用のURL")
                .type(ProviderConfigProperty.STRING_TYPE)
                .helpText("認証コードの確認用のURLを入力する")
                .add()

                .build();
	}

	public Authenticator create(KeycloakSession session) {
		return SINGLETON;
	}

	public String getId() {
		return PROVIDER_ID;
	}

	public void init(Scope scope) {
		logger.debug("Method [init]");
	}

	public void postInit(KeycloakSessionFactory factory) {
		logger.debug("Method [postInit]");
	}

	public List<ProviderConfigProperty> getConfigProperties() {
		return configProperties;
	}

	public String getHelpText() {
		return "SMS Authenticate using Twilio.";
	}

	public String getDisplayType() {
		return "Twilio SMS Authentication.";
	}

	public String getReferenceCategory() {
		logger.debug("Method [getReferenceCategory]");
        return "sms-auth-code";
	}

	public boolean isConfigurable() {
		return true;
	}

	public Requirement[] getRequirementChoices() {
		return REQUIREMENT_CHOICES;
	}

	public boolean isUserSetupAllowed() {
		return true;
	}

	public void close() {
		logger.debug("<<<<<<<<<<<<<<< SMSAuthenticatorFactory close");
	}
}