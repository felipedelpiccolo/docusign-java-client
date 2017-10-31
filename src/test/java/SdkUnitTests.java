
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import com.docusign.esign.api.*;
import com.docusign.esign.api.EnvelopesApi.ListDocumentTabsOptions;
import com.docusign.esign.client.*;
//import com.docusign.esign.client.auth.AccessTokenListener;
import com.docusign.esign.model.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.awt.Desktop;
import junit.framework.Assert;

//import org.apache.oltu.oauth2.common.token.BasicOAuthToken;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.migcomponents.migbase64.Base64;

/**
 *
 * @author majid.mallis
 */
public class SdkUnitTests {

	public static final String UserName = "node_sdk@mailinator.com";
	public static final String Password = "qweqweasd";
	public static final String UserId = "fcc5726c-cd73-4844-b580-40bbbe6ca126";
	public static final String IntegratorKey = "ae30ea4e-3959-4d1c-b867-fcb57d2dc4df";
	public static final String ClientSecret = "b4dccdbe-232f-46cc-96c5-b2f0f7448f8f";
	public static final String RedirectURI = "https://www.docusign.com/api";

	public static final String BaseUrl = "https://demo.docusign.net/restapi";
	public static final String OAuthBaseUrl = "account-d.docusign.com";
	public static final String publicKeyFilename = "/src/test/keys/docusign_public_key.txt";
	public static final String privateKeyFilename = "/src/test/keys/docusign_private_key.txt";

	public static final String SignTest1File = "/src/test/docs/SignTest1.pdf";
	public static final String TemplateId = "cf2a46c2-8d6e-4258-9d62-752547b1a419";
	public String EnvelopeId = "df3242e8-9b24-4c3f-861f-125d99957384";
	// JUnit 4.12 runs test cases in parallel, so the envelope ID needs to be initiated as well.

	// private JSON json = new JSON();

	public SdkUnitTests() {
	}

	@BeforeClass
	public static void setUpClass() {
	}

	@AfterClass
	public static void tearDownClass() {
	}

	@Before
	public void setUp() {
	}

	@After
	public void tearDown() {
	}

	// TODO add test methods here.
	// The methods must be annotated with annotation @Test. For example:
	//
	@Test
	public void LoginTest() {

		ApiClient apiClient = new ApiClient(BaseUrl);
		String currentDir = System.getProperty("user.dir");
		
		try {
			// IMPORTANT NOTE:
			// the first time you ask for a JWT access token, you should grant access by making the following call
			// get DocuSign OAuth authorization url:
			//String oauthLoginUrl = apiClient.getJWTUri(IntegratorKey, RedirectURI, OAuthBaseUrl);
			// open DocuSign OAuth authorization url in the browser, login and grant access
			//Desktop.getDesktop().browse(URI.create(oauthLoginUrl));
			// END OF NOTE
			
			apiClient.configureJWTAuthorizationFlow(currentDir + publicKeyFilename, currentDir + privateKeyFilename, OAuthBaseUrl, IntegratorKey, UserId, 3600);
			Configuration.setDefaultApiClient(apiClient);

			AuthenticationApi authApi = new AuthenticationApi();
			AuthenticationApi.LoginOptions loginOps = authApi.new LoginOptions();
			loginOps.setApiPassword("true");
			loginOps.setIncludeAccountIdGuid("true");
			LoginInformation loginInfo = authApi.login(loginOps);

			Assert.assertNotSame(null, loginInfo);
			Assert.assertNotNull(loginInfo.getLoginAccounts());
			Assert.assertTrue(loginInfo.getLoginAccounts().size() > 0);
			List<LoginAccount> loginAccounts = loginInfo.getLoginAccounts();
			Assert.assertNotNull(loginAccounts.get(0).getAccountId());

			System.out.println("LoginInformation: " + loginInfo);

			// parse first account's baseUrl
			String[] accountDomain = loginInfo.getLoginAccounts().get(0).getBaseUrl().split("/v2");

			// below code required for production, no effect in demo (same
			// domain)
			apiClient.setBasePath(accountDomain[0]);
			Configuration.setDefaultApiClient(apiClient);
		} catch (ApiException ex) {
			System.out.println("Exception: " + ex);
		} catch (Exception e) {
			System.out.println("Exception: " + e.getLocalizedMessage());
		}
	}

	@Test
	public void OAuthLoginTest() {

		ApiClient apiClient = new ApiClient("https://" + OAuthBaseUrl, "docusignAccessCode", IntegratorKey, ClientSecret);
		apiClient.setBasePath(BaseUrl);
		// make sure to pass the redirect uri
		apiClient.configureAuthorizationFlow(IntegratorKey, ClientSecret, RedirectURI);
		Configuration.setDefaultApiClient(apiClient);

		try {
			// get DocuSign OAuth authorization url
			String oauthLoginUrl = apiClient.getAuthorizationUri();
			// open DocuSign OAuth login in the browser
			Desktop.getDesktop().browse(URI.create(oauthLoginUrl));
			// IMPORTANT: after the login, DocuSign will send back a fresh
			// authorization code as a query param of the redirect URI.
			// You should set up a route that handles the redirect call to get
			// that code and pass it to token endpoint as shown in the next
			// lines:
			String code = "<once_you_get_the_oauth_code_put_it_here>";
			// assign it to the token endpoint
			apiClient.getTokenEndPoint().setCode(code);
			// optionally register to get notified when a new token arrives
			/*apiClient.registerAccessTokenListener(new AccessTokenListener() {
				@Override
				public void notify(BasicOAuthToken token) {
					System.out.println("Got a fresh token: " + token.getAccessToken());
				}
			});
			// ask to exchange the auth code with an access code
			apiClient.updateAccessToken();

			// now that the API client has an OAuth token, let's use in all
			// DocuSign APIs
			AuthenticationApi authApi = new AuthenticationApi(apiClient);
			AuthenticationApi.LoginOptions loginOps = authApi.new LoginOptions();
			loginOps.setApiPassword("true");
			loginOps.setIncludeAccountIdGuid("true");
			LoginInformation loginInfo = authApi.login(loginOps);

			List<LoginAccount> loginAccounts = loginInfo.getLoginAccounts();

			System.out.println("LoginInformation: " + loginInfo);

			// parse first account's baseUrl
			String[] accountDomain = loginInfo.getLoginAccounts().get(0).getBaseUrl().split("/v2");

			// below code required for production, no effect in demo (same
			// domain)
			apiClient.setBasePath(accountDomain[0]);
			Configuration.setDefaultApiClient(apiClient);*/
		//} catch (ApiException ex) {
		//	System.out.println("Exception: " + ex);
		} catch (Exception e) {
			System.out.println("Exception: " + e.getLocalizedMessage());
		}
	}

