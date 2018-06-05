package two;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.analyticsreporting.v4.AnalyticsReporting;
import com.google.api.services.analyticsreporting.v4.AnalyticsReportingScopes;
import com.google.api.services.analyticsreporting.v4.model.ColumnHeader;
import com.google.api.services.analyticsreporting.v4.model.DateRange;
import com.google.api.services.analyticsreporting.v4.model.DateRangeValues;
import com.google.api.services.analyticsreporting.v4.model.Dimension;
import com.google.api.services.analyticsreporting.v4.model.GetReportsRequest;
import com.google.api.services.analyticsreporting.v4.model.GetReportsResponse;
import com.google.api.services.analyticsreporting.v4.model.Metric;
import com.google.api.services.analyticsreporting.v4.model.MetricHeaderEntry;
import com.google.api.services.analyticsreporting.v4.model.OrderBy;
import com.google.api.services.analyticsreporting.v4.model.Report;
import com.google.api.services.analyticsreporting.v4.model.ReportRequest;
import com.google.api.services.analyticsreporting.v4.model.ReportRow;

/*
 * 
 * 
 *    1)https://developers.google.com/analytics/devguides/reporting/core/v4/quickstart/service-java
 *    Background information
 *    
 *    2)How to Create  service account when you are logged in with the gmail account that has the google analytics account
 *    GA account:
 *    
 *    https://console.developers.google.com/apis/api/analyticsreporting.googleapis.com/
 *    
 *    -project=Project    
 *    -service account 
 *  
 */


public class TopsAnalyticsReporting {
    private static final String APPLICATION_NAME = "Analytics Reporting PRESA PRE"; 
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
//    private static final String KEY_FILE_LOCATION = "<REPLACE_WITH_JSON_FILE>";
//    private static final String VIEW_ID = "<REPLACE_WITH_VIEW_ID>";

	
	// Path to JSON file downloaded from the Developer's Console.
    private static final String DATA_STORE_DIR = System.getProperty("user.home")+System.getProperty("file.separator")+
		                                                         "storeGA"+ System.getProperty("file.separator");

    private static final String CLIENT_SECRET_JSON_RESOURCE =  "project-er-72709-55e576d65df7.json";
     
    private static final String VIEW_ID_PRISA_PRE = "175644070" ; // View Name"PRISA PRE" with ga eliRigol@gmail.com URL:base-saas-pre.agilecontent.com    
    private static final String VIEW_ID = VIEW_ID_PRISA_PRE; 

    //Maximum number of TOPS
    private static final int TOP_N= 4;
    //Filters https://developers.google.com/analytics/devguides/reporting/core/v3/reference#filters).
    //eventAction stores COMMENT_video 
    //					 VIEW_video 
    //					 SHARE_video
    private static final String TOP_COMMENTED = "ga:eventAction=~^COMMENT"; 
    private static final String TOP_VIEWED =    "ga:eventAction=~^VIEW";
    private static final String TOP_SHARED_VIDEO ="ga:eventAction=~^SHARE_video$";
    private static final String TOP_SHARED_AUDIO ="ga:eventAction=~^SHARE_audio$";

    //ga:eventLabel stores ESI: P_AGILE/ASSET/CA/100000000001230
    // 							P_AGILE/AUTHOR/CA/100000000001232
    // 							P_AGILE/NEWS/CA/100000000001232

    private static final String TOP_ESI_TPOLL ="ga:eventLabel=~.*(/T_POLL/).*";   
    private static final String TOP_ESI_ASSETGROUP ="ga:eventLabel=~.*(/ASSETGROUP/).*";
    private static final String TOP_ESI_NEWS = "ga:eventLabel=~.*(/NEWS/).*";
    private static final String TOP_ESI_NEWS_CA= "ga:eventLabel=~.*(/NEWS/CA/).*";
    
    //ga:eventCategory
    private static final String TOP_CATEGORY_ACT = "ga:eventCategory=~^actualidad$";

    //Combining filter: , OR      ;AND
    private static final String TOP_COMMENTED_ESI_ASSETGROUP = TOP_COMMENTED + ";" +TOP_ESI_ASSETGROUP;
    private static final String TOP_COMMENTED_ESI_NEWS = TOP_COMMENTED +";" +TOP_ESI_NEWS;
    private static final String TOP_COMMENTED_ESI_NEWS_CA = TOP_COMMENTED +";" +TOP_ESI_NEWS;    
    private static final String TOP_SHARED_CATEGORY_ACT = TOP_COMMENTED + ";" +TOP_CATEGORY_ACT;
     
    //utility functions
    static List<Dimension> getDimensionList(){
    	// Create Dimension objects  action, category   / action, label / action, caegory, label
        Dimension eventAction = new Dimension().setName("ga:eventAction");
        Dimension eventCategory = new Dimension().setName("ga:eventCategory");    
        Dimension eventLabel = new Dimension().setName("ga:eventLabel"); 
        return Arrays.asList(eventAction,eventCategory,eventLabel);   	 
   }
  
