package datasetbuilder;

import java.util.ArrayList;
import java.util.Iterator;

import entities.JiraTicket;
import entities.Release;
import utilities.CSVWriter;

public class Main {
	public static void main(String[] args){
		
		   String projName ="BOOKKEEPER";
		   //Name of CSV for output
		   String outnameInfo = projName + "VersionInfo.csv";
		   String outnameFiles = projName + "VersionFiles.csv";
		   RetrieveGitLog gitRetriever = new RetrieveGitLog("C:/Users/Alex/Desktop/Università/ISW2/Falessi/Progetto/Bookkeeper/bookkeeper");
		   CSVWriter csvWriter = new CSVWriter("C:/Users/Alex/Desktop/Università/ISW2/Falessi/Progetto/progetto/Deliverable2/");
		   ArrayList<JiraTicket> tickets;
		   ArrayList<Release> totalReleases = (ArrayList<Release>) RetrieveJiraReleases.getReleases(projName);
		   int ver = 0;
		   Iterator<Release> iter;
		   
		   if (totalReleases.size() < 6)
	            return;
		   		   
		   gitRetriever.setReleasesCommits(totalReleases);
		   iter = totalReleases.iterator();
		   while(iter.hasNext()){
			   Release release = iter.next();
			   if (!release.getCommits().isEmpty()){
				   ver++;
				   release.setFiles(gitRetriever.getCommitFiles(gitRetriever.getLastReleaseCommit(release)));
				   release.setVersion(ver);
			   } else {
				   //release has no commit, remove it
				   iter.remove();
			   }
		   }
		   tickets = (ArrayList<JiraTicket>) RetrieveJiraTickets.retrieveTickets(projName, totalReleases);
		   RetrieveJiraTickets.fillAndSortTickets(tickets, totalReleases, gitRetriever);
		   
		   ArrayList<Release> releases = new ArrayList<>(totalReleases.subList(0, totalReleases.size()/2 + (totalReleases.size()%2)));
		   
		   gitRetriever.setReleasesMetrics(releases);
		   csvWriter.printReleasesInfo(outnameInfo, releases);
		   csvWriter.printReleasesFiles(outnameFiles, releases);
		   csvWriter.printJiraTickets(tickets);
	   }
}
