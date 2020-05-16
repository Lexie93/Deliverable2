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
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;

public class RetrieveGitLog {
	
	private static final Logger LOGGER = Logger.getLogger(RetrieveGitLog.class.getName());
	private static final String GREP = "--grep=";
	private static final String SHORTDATE = "--date=short";
	private static final String MAXCOUNT_1 = "--max-count=1";
	private static final String FORMAT = "--pretty=format:";
	private static final String DATE = "\"%cd\"";
	private static final String HASH = "\"%H\"";
	private static final String NO_NUMBER = "[^0-9]";
	private static final String FIRST_PARENT = "--first-parent";
	private static final String BEFORE = "--before=";
	private static final String AFTER = "--after=";
	private static final String MASTER = "master";
	private static final String ONELINE = "--oneline";
	private static final String JAVAFILE = ".java";
	
	private File filePath;
	private TreeMap<String, String> renameTable;
	private TreeMap<String, String> reverseRenameTable;
	private TreeMap<String, LocalDate> fileCreationTable;
	
	public RetrieveGitLog(String path){
		this.filePath = new File(path);
		this.renameTable = new TreeMap<>();
		this.reverseRenameTable = new TreeMap<>();
		this.fileCreationTable = new TreeMap<>();
	}
	
	private LocalDate getDate(ProcessBuilder pb){
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
	
	public LocalDate getTicketLastDate(String ticketId){
		ProcessBuilder pb = new ProcessBuilder("git", "log", SHORTDATE, FORMAT + DATE, MAXCOUNT_1, GREP + ticketId + NO_NUMBER, GREP + ticketId + "$");
		return getDate(pb);
	}
	
	private LocalDate getFileCreationDate(String fileName){
		ProcessBuilder pb = new ProcessBuilder("git", "log", "--follow", FORMAT + DATE, SHORTDATE, "--diff-filter=A", "--", fileName);
		return getDate(pb);
	}
	
	public List<LocalDate> getTicketDates(String ticketId){
		ArrayList<LocalDate> dates = new ArrayList<>();
		ProcessBuilder pb = new ProcessBuilder("git", "log", SHORTDATE, FORMAT + DATE, GREP + ticketId + NO_NUMBER, GREP + ticketId + "$");
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
		ProcessBuilder pb = new ProcessBuilder("git", "log", FORMAT + HASH, MAXCOUNT_1, GREP + ticketId + NO_NUMBER, GREP + ticketId + "$");
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
	
	public List<CommitWithChanges> getBuggyFilesAndFixes(String ticketId){
		ArrayList<CommitWithChanges> commits = new ArrayList<>();
		CommitWithChanges comm = null;
		ProcessBuilder pb = new ProcessBuilder("git", "log", "--name-only", ONELINE, SHORTDATE, FORMAT + "\"Date:%cd\"", GREP + ticketId + NO_NUMBER, GREP + ticketId + "$");
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
				} else if (line.endsWith(JAVAFILE) && comm!=null){
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
		ProcessBuilder pb = new ProcessBuilder("git", "show", "--name-status", "--diff-filter=RA", FORMAT + DATE, SHORTDATE, commitHash);
		pb.directory(filePath);
		String line=null;
		try {
			Process process = pb.start();
			BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
			if ((line = br.readLine())!=null){
				date = LocalDate.parse(line);
				while ((line = br.readLine())!=null){
					if (line.endsWith(JAVAFILE)){
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
	
	public SortedMap<String, ReleaseFile> getCommitFiles(String commitHash){
		TreeMap<String, ReleaseFile> files = new TreeMap<>();
		ReleaseFile relFile;
		ProcessBuilder pb = new ProcessBuilder("git", "ls-tree", "-r", commitHash);
		pb.directory(filePath);
		String line=null;
		try {
			Process process = pb.start();
			BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
			while ((line = br.readLine())!=null){
				if (line.endsWith(JAVAFILE)){
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
		ProcessBuilder pb = new ProcessBuilder("git", "show", SHORTDATE, "--numstat", ONELINE, FORMAT + DATE, commitHash);
		pb.directory(filePath);
		String line=null;
		try {
			Process process = pb.start();
			BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
			if ((line = br.readLine())!=null){
				comm = new CommitWithChanges(LocalDate.parse(line));
				while ((line = br.readLine())!=null){
					if (line.endsWith(JAVAFILE)){
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
		ProcessBuilder pb = new ProcessBuilder("git", "log", FIRST_PARENT, MASTER, BEFORE + release.getDate().toString(), FORMAT + HASH, MAXCOUNT_1);
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
	public void setReleasesCommits(List<Release> releases){
		TreeMap<String, String> commits;
		ProcessBuilder pb;
		for(int i=0; i<releases.size(); i++){
			commits = new TreeMap<>();
			if (i==0){
				pb = new ProcessBuilder("git", "log", FIRST_PARENT, MASTER, BEFORE + releases.get(i).getDate(), FORMAT + "\"%H", "%an\"");
			} else {
				pb = new ProcessBuilder("git", "log", FIRST_PARENT, MASTER, BEFORE + releases.get(i).getDate(), AFTER + releases.get(i-1).getDate(), FORMAT + "\"%H", "%an\"");
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
	private void recordIncrementalValues(List<Release> releases){
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
	
	private void setCreationDate(List<Release> releases){
		String updatedName;
		for(Release release : releases){
			for(String fileName : release.getFiles().keySet()){
				for (Release r : releases){
					updatedName=getMatchingName(fileName, r);
					if (updatedName!=null && fileCreationTable.containsKey((updatedName))){
						release.getFiles().get(fileName).setCreationDate(fileCreationTable.get(updatedName));
						break;
					}
				}
				if (release.getFiles().get(fileName).getCreationDate()==null){
					release.getFiles().get(fileName).setCreationDate(getFileCreationDate(fileName));
				}
			}
		}
	}
	
	private void doRevisionsAndSetAuthors(Release release){
		String updatedName;
		CommitWithChanges comm;
		int commitAge;
		for(String commit : release.getCommits().keySet()){
			if ((comm = getCommitChanges(commit)) != null){
				for(String name : comm.getFilesChanged().keySet()){
					updatedName=getMatchingName(name, release);
					if (updatedName!=null && release.getFiles().containsKey(updatedName)){
						commitAge = (int) ChronoUnit.WEEKS.between(comm.getDate(), release.getDate());
						release.getFiles().get(updatedName).doRevision(comm.getFilesChanged().get(name)[0], comm.getFilesChanged().get(name)[1], comm.getFilesChanged().keySet().size(), commitAge);
						release.getFiles().get(updatedName).addAuthor(release.getCommits().get(commit));
					}
				}
			}
		}
	}
	
	public void setReleasesMetrics(List<Release> releases){
		//build up rename tables
		for(Release release : releases){
			for(String commit : release.getCommits().keySet()){
				getCommitRenamesAndCreations(commit);
			}
		}
		setCreationDate(releases);
		for(Release release : releases)
			doRevisionsAndSetAuthors(release);
		recordIncrementalValues(releases);
	}
	
}
