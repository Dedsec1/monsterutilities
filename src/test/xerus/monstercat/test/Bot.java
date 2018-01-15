package xerus.monstercat.test;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;

import javax.swing.SwingUtilities;
import java.io.*;
import java.util.Arrays;
import java.util.List;

public class Bot {
	
    private static final String APPLICATION_NAME = "MCatalog Bot";

    /** Directory to store user credentials for this application */
    private static final java.io.File DATA_STORE_DIR = new File(
        System.getProperty("user.home"), ".credentials/sheets.googleapis.com-mcatalog");

    private static FileDataStoreFactory DATA_STORE_FACTORY;
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static HttpTransport HTTP_TRANSPORT;

    /** Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved credentials */
    private static final List<String> SCOPES = Arrays.asList(SheetsScopes.SPREADSHEETS_READONLY);

    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }
	
	// == REQUEST ==

    // improve create MCatalog sheet editor
	/*final static String sheetid = "";
	protected void buttonCall() throws Exception {
        String range = "Catalog!D2:F";
        ValueRange response = service.spreadsheets().values()
            .get(sheetid, range)
            .setKey("AIzaSyDZSojQaxR2VMx3U7hEgKDAUQqHy8OiihQ")
            .execute();
        List<List<Object>> values = response.getValues();
        if (values == null || values.size() == 0) {
            System.out.println("No data found.");
        } else {
        	for (List row : values) {
        		if(row.size()>0)
        			if(row.size()<3) {       				
        				output.addRow(row.get(0), row.get(1));
        			} else
        				output.addRow(row.get(0), row.get(1), row.get(2));
        	}
        }
	}*/

	// == STATIC PREPARATION ==
	
	static Sheets service;
    public static void main(String[] args) throws IOException {
        // Build a new authorized API client service.
        service = getSheetsService();
        // Opens the Application
		SwingUtilities.invokeLater(() -> new Bot());
    }

    /** Build and return an authorized Sheets API client service.
     * @return an authorized Sheets API client service
     * @throws IOException
     */
    public static Sheets getSheetsService() throws IOException {
    	GoogleCredential credential = new GoogleCredential();
        credential.createScoped(SCOPES);
        //authorize();
        return new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    /** Creates an authorized Credential object. */
    public static Credential authorize() throws IOException {
        // Load client secrets.
        InputStream in = Bot.class.getResourceAsStream("/client_secret.json");
        GoogleClientSecrets clientSecrets =
            GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow =
                new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(DATA_STORE_FACTORY)
                .setAccessType("offline")
                .build();
        Credential credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
        System.out.println("Credentials saved to " + DATA_STORE_DIR.getAbsolutePath());
        return credential;
    }
	
}
