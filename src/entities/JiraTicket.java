package entities;

import java.time.LocalDate;

public class JiraTicket implements Comparable <JiraTicket>{

	private String id;
	private LocalDate creationDate;
	private Release fixedVersion;
	private Release openingVersion;
	private Release injectedVersion;
	private boolean estimatedIv;
	
	public JiraTicket(String id, LocalDate creationDate){
		setId(id);
		setCreationDate(creationDate);
		setFixedVersion(null);
		setOpeningVersion(null);
		setEstimatedIv(false);
	}
	
	public boolean hasEstimatedIv() {
		return estimatedIv;
	}

	public void setEstimatedIv(boolean estimatedIv) {
		this.estimatedIv = estimatedIv;
	}

	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public LocalDate getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(LocalDate creationDate) {
		this.creationDate = creationDate;
	}
	
	public Release getFixedVersion() {
		return fixedVersion;
	}
	
	public void setFixedVersion(Release fixedVersion) {
		this.fixedVersion = fixedVersion;
	}
	
	public Release getOpeningVersion() {
		return openingVersion;
	}
	
	public void setOpeningVersion(Release openingVersion) {
		this.openingVersion = openingVersion;
	}
	
	public Release getInjectedVersion() {
		return injectedVersion;
	}
	
	public void setInjectedVersion(Release injectedVersion) {
		this.injectedVersion = injectedVersion;
	}
	
	@Override
    public int compareTo(JiraTicket j) {
        return this.getFixedVersion().compareTo(j.getFixedVersion());
    }
}