	@Test
	public void RequestASignatureTest() {

		byte[] fileBytes = null;
		try {
			// String currentDir = new java.io.File(".").getCononicalPath();

			String currentDir = System.getProperty("user.dir");

			Path path = Paths.get(currentDir + SignTest1File);
			fileBytes = Files.readAllBytes(path);
		} catch (IOException ioExcp) {
			Assert.assertEquals(null, ioExcp);
		}

		// create an envelope to be signed
		EnvelopeDefinition envDef = new EnvelopeDefinition();
		envDef.setEmailSubject("Please Sign my Java SDK Envelope");
		envDef.setEmailBlurb("Hello, Please sign my Java SDK Envelope.");

		// add a document to the envelope
		Document doc = new Document();
		String base64Doc = Base64.encodeToString(fileBytes, false);
		doc.setDocumentBase64(base64Doc);
		doc.setName("TestFile.pdf");
		doc.setDocumentId("1");

		List<Document> docs = new ArrayList<Document>();
		docs.add(doc);
		envDef.setDocuments(docs);

		// Add a recipient to sign the document
		Signer signer = new Signer();
		signer.setEmail(UserName);
		signer.setName("Pat Developer");
		signer.setRecipientId("1");

		// Create a SignHere tab somewhere on the document for the signer to
		// sign
		SignHere signHere = new SignHere();
		signHere.setDocumentId("1");
		signHere.setPageNumber("1");
		signHere.setRecipientId("1");
		signHere.setXPosition("100");
		signHere.setYPosition("100");
		signHere.setScaleValue("0.5");

		List<SignHere> signHereTabs = new ArrayList<SignHere>();
		signHereTabs.add(signHere);
		Tabs tabs = new Tabs();
		tabs.setSignHereTabs(signHereTabs);
		signer.setTabs(tabs);

		// Above causes issue
		envDef.setRecipients(new Recipients());
		envDef.getRecipients().setSigners(new ArrayList<Signer>());
		envDef.getRecipients().getSigners().add(signer);

		// send the envelope (otherwise it will be "created" in the Draft folder
		envDef.setStatus("sent");

		ApiClient apiClient = new ApiClient(BaseUrl);
		String currentDir = System.getProperty("user.dir");
		
		try {
			// IMPORTANT NOTE:
			// the first time you ask for a JWT access token, you should grant access by making the following call
			// get DocuSign OAuth authorization url:
			//String oauthLoginUrl = apiClient.getJWTUri(IntegratorKey, RedirectURI, OAuthBaseUrl);
			// open DocuSign OAuth authorization url in the browser, login and grant access
			//Desktop.getDesktop().browse(URI.create(oauthLoginUrl));
			// END OF NOTE
			
			apiClient.configureJWTAuthorizationFlow(currentDir + publicKeyFilename, currentDir + privateKeyFilename, OAuthBaseUrl, IntegratorKey, UserId, 3600);
			Configuration.setDefaultApiClient(apiClient);

			AuthenticationApi authApi = new AuthenticationApi();

			AuthenticationApi.LoginOptions loginOptions = authApi.new LoginOptions();
			loginOptions.setApiPassword("true");
			loginOptions.setIncludeAccountIdGuid("true");
			LoginInformation loginInfo = authApi.login(loginOptions);

			Assert.assertNotNull(loginInfo);
			Assert.assertNotNull(loginInfo.getLoginAccounts());
			Assert.assertTrue(loginInfo.getLoginAccounts().size() > 0);
			List<LoginAccount> loginAccounts = loginInfo.getLoginAccounts();
			Assert.assertNotNull(loginAccounts.get(0).getAccountId());

			String accountId = loginInfo.getLoginAccounts().get(0).getAccountId();

			// parse first account's baseUrl
			String[] accountDomain = loginInfo.getLoginAccounts().get(0).getBaseUrl().split("/v2");

			// below code required for production, no effect in demo (same
			// domain)
			apiClient.setBasePath(accountDomain[0]);
			Configuration.setDefaultApiClient(apiClient);

			EnvelopesApi envelopesApi = new EnvelopesApi();

			EnvelopeSummary envelopeSummary = envelopesApi.createEnvelope(accountId, envDef);

			Assert.assertNotNull(envelopeSummary);
			Assert.assertNotNull(envelopeSummary.getEnvelopeId());
			Assert.assertEquals("sent", envelopeSummary.getStatus());

			System.out.println("EnvelopeSummary: " + envelopeSummary);

		} catch (ApiException ex) {
			System.out.println("Exception: " + ex);
		} catch (Exception e) {
			System.out.println("Exception: " + e.getLocalizedMessage());
		}
	}

