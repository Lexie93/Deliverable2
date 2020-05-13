package entities;

import java.time.LocalDate;
import java.util.ArrayList;

public class ReleaseFile{
	
	private String hash;
	private String name;
	private int loc;
	private int locAdded;
	private int locRemoved;
	private int maxLocAdded;
	private int maxChurn;
	private int revisions;
	private int bugs;
	private ArrayList<String> authors;
	private int chgSetSize;
	private int maxChgSetSize;
	private LocalDate creationDate;
	private long weightedLocAgeSum;
	private int fixNumber;

	public ReleaseFile(String hash, String name){
		this.hash = hash;
		this.name = name;
		this.bugs = 0;
		this.maxChurn = 0;
		this.maxChgSetSize = 0;
		this.fixNumber = 0;
		this.authors = new ArrayList<String>();
		this.creationDate = null;
	}

	//LOC of the file in previous Release
	public void sumToLoc(int loc) {
		this.loc += loc;
	}
	
	public void doRevision(int add, int rem, int setSize, int age){
		revisions++;
		locAdded += add;
		locRemoved += rem;
		loc = loc + add - rem;
		chgSetSize += setSize;
		weightedLocAgeSum += age * (add + rem);
		if (add > maxLocAdded)
			maxLocAdded = add;
		if (add-rem > maxChurn)
			maxChurn = add-rem;
		if (setSize > maxChgSetSize)
			maxChgSetSize = setSize;
	}
	
	public int getFixNumber(){
		return this.fixNumber;
	}
	
	public void incFixNumber(){
		this.fixNumber++;
	}
	
	public int getWeightedAge(){
		if (getLocModified()>0)
			return (int) Math.round(((float) weightedLocAgeSum )/getLocModified());
		return 0;
	}
	
	public LocalDate getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(LocalDate creationDate) {
		this.creationDate = creationDate;
	}

	public int getMaxChgSetSize(){
		return this.maxChgSetSize;
	}
	
	public int getChgSetSize(){
		return this.chgSetSize;
	}

	public String getHash() {
		return hash;
	}

	public String getName() {
		return name;
	}
	
	public ArrayList<String> getAuthors(){
		return authors;
	}
	
	public int getAuthorsNumber(){
		return authors.size();
	}
	
	public void addAuthor(String auth){
		if (!this.authors.contains(auth))
			authors.add(auth);
	}
	
	public boolean isBuggy(){
		return bugs>0 ? true : false;
	}
	
	public int getBugs() {
		return bugs;
	}

	public void incBugs() {
		this.bugs++;
	}
	
	public int getMaxLocAdded() {
		return maxLocAdded;
	}
	
	public int getMaxChurn(){
		return maxChurn;
	}

	public int getRevisions() {
		return revisions;
	}

	public int getLoc() {
		return loc;
	}

	public int getLocAdded() {
		return locAdded;
	}

	public int getLocRemoved() {
		return locRemoved;
	}
	
	public int getLocModified(){
		return locAdded + locRemoved;
	}
	
	public int getChurn(){
		return locAdded - locRemoved;
	}
	
	//round avg to closest int
	public int getAvgLocAdded(){
		if (revisions>0)
			return (int) Math.round(((float) locAdded )/revisions);
		return 0;
	}
	
	//round avg to closest int
	public int getAvgChurn(){
		if (revisions>0)
			return (int) Math.round(((float) locAdded-locRemoved )/revisions);
		return 0;
	}
	
	//round avg to closest int
	public int getAvgChgSetSize(){
		if (revisions>0)
			return (int) Math.round(((float) chgSetSize )/revisions);
		return 0;
	}
	
}
