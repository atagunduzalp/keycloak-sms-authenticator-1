package jp.openstandia.keycloak.authenticator;

import java.util.List;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;

import jp.openstandia.keycloak.authenticator.api.SMSSendVerify;

public class SMSAuthenticator implements Authenticator {

	private static final Logger logger = Logger.getLogger(SMSAuthenticator.class.getPackage().getName());

	public void authenticate(AuthenticationFlowContext context) {
		logger.info("Method [authenticate]");

		MultivaluedMap<String, String> formParameters = context.getHttpRequest().getFormParameters();
		for (String key : formParameters.keySet()) {
			logger.info("parameter key: " + key);
			logger.info("parameter value: " + formParameters.getFirst(key));
		}

		AuthenticatorConfigModel config = context.getAuthenticatorConfig();
		UserModel user = context.getUser();

		List<String> iysCodeList = user.getAttribute(SMSAuthContstants.ATTR_IYS_CODE);
		for (String iysCode : iysCodeList) {
			logger.info("IYS code of user attribute: " + iysCode);
		}
		String iysCode = formParameters.getFirst(SMSAuthContstants.ATTR_IYS_CODE);
		if (iysCodeList.contains(iysCode)) {
			logger.info("IYS code accepted.");

			String phoneNumber = getPhoneNumber(user);
			logger.infov("phoneNumber : {0}", phoneNumber);

			if (phoneNumber != null) {

				// SendSMS
				SMSSendVerify sendVerify = new SMSSendVerify(
						getConfigString(config, SMSAuthContstants.CONFIG_SMS_API_KEY),
						getConfigString(config, SMSAuthContstants.CONFIG_PROXY_FLAG),
						getConfigString(config, SMSAuthContstants.CONFIG_PROXY_URL),
						getConfigString(config, SMSAuthContstants.CONFIG_PROXY_PORT),
						getConfigString(config, SMSAuthContstants.CONFIG_CODE_LENGTH));

				logger.info("send sms method starts: ");

				String sendSMSResponse = sendVerify.sendSMS(phoneNumber);
				if ("OK".equals(sendSMSResponse)) {
					Response challenge = context.form().createForm("sms-validation.ftl");
					context.challenge(challenge);

				} else if ("recipients".equals(sendSMSResponse)) {
					Response challenge = context.form().addError(new FormMessage("recipientsError"))
							.createForm("sms-validation-error.ftl");
					context.challenge(challenge);
				} else {
					Response challenge = context.form().addError(new FormMessage("sendSMSCodeErrorMessage"))
							.createForm("sms-validation-error.ftl");
					context.challenge(challenge);
				}

			} else {
				Response challenge = context.form().addError(new FormMessage("missingTelNumberMessage"))
						.createForm("sms-validation-error.ftl");
				context.challenge(challenge);
			}
		} else {
			logger.info("iys code wrong.");
			Response challenge = context.form().addError(new FormMessage("wrongIysCode"))
					.createForm("sms-validation-error.ftl");
			context.challenge(challenge);
		}

	}

	public void action(AuthenticationFlowContext context) {
		logger.info("Method [action]");

		MultivaluedMap<String, String> inputData = context.getHttpRequest().getDecodedFormParameters();
		String enteredCode = inputData.getFirst("smsCode");

		UserModel user = context.getUser();
		String phoneNumber = getPhoneNumber(user);
		logger.debugv("phoneNumber : {0}", phoneNumber);

		// SendSMS
		AuthenticatorConfigModel config = context.getAuthenticatorConfig();
		SMSSendVerify sendVerify = new SMSSendVerify(getConfigString(config, SMSAuthContstants.CONFIG_SMS_API_KEY),
				getConfigString(config, SMSAuthContstants.CONFIG_PROXY_FLAG),
				getConfigString(config, SMSAuthContstants.CONFIG_PROXY_URL),
				getConfigString(config, SMSAuthContstants.CONFIG_PROXY_PORT),
				getConfigString(config, SMSAuthContstants.CONFIG_CODE_LENGTH));

		String verifySMSResponse = sendVerify.verifySMS(phoneNumber, enteredCode);
		if ("OK".equals(verifySMSResponse)) {
			logger.info("verify code check : OK");
			context.success();

		} else {
			Response challenge = context.form()
					.setAttribute("username", context.getAuthenticationSession().getAuthenticatedUser().getUsername())
					.addError(new FormMessage("invalidSMSCodeMessage")).createForm("sms-validation-error.ftl");
			context.challenge(challenge);
		}

	}

	public boolean requiresUser() {
		logger.debug("Method [requiresUser]");
		return false;
	}

	public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
		logger.debug("Method [configuredFor]");
		return false;
	}

	public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {

	}

	public void close() {
		logger.debug("<<<<<<<<<<<<<<< SMSAuthenticator close");
	}

	private String getPhoneNumber(UserModel user) {
		List<String> phoneNumberList = user.getAttribute(SMSAuthContstants.ATTR_PHONE_NUMBER);
		if (phoneNumberList != null && !phoneNumberList.isEmpty()) {
			return phoneNumberList.get(0);
		}
		return null;
	}

	private String getConfigString(AuthenticatorConfigModel config, String configName) {
		String value = null;
		logger.info("config: " + config);
		if (config.getConfig() != null) {
			// Get value
			value = config.getConfig().get(configName);
		}
		return value;
	}
}