	@Test
	public void RequestSignatureFromTemplate() {

		String templateRoleName = "Needs to sign";

		// create an envelope to be signed
		EnvelopeDefinition envDef = new EnvelopeDefinition();
		envDef.setEmailSubject("Please Sign my Java SDK Envelope");
		envDef.setEmailBlurb("Hello, Please sign my Java SDK Envelope.");

		/// assign template information including ID and role(s)
		envDef.setTemplateId(TemplateId);

		// create a template role with a valid templateId and roleName and
		// assign signer info
		TemplateRole tRole = new TemplateRole();
		tRole.setRoleName(templateRoleName);
		tRole.setName("Pat Developer");
		tRole.setEmail(UserName);

		// create a list of template roles and add our newly created role
		List<TemplateRole> templateRolesList = new ArrayList<TemplateRole>();
		templateRolesList.add(tRole);

		// assign template role(s) to the envelope
		envDef.setTemplateRoles(templateRolesList);

		// send the envelope by setting |status| to "sent". To save as a draft
		// set to "created"
		envDef.setStatus("sent");

		ApiClient apiClient = new ApiClient(BaseUrl);
		String currentDir = System.getProperty("user.dir");
		
		try {
			// IMPORTANT NOTE:
			// the first time you ask for a JWT access token, you should grant access by making the following call
			// get DocuSign OAuth authorization url:
			//String oauthLoginUrl = apiClient.getJWTUri(IntegratorKey, RedirectURI, OAuthBaseUrl);
			// open DocuSign OAuth authorization url in the browser, login and grant access
			//Desktop.getDesktop().browse(URI.create(oauthLoginUrl));
			// END OF NOTE
			
			apiClient.configureJWTAuthorizationFlow(currentDir + publicKeyFilename, currentDir + privateKeyFilename, OAuthBaseUrl, IntegratorKey, UserId, 3600);
			Configuration.setDefaultApiClient(apiClient);

			AuthenticationApi authApi = new AuthenticationApi();

			AuthenticationApi.LoginOptions loginOptions = authApi.new LoginOptions();
			loginOptions.setApiPassword("true");
			loginOptions.setIncludeAccountIdGuid("true");
			LoginInformation loginInfo = authApi.login(loginOptions);

			Assert.assertNotNull(loginInfo);
			Assert.assertNotNull(loginInfo.getLoginAccounts());
			Assert.assertTrue(loginInfo.getLoginAccounts().size() > 0);
			List<LoginAccount> loginAccounts = loginInfo.getLoginAccounts();
			Assert.assertNotNull(loginAccounts.get(0).getAccountId());

			String accountId = loginInfo.getLoginAccounts().get(0).getAccountId();

			// parse first account's baseUrl
			String[] accountDomain = loginInfo.getLoginAccounts().get(0).getBaseUrl().split("/v2");

			// below code required for production, no effect in demo (same
			// domain)
			apiClient.setBasePath(accountDomain[0]);
			Configuration.setDefaultApiClient(apiClient);

			EnvelopesApi envelopesApi = new EnvelopesApi();

			EnvelopeSummary envelopeSummary = envelopesApi.createEnvelope(accountId, envDef);

			Assert.assertNotNull(envelopeSummary);
			Assert.assertNotNull(envelopeSummary.getEnvelopeId());
			Assert.assertEquals("sent", envelopeSummary.getStatus());

			System.out.println("EnvelopeSummary: " + envelopeSummary);
			EnvelopeId = envelopeSummary.getEnvelopeId();

		} catch (ApiException ex) {
			System.out.println("Exception: " + ex);
		} catch (Exception e) {
			System.out.println("Exception: " + e.getLocalizedMessage());
		}
	}

	@Test
	public void EmbeddedSigningTest() {
		byte[] fileBytes = null;
		try {
			// String currentDir = new java.io.File(".").getCononicalPath();

			String currentDir = System.getProperty("user.dir");

			Path path = Paths.get(currentDir + SignTest1File);
			fileBytes = Files.readAllBytes(path);
		} catch (IOException ioExcp) {
			Assert.assertEquals(null, ioExcp);
		}

		// create an envelope to be signed
		EnvelopeDefinition envDef = new EnvelopeDefinition();
		envDef.setEmailSubject("Please Sign my Java SDK Envelope (Embedded Signer)");
		envDef.setEmailBlurb("Hello, Please sign my Java SDK Envelope.");

		// add a document to the envelope
		Document doc = new Document();
		String base64Doc = Base64.encodeToString(fileBytes, false);
		doc.setDocumentBase64(base64Doc);
		doc.setName("TestFile.pdf");
		doc.setDocumentId("1");

		List<Document> docs = new ArrayList<Document>();
		docs.add(doc);
		envDef.setDocuments(docs);

		// Add a recipient to sign the document
		Signer signer = new Signer();
		signer.setEmail(UserName);
		String name = "Pat Developer";
		signer.setName(name);
		signer.setRecipientId("1");

		// this value represents the client's unique identifier for the signer
		String clientUserId = "2939";
		signer.setClientUserId(clientUserId);

		// Create a SignHere tab somewhere on the document for the signer to
		// sign
		SignHere signHere = new SignHere();
		signHere.setDocumentId("1");
		signHere.setPageNumber("1");
		signHere.setRecipientId("1");
		signHere.setXPosition("100");
		signHere.setYPosition("100");
		signHere.setScaleValue("0.5");

		List<SignHere> signHereTabs = new ArrayList<SignHere>();
		signHereTabs.add(signHere);
		Tabs tabs = new Tabs();
		tabs.setSignHereTabs(signHereTabs);
		signer.setTabs(tabs);

		// Above causes issue
		envDef.setRecipients(new Recipients());
		envDef.getRecipients().setSigners(new ArrayList<Signer>());
		envDef.getRecipients().getSigners().add(signer);

		// send the envelope (otherwise it will be "created" in the Draft folder
		envDef.setStatus("sent");

		ApiClient apiClient = new ApiClient(BaseUrl);
		String currentDir = System.getProperty("user.dir");
		
		try {
			// IMPORTANT NOTE:
			// the first time you ask for a JWT access token, you should grant access by making the following call
			// get DocuSign OAuth authorization url:
			//String oauthLoginUrl = apiClient.getJWTUri(IntegratorKey, RedirectURI, OAuthBaseUrl);
			// open DocuSign OAuth authorization url in the browser, login and grant access
			//Desktop.getDesktop().browse(URI.create(oauthLoginUrl));
			// END OF NOTE
			
			apiClient.configureJWTAuthorizationFlow(currentDir + publicKeyFilename, currentDir + privateKeyFilename, OAuthBaseUrl, IntegratorKey, UserId, 3600);
			Configuration.setDefaultApiClient(apiClient);

			AuthenticationApi authApi = new AuthenticationApi();
			LoginInformation loginInfo = authApi.login();

			Assert.assertNotSame(null, loginInfo);
			Assert.assertNotNull(loginInfo.getLoginAccounts());
			Assert.assertTrue(loginInfo.getLoginAccounts().size() > 0);
			List<LoginAccount> loginAccounts = loginInfo.getLoginAccounts();
			Assert.assertNotNull(loginAccounts.get(0).getAccountId());

			String accountId = loginInfo.getLoginAccounts().get(0).getAccountId();

			// parse first account's baseUrl
			String[] accountDomain = loginInfo.getLoginAccounts().get(0).getBaseUrl().split("/v2");

			// below code required for production, no effect in demo (same
			// domain)
			apiClient.setBasePath(accountDomain[0]);
			Configuration.setDefaultApiClient(apiClient);

			EnvelopesApi envelopesApi = new EnvelopesApi();
			EnvelopeSummary envelopeSummary = envelopesApi.createEnvelope(accountId, envDef);

			Assert.assertNotNull(envelopeSummary);
			Assert.assertNotNull(envelopeSummary.getEnvelopeId());

			System.out.println("EnvelopeSummary: " + envelopeSummary);

			String returnUrl = "http://www.docusign.com/developer-center";
			RecipientViewRequest recipientView = new RecipientViewRequest();
			recipientView.setReturnUrl(returnUrl);
			recipientView.setClientUserId(clientUserId);
			recipientView.setAuthenticationMethod("email");
			recipientView.setUserName(name);
			recipientView.setEmail(UserName);

			ViewUrl viewUrl = envelopesApi.createRecipientView(loginInfo.getLoginAccounts().get(0).getAccountId(),
					envelopeSummary.getEnvelopeId(), recipientView);

			Assert.assertNotNull(viewUrl);
			Assert.assertNotNull(viewUrl.getUrl());

			// This Url should work in an Iframe or browser to allow signing
			System.out.println("ViewUrl is " + viewUrl);

		} catch (ApiException ex) {
			System.out.println("Exception: " + ex);
		} catch (Exception e) {
			System.out.println("Exception: " + e.getLocalizedMessage());
		}

	}

