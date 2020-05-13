package datasetbuilder;

import java.util.ArrayList;

import boundaries.CSVWriter;
import entities.JiraTicket;
import entities.Release;

public class Main {
	public static void main(String[] args){
		
		   String projName ="BOOKKEEPER";
		   //Name of CSV for output
		   String outnameInfo = projName + "VersionInfo.csv";
		   String outnameFiles = projName + "VersionFiles.csv";
		   RetrieveGitLog gitRetriever = new RetrieveGitLog("C:/Users/Alex/Desktop/Università/ISW2/Falessi/Progetto/Bookkeeper/bookkeeper");
		   CSVWriter csvWriter = new CSVWriter("C:/Users/Alex/Desktop/Università/ISW2/Falessi/Progetto/Deliverable2/Deliverable2/");
		   ArrayList<JiraTicket> tickets;
		   
		   ArrayList<Release> totalReleases = RetrieveJiraReleases.getReleases(projName);
		   if (totalReleases.size() < 6)
	            return;
		   		   
		   gitRetriever.setReleasesCommits(totalReleases);
		   for(int i=0; i<totalReleases.size(); i++){
			   if (!totalReleases.get(i).getCommits().isEmpty()){
				   totalReleases.get(i).setFiles(gitRetriever.getCommitFiles(gitRetriever.getLastReleaseCommit(totalReleases.get(i))));
				   totalReleases.get(i).setVersion(i+1);
			   } else {
				   //release has no commit, remove it
				   totalReleases.remove(i);
				   i--;
			   }
		   }
		   tickets = RetrieveJiraTickets.retrieveTickets(projName, totalReleases);
		   RetrieveJiraTickets.fillAndSortTickets(tickets, totalReleases, gitRetriever);
		   
		   ArrayList<Release> releases = new ArrayList<Release>(totalReleases.subList(0, totalReleases.size()/2 + (totalReleases.size()%2)));
		   
		   gitRetriever.setReleasesMetrics(releases);
		   csvWriter.printReleasesInfo(outnameInfo, releases);
		   csvWriter.printReleasesFiles(outnameFiles, releases);
		   csvWriter.printJiraTickets(tickets);
	   }
}
