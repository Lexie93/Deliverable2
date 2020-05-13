package datasetbuilder;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Collections;
import java.time.LocalDate;
import org.json.JSONObject;
import entities.Release;
import org.json.JSONArray;
import boundaries.JSONReader;

public class RetrieveJiraReleases {
	
	private static final Logger LOGGER = Logger.getLogger(RetrieveJiraTickets.class.getName());
	
	private static void filterSameDates(ArrayList<Release> releases){
		for(int i=1; i<releases.size(); i++){
			if (releases.get(i).compareTo(releases.get(i-1))==0){
				releases.remove(i-1);
				i--;
			}
		}
	}

	public static ArrayList<Release> getReleases(String projName) {
		   
		//Fills the arraylist with releases dates and orders them
		//Ignores releases with missing dates
		ArrayList<Release> releases = new ArrayList<Release>();
		String url = "https://issues.apache.org/jira/rest/api/2/project/" + projName;
		try {
			JSONObject json = JSONReader.readJsonFromUrl(url);
			JSONArray versions = json.getJSONArray("versions");
			for (int i = 0; i < versions.length(); i++ ) {
				String name = "";
				String id = "";
		        if(versions.getJSONObject(i).has("releaseDate")) {
		        	if (versions.getJSONObject(i).has("name"))
		                  name = versions.getJSONObject(i).get("name").toString();
		            if (versions.getJSONObject(i).has("id"))
		                  id = versions.getJSONObject(i).get("id").toString();
		            releases.add(new Release(id, name, LocalDate.parse(versions.getJSONObject(i).get("releaseDate").toString())));
		        }
			}
			// order releases by date
			Collections.sort(releases);
			filterSameDates(releases);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
	   	}
	    return releases;    
	}
	
}