    //utility functions
    static List<Dimension> getDimensionList_Action(){
    	// Create Dimension objects  action, category   / action, label / action, caegory, label
        Dimension eventAction = new Dimension().setName("ga:eventAction");
   
        return Arrays.asList(eventAction);   	
       
   }
    
    //utility functions
    static List<Dimension> getDimensionList_ActionCategory(){
    	// Create Dimension objects  action, category   / action, label / action, caegory, label
        Dimension eventAction = new Dimension().setName("ga:eventAction");
        Dimension eventCategory = new Dimension().setName("ga:eventCategory");    
  
        return Arrays.asList(eventAction,eventCategory);   	
      
   }
    
    //utility functions
    static List<Dimension> getDimensionList_ActionLabel(){
    	// Create Dimension objects  action, category   / action, label / action, caegory, label
        Dimension eventAction = new Dimension().setName("ga:eventAction");
        Dimension eventLabel = new Dimension().setName("ga:eventLabel"); //ESI
   
        return Arrays.asList(eventAction, eventLabel);   	
     }
   static List<Metric> getMetricList(){
		Metric totalEvents = new Metric().setExpression("ga:totalEvents");
		Metric uniqueEvents = new Metric().setExpression("ga:uniqueEvents");

       //return a list with them
       return Arrays.asList(totalEvents,uniqueEvents);
    }
   
