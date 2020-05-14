package entities;

import java.time.LocalDate;
import java.util.SortedMap;
import java.util.TreeMap;

public class Release /*implements Comparable <Release>*/{
	
	private int version;
	private String id;
	private String name;
	private LocalDate date;
	private TreeMap<String, String> commits;
	//files of last release commit
	private TreeMap<String, ReleaseFile> files;
	
	public Release(String id, String name, LocalDate date){
		setId(id);
		setName(name);
		setDate(date);
		setCommits(null);
		setFiles(null);
	}
	
	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public SortedMap<String, ReleaseFile> getFiles() {
		return files;
	}

	public void setFiles(SortedMap<String, ReleaseFile> sortedMap) {
		this.files = (TreeMap<String, ReleaseFile>) sortedMap;
	}

	public SortedMap<String, String> getCommits() {
		return commits;
	}

	public void setCommits(SortedMap<String, String> commits) {
		this.commits = (TreeMap<String, String>) commits;
	}

	public void setId(String id){
		this.id=id;
	}
	
	public String getId(){
		return id;
	}
	
	public void setName(String name){
		this.name=name;
	}
	
	public String getName(){
		return name;
	}
	
	public void setDate(LocalDate date){
		this.date=date;
	}
	
	public LocalDate getDate(){
		return date;
	}
	
}