	@Test
	public void CreateTemplateTest() {

		byte[] fileBytes = null;
		File f = null;
		try {
			// String currentDir = new java.io.File(".").getCononicalPath();

			String currentDir = System.getProperty("user.dir");

			Path path = Paths.get(currentDir + SignTest1File);
			fileBytes = Files.readAllBytes(path);

			f = new File(path.toString());
		} catch (IOException ioExcp) {
			Assert.assertEquals(null, ioExcp);
		}

		// create an envelope to be signed

		EnvelopeTemplate templateDef = new EnvelopeTemplate();
		templateDef.setEmailSubject("Please Sign my Java SDK Envelope");
		templateDef.setEmailBlurb("Hello, Please sign my Java SDK Envelope.");

		// add a document to the envelope
		Document doc = new Document();
		String base64Doc = Base64.encodeToString(fileBytes, false);
		doc.setDocumentBase64(base64Doc);
		doc.setName("TestFile.pdf");
		doc.setDocumentId("1");

		List<Document> docs = new ArrayList<Document>();
		docs.add(doc);
		templateDef.setDocuments(docs);

		// Add a recipient to sign the document
		Signer signer = new Signer();
		signer.setRoleName("Signer1");
		signer.setRecipientId("1");

		// Create a SignHere tab somewhere on the document for the signer to
		// sign
		SignHere signHere = new SignHere();
		signHere.setDocumentId("1");
		signHere.setPageNumber("1");
		signHere.setRecipientId("1");
		signHere.setXPosition("100");
		signHere.setYPosition("100");
		signHere.setScaleValue("0.5");

		List<SignHere> signHereTabs = new ArrayList<SignHere>();
		signHereTabs.add(signHere);
		Tabs tabs = new Tabs();
		tabs.setSignHereTabs(signHereTabs);
		signer.setTabs(tabs);

		templateDef.setRecipients(new Recipients());
		templateDef.getRecipients().setSigners(new ArrayList<Signer>());
		templateDef.getRecipients().getSigners().add(signer);

		EnvelopeTemplateDefinition envTemplateDef = new EnvelopeTemplateDefinition();
		envTemplateDef.setName("myTemplate");
		templateDef.setEnvelopeTemplateDefinition(envTemplateDef);

		ApiClient apiClient = new ApiClient(BaseUrl);
		String currentDir = System.getProperty("user.dir");
		
		try {
			// IMPORTANT NOTE:
			// the first time you ask for a JWT access token, you should grant access by making the following call
			// get DocuSign OAuth authorization url:
			//String oauthLoginUrl = apiClient.getJWTUri(IntegratorKey, RedirectURI, OAuthBaseUrl);
			// open DocuSign OAuth authorization url in the browser, login and grant access
			//Desktop.getDesktop().browse(URI.create(oauthLoginUrl));
			// END OF NOTE
			
			apiClient.configureJWTAuthorizationFlow(currentDir + publicKeyFilename, currentDir + privateKeyFilename, OAuthBaseUrl, IntegratorKey, UserId, 3600);
			Configuration.setDefaultApiClient(apiClient);

			AuthenticationApi authApi = new AuthenticationApi();
			LoginInformation loginInfo = authApi.login();

			Assert.assertNotNull(loginInfo);
			Assert.assertNotNull(loginInfo.getLoginAccounts());
			Assert.assertTrue(loginInfo.getLoginAccounts().size() > 0);
			List<LoginAccount> loginAccounts = loginInfo.getLoginAccounts();
			Assert.assertNotNull(loginAccounts.get(0).getAccountId());

			String accountId = loginInfo.getLoginAccounts().get(0).getAccountId();

			// parse first account's baseUrl
			String[] accountDomain = loginInfo.getLoginAccounts().get(0).getBaseUrl().split("/v2");

			// below code required for production, no effect in demo (same
			// domain)
			apiClient.setBasePath(accountDomain[0]);
			Configuration.setDefaultApiClient(apiClient);

			TemplatesApi templatesApi = new TemplatesApi();
			TemplateSummary templateSummary = templatesApi.createTemplate(accountId, templateDef);

			Assert.assertNotNull(templateSummary);
			Assert.assertNotNull(templateSummary.getTemplateId());

			System.out.println("TemplateSummary: " + templateSummary);

		} catch (ApiException ex) {
			System.out.println("Exception: " + ex);
		} catch (Exception e) {
			System.out.println("Exception: " + e.getLocalizedMessage());
		}
	}

