package datasetbuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.logging.Logger;

import entities.CommitWithChanges;
import entities.Release;
import entities.ReleaseFile;

import java.util.ArrayList;
import java.util.TreeMap;
import java.util.logging.Level;

public class RetrieveGitLog {
	
	private static final Logger LOGGER = Logger.getLogger(RetrieveGitLog.class.getName());
	private File filePath;
	private TreeMap<String, String> renameTable;
	private TreeMap<String, String> reverseRenameTable;
	private TreeMap<String, LocalDate> fileCreationTable;
	
	public RetrieveGitLog(String path){
		this.filePath = new File(path);
		this.renameTable = new TreeMap<String, String>();
		this.reverseRenameTable = new TreeMap<String, String>();
		this.fileCreationTable = new TreeMap<String, LocalDate>();
	}

	public LocalDate getTicketLastDate(String TicketId){
		ProcessBuilder pb = new ProcessBuilder("git", "log", "--date=short", "--pretty=format:\"%cd\"", "--max-count=1", "--grep=" + TicketId + "[^0-9]");
		pb.directory(filePath);
		String line=null;
		try {
			Process process = pb.start();
			BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
			line = br.readLine();
		} catch(IOException e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
		if (line==null)
			return null;
		return LocalDate.parse(line);
	}
	
	public ArrayList<LocalDate> getTicketDates(String TicketId){
		ArrayList<LocalDate> dates = new ArrayList<LocalDate>();
		ProcessBuilder pb = new ProcessBuilder("git", "log", "--date=short", "--pretty=format:\"%cd\"", "--grep=" + TicketId + "[^0-9]");
		pb.directory(filePath);
		String line=null;
		try {
			Process process = pb.start();
			BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
			while ((line = br.readLine()) != null) {
				dates.add(LocalDate.parse(line));
			}
		} catch(IOException e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
		return dates;
	}

	public String getLastTicketCommit(String ticketId){
		ProcessBuilder pb = new ProcessBuilder("git", "log", "--format=format:\"%H\"", "--max-count=1", "--grep=" + ticketId + "[^0-9]");
		pb.directory(filePath);
		String line=null;
		try {
			Process process = pb.start();
			BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
			line = br.readLine();
		} catch(IOException e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
		return line;
	}
	
	public ArrayList<CommitWithChanges> getBuggyFilesAndFixes(String TicketId){
		ArrayList<CommitWithChanges> commits = new ArrayList<CommitWithChanges>();
		CommitWithChanges comm = null;
		ProcessBuilder pb = new ProcessBuilder("git", "log", "--name-only", "--oneline", "--date=short", "--pretty=\"Date:%cd\"", "--grep=" + TicketId + "[^0-9]");
		pb.directory(filePath);
		String line=null;
		try {
			Process process = pb.start();
			BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
			while ((line = br.readLine())!=null){
				if (line.startsWith("Date:")){
					//save old commit data
					if (comm!=null)
						commits.add(comm);
					comm = new CommitWithChanges(LocalDate.parse(line.split(":")[1]));
				} else if (line.endsWith(".java")){
					if (comm!=null)
						comm.addChangedFile(line, 0, 0);
				}
			}
			//save last commit data
			if (comm!=null)
				commits.add(comm);
		} catch(IOException e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
		return commits;
	}
	
	private void getCommitRenamesAndCreations(String commitHash){
		LocalDate date;
		ProcessBuilder pb = new ProcessBuilder("git", "show", "--name-status", "--diff-filter=RA", "--pretty=\"%cd\"", "--date=short", commitHash);
		pb.directory(filePath);
		String line=null;
		try {
			Process process = pb.start();
			BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
			if ((line = br.readLine())!=null){
				date = LocalDate.parse(line);
				while ((line = br.readLine())!=null){
					if (line.endsWith(".java")){
						if (line.split("\t")[0].equals("R100")){
							renameTable.put(line.split("\t")[1], line.split("\t")[2]);
							reverseRenameTable.put(line.split("\t")[2], line.split("\t")[1]);
						} else if (line.split("\t")[0].equals("A")){
							fileCreationTable.put(line.split("\t")[1], date);
						}
					}
				}
			}
		} catch(IOException e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
	}
	
	public TreeMap<String, ReleaseFile> getCommitFiles(String commitHash){
		TreeMap<String, ReleaseFile> files = new TreeMap<String, ReleaseFile>();
		ReleaseFile relFile;
		ProcessBuilder pb = new ProcessBuilder("git", "ls-tree", "-r", commitHash);
		pb.directory(filePath);
		String line=null;
		try {
			Process process = pb.start();
			BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
			while ((line = br.readLine())!=null){
				if (line.endsWith(".java")){
					line=line.split(" ")[2];
					
					//file hash, file name
					relFile = new ReleaseFile(line.split("\t")[0], line.split("\t")[1]);
					files.put(relFile.getName(), relFile);
				}
			}
		} catch(IOException e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
		return files;
	}
	
	public CommitWithChanges getCommitChanges(String commitHash){
		CommitWithChanges comm = null;
		ProcessBuilder pb = new ProcessBuilder("git", "show", "--date=short", "--numstat", "--oneline", "--pretty=\"%cd\"", commitHash);
		pb.directory(filePath);
		String line=null;
		try {
			Process process = pb.start();
			BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
			if ((line = br.readLine())!=null){
				comm = new CommitWithChanges(LocalDate.parse(line));
				while ((line = br.readLine())!=null){
					if (line.endsWith(".java")){
						comm.addChangedFile(line.split("\t")[2], Integer.parseInt(line.split("\t")[0]), Integer.parseInt(line.split("\t")[1]));
					}
				}
			}
		} catch(IOException e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
		return comm;
	}
	
	public String getLastReleaseCommit(Release release){
		ProcessBuilder pb = new ProcessBuilder("git", "log", "--first-parent", "master", "--before=" + release.getDate().toString(), "--format=format:\"%H\"", "--max-count=1");
		pb.directory(filePath);
		String line=null;
		try {
			Process process = pb.start();
			BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
			line = br.readLine();
		} catch(IOException e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
		return line;
	}
	
	public String getReleaseCommits(Release release){
		ProcessBuilder pb = new ProcessBuilder("git", "log", "--first-parent", "master", "--before=" + release.getDate().toString(), "--format=format:\"%H\"", "--max-count=1");
		pb.directory(filePath);
		String line=null;
		try {
			Process process = pb.start();
			BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
			line = br.readLine();
		} catch(IOException e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
		return line;
	}
	
	//finds commits of the Releases (follows only master branch)
	public void setReleasesCommits(ArrayList<Release> releases){
		TreeMap<String, String> commits;
		ProcessBuilder pb;
		for(int i=0; i<releases.size(); i++){
			commits = new TreeMap<String, String>();
			if (i==0){
				pb = new ProcessBuilder("git", "log", "--first-parent", "master", "--before=" + releases.get(i).getDate(), "--format=format:\"%H", "%an\"");
			} else {
				pb = new ProcessBuilder("git", "log", "--first-parent", "master", "--before=" + releases.get(i).getDate(), "--after=" + releases.get(i-1).getDate(), "--format=format:\"%H", "%an\"");
			}
			pb.directory(filePath);
			String line=null;
			try {
				Process process = pb.start();
				BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
				while ((line = br.readLine())!=null){
					commits.put(line.split(" ", 2)[0], line.split(" ", 2)[1]);
				}
			} catch(IOException e) {
				LOGGER.log(Level.SEVERE, e.getMessage(), e);
			}
			releases.get(i).setCommits(commits);
		}
	}
	
	private LocalDate getFileCreationDate(String fileName){
		ProcessBuilder pb = new ProcessBuilder("git", "log", "--follow", "--format=format:\"%cd\"", "--date=short", "--diff-filter=A", "--", fileName);
		pb.directory(filePath);
		String line=null;
		try {
			Process process = pb.start();
			BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
			line = br.readLine();
		} catch(IOException e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
		return LocalDate.parse(line);
	}
	
	//retrieve file name in the specific release
	public String getMatchingName(String name, Release release){
		String matchingName=name;
		if (release.getFiles().containsKey(matchingName))
			return matchingName;
		while(renameTable.containsKey(matchingName)){
			matchingName=renameTable.get(matchingName);
			if (release.getFiles().containsKey(matchingName))
				return matchingName;
		}
		matchingName=name;
		while(reverseRenameTable.containsKey(matchingName)){
			matchingName=reverseRenameTable.get(matchingName);
			if (release.getFiles().containsKey(matchingName))
				return matchingName;
		}
		return null;
	}
	
	//sum the previous Release values
	private void recordIncrementalValues(ArrayList<Release> releases){
		String oldFileName;
		for(int i=1; i<releases.size(); i++){
			for(String fileName : releases.get(i).getFiles().keySet()){
				if ((oldFileName=getMatchingName(fileName, releases.get(i-1))) != null){
					releases.get(i).getFiles().get(fileName).sumToLoc(releases.get(i-1).getFiles().get(oldFileName).getLoc());
					for (String auth : releases.get(i-1).getFiles().get(oldFileName).getAuthors()){
						releases.get(i).getFiles().get(fileName).addAuthor(auth);
					}
				}
			}
		}
	}
	
	public void setReleasesMetrics(ArrayList<Release> releases){
		CommitWithChanges comm;
		String updatedName;
		int commitAge;
		//build up rename tables
		for(Release release : releases){
			for(String commit : release.getCommits().keySet()){
				getCommitRenamesAndCreations(commit);
			}
		}
		for(Release release : releases){
			for(String fileName : release.getFiles().keySet()){
				for (Release r : releases){
					updatedName=getMatchingName(fileName, r);
					if (updatedName!=null){
						if (fileCreationTable.containsKey((updatedName))){
							release.getFiles().get(fileName).setCreationDate(fileCreationTable.get(updatedName));
							break;
						}
					}
				}
				if (release.getFiles().get(fileName).getCreationDate()==null){
					release.getFiles().get(fileName).setCreationDate(getFileCreationDate(fileName));
				}
			}
		}
		for(Release release : releases){
			for(String commit : release.getCommits().keySet()){
				if ((comm = getCommitChanges(commit)) != null){
					for(String name : comm.getFilesChanged().keySet()){
						updatedName=getMatchingName(name, release);
						if (updatedName!=null){
							if (release.getFiles().containsKey(updatedName)){
								commitAge = (int) ChronoUnit.WEEKS.between(comm.getDate(), release.getDate());
								release.getFiles().get(updatedName).doRevision(comm.getFilesChanged().get(name)[0], comm.getFilesChanged().get(name)[1], comm.getFilesChanged().keySet().size(), commitAge);
								release.getFiles().get(updatedName).addAuthor(release.getCommits().get(commit));
							}
						}
					}
				}
			}
		}
		recordIncrementalValues(releases);
	}
	
}
