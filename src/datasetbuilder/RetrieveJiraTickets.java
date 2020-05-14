package datasetbuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.JSONObject;

import entities.CommitWithChanges;
import entities.JiraTicket;
import entities.Release;
import utilities.JSONReader;

import org.json.JSONArray;
import java.util.logging.Logger;
import java.util.logging.Level;

public class RetrieveJiraTickets {
	
		private static final Logger LOGGER = Logger.getLogger(RetrieveJiraTickets.class.getName());
	
		private RetrieveJiraTickets(){
			//not called
		}
	  
	   public static List<JiraTicket> retrieveTickets(String projName, List<Release> releases) {
			   
		   Integer j = 0;
		   Integer i = 0;
		   Integer total = 1;
		   ArrayList<JiraTicket> tickets = new ArrayList<>();
		   JiraTicket ticket;
		   JSONArray versions;
		   
	      //Get JSON API for closed bugs w/ AV in the project
	      do {
	         //Only gets a max of 1000 at a time, so must do this multiple times if bugs >1000
	         j = i + 1000;
	         String url = "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22"
	                + projName + "%22AND%22issueType%22=%22Bug%22AND(%22status%22=%22closed%22OR"
	                + "%22status%22=%22resolved%22)AND%22resolution%22=%22fixed%22&fields=key,resolutiondate,versions,fixVersions,created&startAt="
	                + i.toString() + "&maxResults=" + j.toString();
	         try {
	        	 JSONObject json = JSONReader.readJsonFromUrl(url);
	        	 JSONArray issues = json.getJSONArray("issues");
	        	 total = json.getInt("total");
	        	 for (; i < total && i < j; i++) {
	        		 //Iterate through each bug
	        		 ticket = new JiraTicket(issues.getJSONObject(i%1000).get("key").toString(), LocalDateTime.parse(((JSONObject) issues.getJSONObject(i%1000).get("fields")).get("created").toString().split("\\+")[0]).toLocalDate());
	        		 versions = (JSONArray) ((JSONObject) issues.getJSONObject(i%1000).get("fields")).get("versions");
	        		 if (versions.isEmpty()){
	        			 ticket.setInjectedVersion(null);
	        		 } else {
	        			 for(Release rel : releases){
	        				 if (rel.getId().equals(((JSONObject) versions.get(0)).get("id"))){
	        					 ticket.setInjectedVersion(rel);
	        					 break;
	        				 }
	        			 }
	        		 }
	        		 tickets.add(ticket);
	        		 
	        	 }  
	         } catch (Exception e) {
	        	 LOGGER.log(Level.SEVERE, e.getMessage(), e);
	         }
	      } while (i < total);
	      return tickets;
	   }
	   
	   private static void prepareTicketsForProportion(List<JiraTicket> tickets){
		   Iterator<JiraTicket> iter = tickets.iterator();
		   while(iter.hasNext()){
			   JiraTicket ticket = iter.next();
			   if (ticket.getFixedVersion()==null || ticket.getFixedVersion().equals(ticket.getOpeningVersion())){
				   iter.remove();
			   }
		   }
		   for(int i=0; i<tickets.size(); i++){
			   if (tickets.get(i).getInjectedVersion()==null){
				   tickets.get(i).setInjectedVersion(tickets.get(i).getOpeningVersion());
				   tickets.get(i).setEstimatedIv(true);
			   }
			   else break;
		   }
	   }
	   
	   private static void proportionMovingWindow(List<JiraTicket> tickets, List<Release> releases){
		   double sum = 0;
		   int windowSize = (int) Math.ceil(((double) tickets.size())/100);
		   int counter = 0;
		   double p;
		   int predictedIV;
		   ArrayList<Double> window = new ArrayList<>();
		   if (windowSize == 0)
			   return;
		   for(int i=0; i<tickets.size(); i++){
			   if (tickets.get(i).getInjectedVersion()!=null){
				   if (!tickets.get(i).hasEstimatedIv()){
					   p = (((double) tickets.get(i).getFixedVersion().getVersion()) - tickets.get(i).getInjectedVersion().getVersion()) / (((double) tickets.get(i).getFixedVersion().getVersion()) - tickets.get(i).getOpeningVersion().getVersion());
					   if (counter==windowSize){
						   sum-=window.get(0);
						   window.remove(0);
					   } else {
						   counter++;
					   }
					   window.add(p);
					   sum+=p;
				   }
			   } else {
				   predictedIV = tickets.get(i).getFixedVersion().getVersion() - ((int) (sum/windowSize)) * (tickets.get(i).getFixedVersion().getVersion() - tickets.get(i).getOpeningVersion().getVersion());
				   if (predictedIV<1)
					   predictedIV = 1;
				   tickets.get(i).setInjectedVersion(releases.get(predictedIV-1));
				   tickets.get(i).setEstimatedIv(true);
			   }
		   }
	   }
	   
	   private static void setBugginessAndFixes(List<JiraTicket> tickets, List<Release> releases, RetrieveGitLog gitRetriever){
		   ArrayList<CommitWithChanges> commits;
		   String updatedFileName;
		   for(JiraTicket tick : tickets){
			   commits=(ArrayList<CommitWithChanges>) gitRetriever.getBuggyFilesAndFixes(tick.getId());
			   for(CommitWithChanges comm : commits){
				   for(String fileName : comm.getFilesChanged().keySet()){
					   for(Release rel : releases){
						   if (comm.getDate().compareTo(rel.getDate())<0){
							   if ((updatedFileName=gitRetriever.getMatchingName(fileName, rel))!=null){
								   rel.getFiles().get(updatedFileName).incFixNumber();
							   }
							   break;
						   }
					   }
				   		for(int i=tick.getInjectedVersion().getVersion(); i<tick.getFixedVersion().getVersion(); i++){
						   if ((updatedFileName=gitRetriever.getMatchingName(fileName, releases.get(i-1)))!=null && releases.get(i-1).getFiles().containsKey(updatedFileName)){
								   releases.get(i-1).getFiles().get(updatedFileName).incBugs();
						   }
				   		}
				   }
			   }
		   }
	   }
	   
	   public static void fillAndSortTickets(List<JiraTicket> tickets, List<Release> releases, RetrieveGitLog gitRetriever){
		   LocalDate lastDate;
		   Iterator<JiraTicket> iter = tickets.iterator();
		   while(iter.hasNext()){
			   JiraTicket ticket = iter.next();
			   lastDate = gitRetriever.getTicketLastDate(ticket.getId());
			   if (lastDate==null){
				   iter.remove();
			   } else {
				   for(Release rel : releases){
					   if (lastDate.compareTo(rel.getDate())<0){
						   ticket.setFixedVersion(rel);
						   break;
					   }
				   }
				   for(Release rel : releases){
					   if (ticket.getCreationDate().compareTo(rel.getDate())<0){
						   ticket.setOpeningVersion(rel);
						   if (ticket.getInjectedVersion()!=null && ticket.getInjectedVersion().getDate().compareTo(ticket.getOpeningVersion().getDate())>0){
								   ticket.setInjectedVersion(ticket.getOpeningVersion());
						   }
						   break;
					   }
				   }
			   }
		   }
		   prepareTicketsForProportion(tickets);
		   tickets.sort((t1, t2) -> t1.getFixedVersion().getDate().compareTo(t2.getFixedVersion().getDate()));
		   proportionMovingWindow(tickets, releases);
		   setBugginessAndFixes(tickets, releases, gitRetriever);
	   }
	
}