	@Test
	public void DownLoadEnvelopeDocumentsTest() {


		byte[] fileBytes = null;
		try {
			// String currentDir = new java.io.File(".").getCononicalPath();

			String currentDir = System.getProperty("user.dir");

			Path path = Paths.get(currentDir + SignTest1File);
			fileBytes = Files.readAllBytes(path);
		} catch (IOException ioExcp) {
			Assert.assertEquals(null, ioExcp);
		}

		// create an envelope to be signed
		EnvelopeDefinition envDef = new EnvelopeDefinition();
		envDef.setEmailSubject("Please Sign my Java SDK Envelope");
		envDef.setEmailBlurb("Hello, Please sign my Java SDK Envelope.");

		// add a document to the envelope
		Document doc = new Document();
		String base64Doc = Base64.encodeToString(fileBytes, false);
		doc.setDocumentBase64(base64Doc);
		doc.setName("TestFile.pdf");
		doc.setDocumentId("1");

		List<Document> docs = new ArrayList<Document>();
		docs.add(doc);
		envDef.setDocuments(docs);

		// Add a recipient to sign the document
		Signer signer = new Signer();
		signer.setEmail(UserName);
		String name = "Pat Developer";
		signer.setName(name);
		signer.setRecipientId("1");

		// this value represents the client's unique identifier for the signer
		String clientUserId = "2939";
		signer.setClientUserId(clientUserId);

		// Create a SignHere tab somewhere on the document for the signer to
		// sign
		Text text = new Text();
		text.setDocumentId("1");
		text.setPageNumber("1");
		text.setRecipientId("1");
		text.setXPosition("100");
		text.setYPosition("100");

		List<Text> textTabs = new ArrayList<Text>();
		textTabs.add(text);
		Tabs tabs = new Tabs();
		tabs.setTextTabs(textTabs);
		signer.setTabs(tabs);

		// Above causes issue
		envDef.setRecipients(new Recipients());
		envDef.getRecipients().setSigners(new ArrayList<Signer>());
		envDef.getRecipients().getSigners().add(signer);

		// send the envelope (otherwise it will be "created" in the Draft folder
		envDef.setStatus("sent");

		try {

			ApiClient apiClient = new ApiClient();
			apiClient.setBasePath(BaseUrl);

			String creds = createAuthHeaderCreds(UserName, Password, IntegratorKey);
			apiClient.addDefaultHeader("X-DocuSign-Authentication", creds);
			Configuration.setDefaultApiClient(apiClient);

			AuthenticationApi authApi = new AuthenticationApi();
			LoginInformation loginInfo = authApi.login();

			Assert.assertNotSame(null, loginInfo);
			Assert.assertNotNull(loginInfo.getLoginAccounts());
			Assert.assertTrue(loginInfo.getLoginAccounts().size() > 0);
			List<LoginAccount> loginAccounts = loginInfo.getLoginAccounts();
			Assert.assertNotNull(loginAccounts.get(0).getAccountId());

			String accountId = loginInfo.getLoginAccounts().get(0).getAccountId();

			// parse first account's baseUrl
			String[] accountDomain = loginInfo.getLoginAccounts().get(0).getBaseUrl().split("/v2");

			// below code required for production, no effect in demo (same
			// domain)
			apiClient.setBasePath(accountDomain[0]);
			Configuration.setDefaultApiClient(apiClient);

			EnvelopesApi envelopesApi = new EnvelopesApi();
			EnvelopeSummary envelopeSummary = envelopesApi.createEnvelope(accountId, envDef);

			Assert.assertNotNull(envelopeSummary);
			Assert.assertNotNull(envelopeSummary.getEnvelopeId());
			EnvelopeId = envelopeSummary.getEnvelopeId();

			System.out.println("EnvelopeSummary: " + envelopeSummary);

			byte[] pdfBytes = envelopesApi.getDocument(accountId, envelopeSummary.getEnvelopeId(), "combined");
			Assert.assertTrue(pdfBytes.length > 0);
			/*
			 * try {
			 * 
			 * File pdfFile = File.createTempFile("ds_", "pdf", null);
			 * FileOutputStream fos = new FileOutputStream(pdfFile);
			 * fos.write(pdfBytes);
			 * 
			 * // show the PDF Desktop.getDesktop().open(pdfFile);
			 * 
			 * 
			 * } catch (Exception ex) {
			 * Assert.fail("Could not create pdf File");
			 * 
			 * }
			 */

		} catch (ApiException ex) {
			System.out.println("Exception: " + ex);
		} catch (Exception e) {
			System.out.println("Exception: " + e.getLocalizedMessage());
		}

	}

	@Test
	public void ListDocumentsTest() {


		ApiClient apiClient = new ApiClient(BaseUrl);
		String currentDir = System.getProperty("user.dir");
		
		try {
			// IMPORTANT NOTE:
			// the first time you ask for a JWT access token, you should grant access by making the following call
			// get DocuSign OAuth authorization url:
			//String oauthLoginUrl = apiClient.getJWTUri(IntegratorKey, RedirectURI, OAuthBaseUrl);
			// open DocuSign OAuth authorization url in the browser, login and grant access
			//Desktop.getDesktop().browse(URI.create(oauthLoginUrl));
			// END OF NOTE
			
			apiClient.configureJWTAuthorizationFlow(currentDir + publicKeyFilename, currentDir + privateKeyFilename, OAuthBaseUrl, IntegratorKey, UserId, 3600);
			Configuration.setDefaultApiClient(apiClient);

			AuthenticationApi authApi = new AuthenticationApi();
			LoginInformation loginInfo = authApi.login();

			Assert.assertNotNull(loginInfo);
			Assert.assertNotNull(loginInfo.getLoginAccounts());
			Assert.assertTrue(loginInfo.getLoginAccounts().size() > 0);
			List<LoginAccount> loginAccounts = loginInfo.getLoginAccounts();
			Assert.assertNotNull(loginAccounts.get(0).getAccountId());

			String accountId = loginInfo.getLoginAccounts().get(0).getAccountId();

			// parse first account's baseUrl
			String[] accountDomain = loginInfo.getLoginAccounts().get(0).getBaseUrl().split("/v2");

			// below code required for production, no effect in demo (same
			// domain)
			apiClient.setBasePath(accountDomain[0]);
			Configuration.setDefaultApiClient(apiClient);

			EnvelopesApi envelopesApi = new EnvelopesApi();

			EnvelopeDocumentsResult docsList = envelopesApi.listDocuments(accountId, EnvelopeId);
			Assert.assertNotNull(docsList);
			Assert.assertEquals(EnvelopeId, docsList.getEnvelopeId());

			System.out.println("EnvelopeDocumentsResult: " + docsList);
		} catch (ApiException ex) {
			System.out.println("Exception: " + ex);
		} catch (Exception e) {
			System.out.println("Exception: " + e.getLocalizedMessage());
		}
	}

