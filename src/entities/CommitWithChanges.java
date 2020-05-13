package entities;

import java.time.LocalDate;
import java.util.TreeMap;

public class CommitWithChanges {
	
	private LocalDate date;
	private TreeMap<String, int[]> filesChanged;
	
	public CommitWithChanges(LocalDate date){
		this.date = date;
		this.filesChanged = new TreeMap<String, int[]>();
	}
	
	public LocalDate getDate(){
		return this.date;
	}
	
	public void addChangedFile(String file, int addedLoc, int removedLoc){
		this.filesChanged.put(file, new int[]{addedLoc, removedLoc});
	}
	
	public TreeMap<String, int[]> getFilesChanged(){
		return this.filesChanged;
	}

}
