package datasetbuilder;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;
import java.time.LocalDate;
import org.json.JSONObject;
import entities.Release;
import utilities.JSONReader;

import org.json.JSONArray;

public class RetrieveJiraReleases {
	
	private static final Logger LOGGER = Logger.getLogger(RetrieveJiraReleases.class.getName());
	
	private RetrieveJiraReleases(){
		//not called
	}
	
	private static ArrayList<Release> filterSameDates(List<Release> releases){
		ArrayList<Release> rel = new ArrayList<>();
		for(int i=1; i<releases.size(); i++){
			if (releases.get(i).getDate().compareTo(releases.get(i-1).getDate())!=0){
				rel.add(releases.get(i-1));
			}
		}
		if (!releases.isEmpty())
			rel.add(releases.get(releases.size()-1));
		return rel;
	}

	public static List<Release> getReleases(String projName) {
		   
		//Fills the arraylist with releases dates and orders them
		//Ignores releases with missing dates
		ArrayList<Release> releases = new ArrayList<>();
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
			releases.sort((r1, r2) -> r1.getDate().compareTo(r2.getDate()));
			releases = filterSameDates(releases);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
	   	}
	    return releases;    
	}
	
}