	@Test
	public void ResendEnvelopeTest() {
		byte[] fileBytes = null;
		try {
			// String currentDir = new java.io.File(".").getCononicalPath();

			String currentDir = System.getProperty("user.dir");

			Path path = Paths.get(currentDir + SignTest1File);
			fileBytes = Files.readAllBytes(path);
		} catch (IOException ioExcp) {
			Assert.assertEquals(null, ioExcp);
		}

		// create an envelope to be signed
		EnvelopeDefinition envDef = new EnvelopeDefinition();
		envDef.setEmailSubject("Please Sign my Java SDK Envelope");
		envDef.setEmailBlurb("Hello, Please sign my Java SDK Envelope.");

		// add a document to the envelope
		Document doc = new Document();
		String base64Doc = Base64.encodeToString(fileBytes, false);
		doc.setDocumentBase64(base64Doc);
		doc.setName("TestFile.pdf");
		doc.setDocumentId("1");

		List<Document> docs = new ArrayList<Document>();
		docs.add(doc);
		envDef.setDocuments(docs);

		// Add a recipient to sign the document
		Signer signer = new Signer();
		signer.setEmail(UserName);
		String name = "Pat Developer";
		signer.setName(name);
		signer.setRecipientId("1");

		// this value represents the client's unique identifier for the signer
		String clientUserId = "2939";
		signer.setClientUserId(clientUserId);

		// Create a SignHere tab somewhere on the document for the signer to
		// sign
		SignHere signHere = new SignHere();
		signHere.setDocumentId("1");
		signHere.setPageNumber("1");
		signHere.setRecipientId("1");
		signHere.setXPosition("100");
		signHere.setYPosition("100");
		signHere.setScaleValue("0.5");

		List<SignHere> signHereTabs = new ArrayList<SignHere>();
		signHereTabs.add(signHere);
		Tabs tabs = new Tabs();
		tabs.setSignHereTabs(signHereTabs);
		signer.setTabs(tabs);

		// Above causes issue
		Recipients recipients = new Recipients();
		recipients.setSigners(new ArrayList<Signer>());
		recipients.getSigners().add(signer);
		envDef.setRecipients(recipients);

		envDef.setStatus("sent");

		ApiClient apiClient = new ApiClient(BaseUrl);
		String currentDir = System.getProperty("user.dir");
		
		try {
			// IMPORTANT NOTE:
			// the first time you ask for a JWT access token, you should grant access by making the following call
			// get DocuSign OAuth authorization url:
			//String oauthLoginUrl = apiClient.getJWTUri(IntegratorKey, RedirectURI, OAuthBaseUrl);
			// open DocuSign OAuth authorization url in the browser, login and grant access
			//Desktop.getDesktop().browse(URI.create(oauthLoginUrl));
			// END OF NOTE
			
			apiClient.configureJWTAuthorizationFlow(currentDir + publicKeyFilename, currentDir + privateKeyFilename, OAuthBaseUrl, IntegratorKey, UserId, 3600);
			Configuration.setDefaultApiClient(apiClient);

			AuthenticationApi authApi = new AuthenticationApi();
			LoginInformation loginInfo = authApi.login();

			Assert.assertNotNull(loginInfo);
			Assert.assertNotNull(loginInfo.getLoginAccounts());
			Assert.assertTrue(loginInfo.getLoginAccounts().size() > 0);
			List<LoginAccount> loginAccounts = loginInfo.getLoginAccounts();
			Assert.assertNotNull(loginAccounts.get(0).getAccountId());

			String accountId = loginInfo.getLoginAccounts().get(0).getAccountId();

			// parse first account's baseUrl
			String[] accountDomain = loginInfo.getLoginAccounts().get(0).getBaseUrl().split("/v2");

			// below code required for production, no effect in demo (same
			// domain)
			apiClient.setBasePath(accountDomain[0]);
			Configuration.setDefaultApiClient(apiClient);

			EnvelopesApi envelopesApi = new EnvelopesApi();
			EnvelopeSummary envelopeSummary = envelopesApi.createEnvelope(accountId, envDef);

			Assert.assertNotNull(envelopeSummary);
			Assert.assertNotNull(envelopeSummary.getEnvelopeId());

			System.out.println("EnvelopeSummary: " + envelopeSummary);

			EnvelopesApi.UpdateRecipientsOptions updateRecipientsOptions = envelopesApi.new UpdateRecipientsOptions();
			updateRecipientsOptions.setResendEnvelope("true");

			RecipientsUpdateSummary recipientsUpdateSummary = envelopesApi.updateRecipients(accountId,
					envelopeSummary.getEnvelopeId(), recipients, updateRecipientsOptions);
			Assert.assertNotNull(recipientsUpdateSummary);
			Assert.assertNotNull(recipientsUpdateSummary.getRecipientUpdateResults().size() > 0);
			Assert.assertEquals("SUCCESS",
					recipientsUpdateSummary.getRecipientUpdateResults().get(0).getErrorDetails().getErrorCode());
			System.out.println("RecipientsUpdateSummary: " + recipientsUpdateSummary);
		} catch (ApiException ex) {
			System.out.println("Exception: " + ex);
		} catch (Exception e) {
			System.out.println("Exception: " + e.getLocalizedMessage());
		}
	}

