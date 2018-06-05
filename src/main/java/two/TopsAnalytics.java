package two;


import java.io.FileInputStream;
import java.io.IOException;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.analytics.Analytics;
import com.google.api.services.analytics.model.Account;
import com.google.api.services.analytics.model.Accounts;
import com.google.api.services.analytics.model.GaData;
import com.google.api.services.analytics.model.Profile;
import com.google.api.services.analytics.model.Profiles;
import com.google.api.services.analytics.model.Webproperties;
import com.google.api.services.analyticsreporting.v4.AnalyticsReportingScopes;


/**
 * A simple example of how to access the Google Analytics API.
 * https://developers.google.com/analytics/devguides/reporting/core/v3/quickstart/installed-java
 * 
 */
public class TopsAnalytics {
	 // Path to JSON file downloaded from the Developer's Console.
	  private static final String DATA_STORE_DIR = System.getProperty("user.home")+System.getProperty("file.separator")+
	              														"storeGA"+ System.getProperty("file.separator");
    
  private static final String CLIENT_SECRET_JSON_RESOURCE =  "project-er-72709-55e576d65df7.json";
  
 
  private static final String APPLICATION_NAME = "Analytics Two ";
  private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
  private static NetHttpTransport httpTransport;
  
  protected  static String getMetric() {
	  return "ga:sessions"; 
  }
  protected  static String getDimension() {
	  return "ga:pagePath"; 
  }
  protected  static String getEventMetric() {
	  return "ga:totalEvents"; 
  } 
  protected  static String getEventDimension() {
	  return "ga:eventCategory,ga:eventAction,ga:eventLabel"; 
	  //or one of them
  }
  public static void main(String[] args) {
    try {
      Analytics analytics = initializeAnalytics();
      String profileId = getFirstProfileId(analytics);
      
      printTotalResults(getResults(analytics, profileId,getMetric()));
      printDetailResults (doQuery(analytics, profileId,getDimension(), getMetric(), 10));

      printTotalResults(getResults(analytics, profileId,getEventMetric()));
      printDetailResults (doQuery(analytics, profileId,getEventDimension(), getEventMetric(), 10));

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static Analytics initializeAnalytics() throws Exception {

    httpTransport = GoogleNetHttpTransport.newTrustedTransport();

    // This part from The Analytics Reporting
    GoogleCredential credentialjson = GoogleCredential
            .fromStream(new FileInputStream(DATA_STORE_DIR +CLIENT_SECRET_JSON_RESOURCE))
            .createScoped( AnalyticsReportingScopes.all());


    // Construct the Analytics service object.
    return new Analytics.Builder(httpTransport, JSON_FACTORY, credentialjson)
        .setApplicationName(APPLICATION_NAME).build();
  }

  private static String getFirstProfileId(Analytics analytics) throws IOException {
	    // Get the first view (profile) ID for the authorized user.
	    String profileId = null;

	    // Query for the list of all accounts associated with the service account.
	    Accounts accounts = analytics.management().accounts().list().execute();

	    if (accounts.getItems().isEmpty()) {
	      System.err.println("No accounts found");
	    } else {
	      String firstAccountId = accounts.getItems().get(0).getId();
	      //display accounts
	      for (Account account : accounts.getItems()) {
	    	  System.out.print("Account Name: " + account.getName());
	    	  System.out.print(" Account ID: " + account.getId());
	    	  System.out.print(" Account Created: " + account.getCreated());
	    	  System.out.println(" Account Updated: " + account.getUpdated());
	    	}
	
	      // Query for the list of properties associated with the first account.
	      Webproperties properties = analytics.management().webproperties()
	          .list(firstAccountId).execute();

	      if (properties.getItems().isEmpty()) {
	        System.err.println("No properties found");
	      } else {
	        String firstWebpropertyId = properties.getItems().get(0).getId();
	        //display the properties
	        for (int i = 0;  i< properties.getItems().size() ; i++) {
	        	System.out.println("Property " +i + " Name : " + properties.getItems().get(i).getName() );
	        	System.out.print(" Property Id : " +properties.getItems().get(i).getId()); 
	        	System.out.println("Properties Username: "+properties.getUsername());
	        }
	        // Query for the list views (profiles) associated with the property.
	        Profiles profiles = analytics.management().profiles()
	            .list(firstAccountId, firstWebpropertyId).execute();

	        if (profiles.getItems().isEmpty()) {
	          System.err.println("No views (profiles) found");
	        } else {
	          // Return the first (view) profile associated with the property.
	          profileId = profiles.getItems().get(0).getId();
	          //display properties
	          for (Profile profile : profiles.getItems()) {
	         	  System.out.print("found ---> Profile Name: " + profile.getName());
	           	  System.out.println(" Profile ID: " + profile.getId());
	          }

	        }
	      }
	    }
	    return profileId;
	  }

 

  private static GaData getResults(Analytics analytics, String profileId, String metric) throws IOException {
    // Query the Core Reporting API for the number of sessions
    // in the past seven days.
    return analytics.data().ga()
        .get("ga:" + profileId, "7daysAgo", "today", metric)
        .execute();
  }

 

  protected static GaData doQuery(Analytics analytics, String profileId, String dimension, String metric,int limit) throws IOException {

      GaData gaData = analytics.data().ga()
              .get("ga:" + profileId, "7daysAgo", "today", metric)
              .setDimensions(dimension)    
              .setSort("-" + metric)
              .setMaxResults(limit)
              .execute();
      return gaData;
  }
  
  private static void printTotalResults(GaData results) {
	System.out.println("\nTotal Aggregated as no Dimension ");  
	// Parse the response from the Core Reporting API for profile name and rows (dimensions, metrics)
	if (results != null && !results.getRows().isEmpty()) {
		System.out.println("View (Profile) Name: " + results.getProfileInfo().getProfileName());
		System.out.println("Dimensions and Metrics " + results.getColumnHeaders().toString());
		System.out.println(results.getRows().toString());

    } else {
      System.out.println("No results found");
    }
  }
	private static void printDetailResults(GaData gaData) {
		System.out.println("\nTOP  with Dimension and Metric");  

	    // Parse the response from the Core Reporting API for profile name and rows(dimensions, metrics)
	    if (gaData != null &&  gaData.getRows()!= null && !gaData.getRows().isEmpty()) {
	    	System.out.println("View (Profile) Name: "  + gaData.getProfileInfo().getProfileName());
	    	System.out.println("Dimensions and Metrics " + gaData.getColumnHeaders().toString());
	      
	    	for (java.util.List<String> rowValues : gaData.getRows()) {
	    		rowValues.forEach( row-> System.out.print( row + " "));
	    		System.out.println();
	         }
	    } else {
	      System.out.println("No results found. Totals results are "+ gaData.getTotalResults());
	    }
	  }
  
 
   
}

/*


*/