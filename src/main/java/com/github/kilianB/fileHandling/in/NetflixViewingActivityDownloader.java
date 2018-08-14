package com.github.kilianB.fileHandling.in;


//import org.openqa.selenium.By;
//import org.openqa.selenium.NoSuchElementException;
//import org.openqa.selenium.TimeoutException;
//import org.openqa.selenium.WebDriver;
//import org.openqa.selenium.WebElement;
//import org.openqa.selenium.chrome.ChromeDriver;
//import org.openqa.selenium.support.ui.ExpectedConditions;
//import org.openqa.selenium.support.ui.WebDriverWait;

/**	
 * 
 * Download the Netflix viewing history file from the server. This requires a Netflix session. We 
 * could achieve this nicely by asking the user for their credentials or session hijacking by extracting
 * their cookies.
 * 
 * As it's bothersome to configure selenimum drivers for such a small gain don't bother.
 * Maybe later as nice assignment.
 * 
 * 
 * @author Kilian
 *	Instead we could directly send the correct packages to netflix instead of relying on htmlunit?
 *	Maybe try out selenium?
 *	How about jaunt ? http://jaunt-api.com/
 */
@Deprecated
@SuppressWarnings("unused")
public class NetflixViewingActivityDownloader {

	private static final String INITIAL_URL = "https://www.netflix.com/browse";
	private static final String VIEWING_ACTIVITY_URL = "https://www.netflix.com/viewingactivity";

	public static void main(String[] args) {
		downloadNetflixViewingActivity("Kilian.Brachtendorf@t-online.de","DuVogel","Kili");
	}

	/**
	 * Hide default constructor
	 */
	private NetflixViewingActivityDownloader() {}

	/**
	 * 
	 * @param loginUsername
	 * @param loginPassword
	 * @param netflixUsername
	 * @return true if the file was sucessfully downloaded. False otherwise
	 */
	@Deprecated
	public static boolean downloadNetflixViewingActivity(String loginUsername, String loginPassword,
			String netflixUsername) {
		return false;
	}

	private boolean downloadUsingSelenium() {
		return false;
//		//Selenium
//		System.setProperty("webdriver.chrome.driver", "");
//		
//		WebDriver driver = new ChromeDriver();
//		driver.get(INITIAL_URL);
//		
//		
//		try {
//			WebElement form = driver.findElement(By.className("login-form"));
//			WebElement usernameField = driver.findElement(By.name("userLoginId"));
//			WebElement passwordField = driver.findElement(By.name("password"));
//			
//			//execute 
//			usernameField.sendKeys(loginUsername);
//			passwordField.sendKeys(loginPassword);
//			form.submit();
//			
//			//do everything else
//			WebDriverWait waitUntilResponse = new WebDriverWait(driver,10);
//			
//			try {
//				WebElement response = waitUntilResponse.until(ExpectedConditions.presenceOfElementLocated(By.className("profile-gate-label")));
//				
//				System.out.println("Login to netflix sucessful");
//				
//			}catch(TimeoutException timeout) {
//				System.out.println("Login wasn't sucessfull");
//				timeout.printStackTrace();
//				return false;
//			}
//			
//			return true;
//		}catch (NoSuchElementException exception) {
//			exception.printStackTrace();
//			return false;
//		}
//		
		
		//Wait until next page has loaded.

	}
	
	@Deprecated
	private boolean downloadUsingHTMLUnit() {
		return false;
//		try (final WebClient webClient = new WebClient()) {
//
//			//Mute and ignore javascript errors
//			java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(java.util.logging.Level.OFF);
//			webClient.getOptions().setJavaScriptEnabled(true);
//			webClient.getOptions().setThrowExceptionOnScriptError(false);
//			
//			
//			// Get the first page
//			final HtmlPage netflixLandingPage = webClient.getPage(INITIAL_URL);
//
//			
//			File html = new File("Hrmlpage"+Math.random()+".html");
//			netflixLandingPage.save(html);
//			
//			// Get the form that we are dealing with and within that form,
//			// find the submit button and the field that we want to change.
//			var forms = netflixLandingPage.getForms();
//
//			Optional<HtmlForm> oForm = forms.stream().filter(form -> form.getAttribute("class").equals("login-form"))
//					.findFirst();
//
//			//Alternative .. netflixLandingPage.getDocumentElement().getElementsByAttribute("form", "class", "login-form").get(0)
//			if (oForm.isPresent()) {
//
//				final HtmlForm signinForm = oForm.get();
//				
//				
//				System.out.println("Form: " );
//				System.out.println(signinForm.asXml());
//
//				//Sometimes works sometimes it doesn't?
//				final HtmlButton button = (HtmlButton) signinForm.getElementsByAttribute("button", "type", "submit").get(0);
//				final HtmlTextInput username = signinForm.getInputByName("userLoginId"); 
//				//OR !final HtmlTextInput username = signinForm.getInputByName("email"); 
//				final HtmlPasswordInput passwordField = signinForm.getInputByName("password");
//				
//				//Fill out the form
//				username.type(loginUsername);
//				passwordField.type(loginPassword);
//				
//				HtmlPage referencePage = button.click();
//				
//				
//				
//				//Do we need to wait for javascript to finish loading? 
//				
//				System.out.println(referencePage.asXml());
//				
////
////				// Change the value of the text field
////				textField.type("root");
////
////				// Now submit the form by clicking the button and get back the second page.
////				final HtmlPage page2 = button.click();
////				
////				
//				
//			} else {
//				System.err.println(
//						"Fatal: Could not find netflix signin form. Maybe the site layout changed. Please download the viewing activity file manually. Abort");
//				return false;
//			}
//
//			
//		} catch (FailingHttpStatusCodeException | IOException e) {
//			e.printStackTrace();
//			return false;
//		}
	}

}