	@Test
	public void GetDiagnosticLogsTest() {

		byte[] fileBytes = null;
		try {
			// String currentDir = new java.io.File(".").getCononicalPath();

			String currentDir = System.getProperty("user.dir");

			Path path = Paths.get(currentDir + SignTest1File);
			fileBytes = Files.readAllBytes(path);
		} catch (IOException ioExcp) {
			Assert.assertEquals(null, ioExcp);
		}

		// create an envelope to be signed
		EnvelopeDefinition envDef = new EnvelopeDefinition();
		envDef.setEmailSubject("Please Sign my Java SDK Envelope");
		envDef.setEmailBlurb("Hello, Please sign my Java SDK Envelope.");

		// add a document to the envelope
		Document doc = new Document();
		String base64Doc = Base64.encodeToString(fileBytes, false);
		doc.setDocumentBase64(base64Doc);
		doc.setName("TestFile.pdf");
		doc.setDocumentId("1");

		List<Document> docs = new ArrayList<Document>();
		docs.add(doc);
		envDef.setDocuments(docs);

		// Add a recipient to sign the document
		Signer signer = new Signer();
		signer.setEmail(UserName);
		String name = "Pat Developer";
		signer.setName(name);
		signer.setRecipientId("1");

		// this value represents the client's unique identifier for the signer
		String clientUserId = "2939";
		signer.setClientUserId(clientUserId);

		// Create a SignHere tab somewhere on the document for the signer to
		// sign
		SignHere signHere = new SignHere();
		signHere.setDocumentId("1");
		signHere.setPageNumber("1");
		signHere.setRecipientId("1");
		signHere.setXPosition("100");
		signHere.setYPosition("100");
		signHere.setScaleValue("0.5");

		List<SignHere> signHereTabs = new ArrayList<SignHere>();
		signHereTabs.add(signHere);
		Tabs tabs = new Tabs();
		tabs.setSignHereTabs(signHereTabs);
		signer.setTabs(tabs);

		// Above causes issue
		envDef.setRecipients(new Recipients());
		envDef.getRecipients().setSigners(new ArrayList<Signer>());
		envDef.getRecipients().getSigners().add(signer);

		// send the envelope (otherwise it will be "created" in the Draft folder
		envDef.setStatus("sent");

		ApiClient apiClient = new ApiClient(BaseUrl);
		String currentDir = System.getProperty("user.dir");
		
		try {
			// IMPORTANT NOTE:
			// the first time you ask for a JWT access token, you should grant access by making the following call
			// get DocuSign OAuth authorization url:
			//String oauthLoginUrl = apiClient.getJWTUri(IntegratorKey, RedirectURI, OAuthBaseUrl);
			// open DocuSign OAuth authorization url in the browser, login and grant access
			//Desktop.getDesktop().browse(URI.create(oauthLoginUrl));
			// END OF NOTE
			
			apiClient.configureJWTAuthorizationFlow(currentDir + publicKeyFilename, currentDir + privateKeyFilename, OAuthBaseUrl, IntegratorKey, UserId, 3600);
			Configuration.setDefaultApiClient(apiClient);

			AuthenticationApi authApi = new AuthenticationApi();
			LoginInformation loginInfo = authApi.login();

			Assert.assertNotSame(null, loginInfo);
			Assert.assertNotNull(loginInfo.getLoginAccounts());
			Assert.assertTrue(loginInfo.getLoginAccounts().size() > 0);
			List<LoginAccount> loginAccounts = loginInfo.getLoginAccounts();
			Assert.assertNotNull(loginAccounts.get(0).getAccountId());

			String accountId = loginInfo.getLoginAccounts().get(0).getAccountId();

			// parse first account's baseUrl
			String[] accountDomain = loginInfo.getLoginAccounts().get(0).getBaseUrl().split("/v2");

			// below code required for production, no effect in demo (same
			// domain)
			apiClient.setBasePath(accountDomain[0]);
			Configuration.setDefaultApiClient(apiClient);

			DiagnosticsApi diagApi = new DiagnosticsApi();

			DiagnosticsSettingsInformation diagSettings = new DiagnosticsSettingsInformation();
			diagSettings.setApiRequestLogging("true");
			diagApi.updateRequestLogSettings(diagSettings);

			EnvelopesApi envelopesApi = new EnvelopesApi();
			EnvelopeSummary envelopeSummary = envelopesApi.createEnvelope(accountId, envDef);

			Assert.assertNotNull(envelopeSummary);
			Assert.assertNotNull(envelopeSummary.getEnvelopeId());

			System.out.println("EnvelopeSummary: " + envelopeSummary);

			byte[] pdfBytes = envelopesApi.getDocument(accountId, envelopeSummary.getEnvelopeId(), "combined");
			Assert.assertTrue(pdfBytes.length > 0);

			/*
			 * try {
			 * 
			 * File pdfFile = File.createTempFile("ds_", "pdf", null);
			 * FileOutputStream fos = new FileOutputStream(pdfFile);
			 * fos.write(pdfBytes);
			 * 
			 * // show the PDF Desktop.getDesktop().open(pdfFile);
			 * 
			 * 
			 * } catch (Exception ex) {
			 * Assert.fail("Could not create pdf File");
			 * 
			 * }
			 */

			ApiRequestLogsResult logsList = diagApi.listRequestLogs();
			String requestLogId = logsList.getApiRequestLogs().get(0).getRequestLogId();
			byte[] diagBytes = diagApi.getRequestLog(requestLogId);
			Assert.assertTrue(diagBytes.length > 0);

			/*
			 * try {
			 * 
			 * File diagFile = File.createTempFile("ds_", "txt", null);
			 * FileOutputStream fos = new FileOutputStream(diagFile);
			 * fos.write(diagBytes);
			 * 
			 * // show the PDF Desktop.getDesktop().open(diagFile);
			 * 
			 * 
			 * } catch (Exception ex) {
			 * Assert.fail("Could not create diag log File");
			 * 
			 * }
			 */

		} catch (ApiException ex) {
			System.out.println("Exception: " + ex);
		} catch (Exception e) {
			System.out.println("Exception: " + e.getLocalizedMessage());
		}

	}