    public static void main(String[] args) {
        try {
            AnalyticsReporting service = initializeAnalyticsReporting();

            System.out.println("\nTop Views");
            GetReportsResponse response1 = getReportViews(service,TOP_N);
            printResponse(response1);
 
            System.out.println("\nTop Views with events "+TOP_VIEWED );
            printResponse (getReportEvents(service,TOP_VIEWED,   getDimensionList_ActionLabel(),getMetricList(),TOP_N ) );
 
            System.out.println("\nTop  " + TOP_COMMENTED);         
            printResponse (getReportEvents(service,TOP_COMMENTED,getDimensionList_ActionLabel(),getMetricList() ,TOP_N) );
 
   
            System.out.println("\nTop   " + TOP_SHARED_VIDEO );         
            printResponse (getReportEvents(service,TOP_SHARED_VIDEO,   getDimensionList_ActionLabel(),getMetricList() ,TOP_N) );
           
            System.out.println("\nTop   " + TOP_SHARED_AUDIO );         
            printResponse (getReportEvents(service,TOP_SHARED_AUDIO,   getDimensionList_ActionLabel(),getMetricList() ,TOP_N) );
 
            System.out.println("\nTop  " + TOP_ESI_ASSETGROUP);         
            printResponse (getReportEvents(service,TOP_ESI_ASSETGROUP,getDimensionList_ActionLabel(),getMetricList(),TOP_N ) );
 

            System.out.println("\nTop  " + TOP_ESI_NEWS_CA);         
            printResponse (getReportEvents(service,TOP_ESI_NEWS_CA,getDimensionList_ActionLabel(),getMetricList(),TOP_N ) );
 
            System.out.println("\nTop  " + TOP_SHARED_CATEGORY_ACT );         
            printResponse (getReportEvents(service,TOP_SHARED_CATEGORY_ACT,   getDimensionList(),getMetricList(),TOP_N ) );
            

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Initializes an Analytics Reporting API V4 service object.
     *
     * @return An authorized Analytics Reporting API V4 service object.
     * @throws IOException
     * @throws GeneralSecurityException
     */
    private static AnalyticsReporting initializeAnalyticsReporting() throws GeneralSecurityException, IOException {

        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
 
        // This one tries to read a JSON file
        GoogleCredential credentialjson = GoogleCredential
                .fromStream(new FileInputStream(DATA_STORE_DIR +CLIENT_SECRET_JSON_RESOURCE))
                .createScoped( AnalyticsReportingScopes.all());
        
        System.out.println("initializeAnalyticsReporting based on Json resource " +  DATA_STORE_DIR +CLIENT_SECRET_JSON_RESOURCE);
        System.out.println("Data for VIEW_ID " + VIEW_ID);
       
        // Construct the Analytics Reporting service object.
        return new AnalyticsReporting.Builder(httpTransport, JSON_FACTORY, credentialjson)
                .setApplicationName(APPLICATION_NAME).build();
    }

    //----------------------------------------------------------------------------------------------------------------~Try why not finding file
  //----------------------------------------------------------------------------------------------------------------
    /**
     * Queries the Analytics Reporting API V4.
     *
     * @param service An authorized Analytics Reporting API V4 service object.
     * @return GetReportResponse The Analytics Reporting API V4 response.
     * @throws IOException
     */
    private static GetReportsResponse getReportViews(AnalyticsReporting service, int pageSize) throws IOException {
        // Create the DateRange object.
        DateRange dateRange7 = new DateRange();
        dateRange7.setStartDate("2DaysAgo");
        dateRange7.setEndDate("today");

        // Create the Metrics object.
        Metric sessions = new Metric()
                .setExpression("ga:sessions");
        Metric users = new Metric()
                .setExpression("ga:users");
  
        //Dimension pageTitle = new Dimension().setName("ga:pageTitle");
        Dimension pagePath = new Dimension().setName("ga:pagePath");
        //Dimension country = new Dimension().setName("ga:country"); //ga:country,ga:region,ga:regionId

        //default is ASCENDING and can only use 1 fieldName
		List<OrderBy> metricOrderBys = new ArrayList<OrderBy>();
		OrderBy orderBy = new OrderBy().setSortOrder("DESCENDING").setFieldName("ga:sessions");
		metricOrderBys.add( orderBy);
				
		
        // Create the ReportRequest object.
        ReportRequest request = new ReportRequest()
                .setViewId(VIEW_ID)
                .setDateRanges(Arrays.asList(dateRange7))
                .setMetrics(Arrays.asList(sessions,users))
       	    	.setDimensions(Arrays.asList(pagePath))
        		.setOrderBys(metricOrderBys)
                .setPageSize(pageSize);
         
        ArrayList<ReportRequest> requests = new ArrayList<ReportRequest>();
        requests.add(request);
        System.out.println ("Report request :" +request.toPrettyString());
        
        // Create the GetReportsRequest object.
        GetReportsRequest getReport = new GetReportsRequest()
                .setReportRequests(requests);

        // Call the batchGet method.
        GetReportsResponse response = service.reports().batchGet(getReport).execute();
        
        // Return the response.
        return response;
    }

    /**
     * Parses and prints the Analytics Reporting API V4 response.
     *
     * @param response An Analytics Reporting API V4 response.
     */
    private static void printResponse(GetReportsResponse response) {

        for (Report report: response.getReports()) {
            ColumnHeader header = report.getColumnHeader();
            List<String> dimensionHeaders = header.getDimensions();
            List<MetricHeaderEntry> metricHeaders = header.getMetricHeader().getMetricHeaderEntries();
            List<ReportRow> rows = report.getData().getRows();
 
            if (rows == null) {
                System.out.println("No data found for " + VIEW_ID);
                return;
            }

             
            for (ReportRow row: rows) {
                List<String> dimensions = row.getDimensions();
                List<DateRangeValues> metrics = row.getMetrics();

                for (int i = 0; i < dimensionHeaders.size() && i < dimensions.size(); i++) {
                    System.out.print(" Dimension =" + dimensionHeaders.get(i) + ": " + dimensions.get(i));
                }
                
                for (int j = 0; j < metrics.size(); j++) {
                    //System.out.print("Date Range (" + j + "): ");
                    DateRangeValues values = metrics.get(j);
                    for (int k = 0; k < values.getValues().size() && k < metricHeaders.size(); k++) {
                        System.out.print("--> Metric:" +metricHeaders.get(k).getName() + ": " + values.getValues().get(k));
                    }
                    System.out.println();
                }
 
            }
        }
    }
    
    /**
     * Queries the Analytics Reporting API V4.
     *
     * @param service An authorized Analytics Reporting API V4 service object.
     * @return GetReportResponse The Analytics Reporting API V4 response.
     * @throws IOException
     * 
     * See https://developers.google.com/analytics/devguides/reporting/core  with API Names
 Dimensions: ga:eventCategory, ga:eventAction, ga:eventLabel
 
 Metrics ga:totalEvents,ga:uniqueEvents,
 	ga:eventValue
	ga:avgEventValue
	ga:sessionsWithEvent
	ga:eventsPerSessionWithEvent

     */
    private static GetReportsResponse getReportEvents(AnalyticsReporting service, String filter, 
    		List<Dimension> dimensions, List<Metric> metrics, int pageSize) throws IOException {
        // Create the DateRange object.
        DateRange dateRange7 = new DateRange();
        dateRange7.setStartDate("2DaysAgo");
        dateRange7.setEndDate("today");

        //default is ASCENDING and can only use 1 fieldName
 		List<OrderBy> metricOrderBys = new ArrayList<OrderBy>();
 		OrderBy orderBy = new OrderBy().setSortOrder("DESCENDING").setFieldName("ga:totalEvents");
 		metricOrderBys.add( orderBy);

        // Create the ReportRequest object.
        ReportRequest request = new ReportRequest()
                .setViewId(VIEW_ID)
                .setDateRanges(Arrays.asList(dateRange7))
                .setMetrics( metrics)
                .setDimensions(dimensions)
                .setFiltersExpression(filter)
                .setOrderBys(metricOrderBys)
                .setPageSize(pageSize);
 
        System.out.println ("Report request :" +request.toPrettyString());
        ArrayList<ReportRequest> requests = new ArrayList<ReportRequest>();
        requests.add(request);
  
        // Create the GetReportsRequest object.
        GetReportsRequest getReport = new GetReportsRequest()
                .setReportRequests(requests);

        // Call the batchGet method.
        GetReportsResponse response = service.reports().batchGet(getReport).execute();

        // Return the response.
        return response;
    }

  
}