	@Test
	public void ListDocumentTabsTest() {
	  byte[] fileBytes = null;
    try {
      // String currentDir = new java.io.File(".").getCononicalPath();

      String currentDir = System.getProperty("user.dir");

      Path path = Paths.get(currentDir + SignTest1File);
      fileBytes = Files.readAllBytes(path);
    } catch (IOException ioExcp) {
      Assert.assertEquals(null, ioExcp);
    }

    // create an envelope to be signed
    EnvelopeDefinition envDef = new EnvelopeDefinition();
    envDef.setEmailSubject("Please Sign my Java SDK Envelope");
    envDef.setEmailBlurb("Hello, Please sign my Java SDK Envelope.");

    // add a document to the envelope
    Document doc = new Document();
    String base64Doc = Base64.encodeToString(fileBytes, false);
    doc.setDocumentBase64(base64Doc);
    doc.setName("TestFile.pdf");
    doc.setDocumentId("1");

    List<Document> docs = new ArrayList<Document>();
    docs.add(doc);
    envDef.setDocuments(docs);

    // Add a recipient to sign the document
    Signer signer = new Signer();
    signer.setEmail(UserName);
    String name = "Pat Developer";
    signer.setName(name);
    signer.setRecipientId("1");

    // this value represents the client's unique identifier for the signer
    String clientUserId = "2939";
    signer.setClientUserId(clientUserId);

    // Create a SignHere tab somewhere on the document for the signer to
    // sign
    SignHere signHere = new SignHere();
    signHere.setDocumentId("1");
    signHere.setPageNumber("1");
    signHere.setRecipientId("1");
    signHere.setXPosition("100");
    signHere.setYPosition("100");
    signHere.setScaleValue("0.5");
    
    List<SignHere> signHereTabs = new ArrayList<SignHere>();
    signHereTabs.add(signHere);
    Tabs tabs = new Tabs();
    tabs.setSignHereTabs(signHereTabs);
    signer.setTabs(tabs);

    // Above causes issue
    envDef.setRecipients(new Recipients());
    envDef.getRecipients().setSigners(new ArrayList<Signer>());
    envDef.getRecipients().getSigners().add(signer);

    // send the envelope (otherwise it will be "created" in the Draft folder
    envDef.setStatus("sent");
	  
    ApiClient apiClient = new ApiClient(BaseUrl);
    String currentDir = System.getProperty("user.dir");
    
    try {
      // IMPORTANT NOTE:
      // the first time you ask for a JWT access token, you should grant access by making the following call
      // get DocuSign OAuth authorization url:
      //String oauthLoginUrl = apiClient.getJWTUri(IntegratorKey, RedirectURI, OAuthBaseUrl);
      // open DocuSign OAuth authorization url in the browser, login and grant access
      //Desktop.getDesktop().browse(URI.create(oauthLoginUrl));
      // END OF NOTE
      
      apiClient.configureJWTAuthorizationFlow(currentDir + publicKeyFilename, currentDir + privateKeyFilename, OAuthBaseUrl, IntegratorKey, UserId, 3600);
      Configuration.setDefaultApiClient(apiClient);

      AuthenticationApi authApi = new AuthenticationApi();
      LoginInformation loginInfo = authApi.login();

      Assert.assertNotNull(loginInfo);
      Assert.assertNotNull(loginInfo.getLoginAccounts());
      Assert.assertTrue(loginInfo.getLoginAccounts().size() > 0);
      List<LoginAccount> loginAccounts = loginInfo.getLoginAccounts();
      Assert.assertNotNull(loginAccounts.get(0).getAccountId());

      String accountId = loginInfo.getLoginAccounts().get(0).getAccountId();

      // parse first account's baseUrl
      String[] accountDomain = loginInfo.getLoginAccounts().get(0).getBaseUrl().split("/v2");

      // below code required for production, no effect in demo (same
      // domain)
      apiClient.setBasePath(accountDomain[0]);
      Configuration.setDefaultApiClient(apiClient);

      EnvelopesApi envelopesApi = new EnvelopesApi();

      EnvelopeSummary envelopeSummary = envelopesApi.createEnvelope(accountId, envDef);

      Assert.assertNotNull(envelopeSummary);
      Assert.assertNotNull(envelopeSummary.getEnvelopeId());

      System.out.println("EnvelopeSummary: " + envelopeSummary);
      
      ListDocumentTabsOptions options = envelopesApi. new ListDocumentTabsOptions();
      options.setPageNumbers(Arrays.asList("1"));
      Tabs docTabs = envelopesApi.listDocumentTabs(accountId, envelopeSummary.getEnvelopeId(), "1", options);
      Assert.assertNotNull(docTabs);
      Assert.assertEquals(tabs.getSignHereTabs().size(), docTabs.getSignHereTabs().size());

      System.out.println("EnvelopeDocumentTabsResult: " + docTabs);
      
      Tabs docPageTabs = envelopesApi.listDocumentPageTabs(accountId, envelopeSummary.getEnvelopeId(), "1", "1");
      Assert.assertNotNull(docPageTabs);
      Assert.assertEquals(docTabs, docPageTabs);
      
      System.out.println("EnvelopeDocumentPageTabsResult: " + docPageTabs);      
    } catch (ApiException ex) {
      System.out.println("Exception: " + ex);
    } catch (Exception e) {
      System.out.println("Exception: " + e.getLocalizedMessage());
    }
	}
	
	private String createAuthHeaderCreds(String userName, String password, String integratorKey) {
		DocuSignCredentials dsCreds = new DocuSignCredentials(UserName, Password, IntegratorKey);

		String creds = null;
		try {
			ObjectMapper mapper = new ObjectMapper();
			creds = mapper.writeValueAsString(dsCreds);
		} catch (JsonProcessingException ex) {
			creds = "";
		}

		return creds;
	}
}
