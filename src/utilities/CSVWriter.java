package utilities;

import java.io.FileWriter;
import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import entities.ClassifierEvaluation;
import entities.JiraTicket;
import entities.Release;

public class CSVWriter {
	
	private static final Logger LOGGER = Logger.getLogger(CSVWriter.class.getName());
	private String path;
	
	public CSVWriter(String path){
		this.path=path;
	}
	
	public void printReleasesInfo(String outname, List<Release> releases){
		try (
            FileWriter writer= new FileWriter(path + outname);
            CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT);)
			{
            printer.printRecord("Index","VersionID","VersionName","Date");
            for ( int i = 0; i < releases.size(); i++) {
            	printer.printRecord(i+1, releases.get(i).getId(), releases.get(i).getName(), releases.get(i).getDate());
            }
            printer.flush();

			} catch (IOException e) {
				LOGGER.log(Level.SEVERE, e.getMessage(), e);
			}
	}
	
	public void printJiraTickets(String outname, List<JiraTicket> tickets){
		String fv;
		String ov;
		String iv;
		try (
				FileWriter writer = new FileWriter(path + outname);
				CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT);)
			{
				printer.printRecord("TicketID","OpeningVersion","FixedVersion","InjectedVersion");
				for(int i=0; i<tickets.size(); i++){
					ov = "next";
					iv = null;
					fv = tickets.get(i).getFixedVersion().getName();
					if (tickets.get(i).getOpeningVersion()!=null)
						ov = tickets.get(i).getOpeningVersion().getName();
					if (tickets.get(i).getInjectedVersion()!=null)
						iv = tickets.get(i).getInjectedVersion().getName();
					
					if (tickets.get(i).hasEstimatedIv())
						printer.printRecord(tickets.get(i).getId(), ov, fv, iv, "stimato");
					else
						printer.printRecord(tickets.get(i).getId(), ov, fv, iv);
				}
			    printer.flush();
				
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, e.getMessage(), e);
			}
	}

	public void printReleasesFiles(String outname, List<Release> releases) {
		int ver=0;
		String bugged;
		int ageInWeeks;
		try (
	            FileWriter writer= new FileWriter(path + outname);
	            CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT);)
				{
	            printer.printRecord("Version","File","NR","LOC","LOC_touched","LOC_added","MAX_LOC_added","AVG_LOC_added",
	            					"Churn","MAX_Churn","AVG_Churn","NAuth","ChgSetSize","MAX_ChgSetSize","AVG_ChgSetSize",
	            					"AgeInWeeks", "AgeWeighted", "NFix", "Bugged");
	            for (Release rel : releases) {
	            	ver++;
	            	for (String name : rel.getFiles().keySet()){
	            		if (rel.getFiles().get(name).isBuggy())
	            			bugged = "yes";
	            		else
	            			bugged = "no";
	            		if (rel.getFiles().get(name).getCreationDate() != null){
	            			ageInWeeks = (int) ChronoUnit.WEEKS.between(rel.getFiles().get(name).getCreationDate(), rel.getDate());
	            			printer.printRecord(ver, rel.getFiles().get(name).getName(), rel.getFiles().get(name).getRevisions(),
	            					rel.getFiles().get(name).getLoc(), rel.getFiles().get(name).getLocModified(), 
	            					rel.getFiles().get(name).getLocAdded(), rel.getFiles().get(name).getMaxLocAdded(),
	            					rel.getFiles().get(name).getAvgLocAdded(), rel.getFiles().get(name).getChurn(),
	            					rel.getFiles().get(name).getMaxChurn(), rel.getFiles().get(name).getAvgChurn(), 
	            					rel.getFiles().get(name).getAuthorsNumber(), rel.getFiles().get(name).getChgSetSize(),
	            					rel.getFiles().get(name).getMaxChgSetSize(), rel.getFiles().get(name).getAvgChgSetSize(), 
	            					ageInWeeks, rel.getFiles().get(name).getWeightedAge(), rel.getFiles().get(name).getFixNumber(), bugged);
	            		}
	            	}
	            }
	            printer.flush();

				} catch (IOException e) {
					LOGGER.log(Level.SEVERE, e.getMessage(), e);
				}
	}
	
	public void printEvaluations(String proj, String outname, List<ClassifierEvaluation> evaluations){
		String selection;
		try (
	            FileWriter writer= new FileWriter(path + outname);
	            CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT);)
				{
	            printer.printRecord("Dataset","#TrainingRelease","%Training","%DefectiveInTraining","%DefectiveInTesting",
	            					"Classifier","Sampling","FeatureSelection","TP","FP","TN","FN","FPRate","TNRate","FNRate","Precision","Recall","ROCArea","Kappa");
	            for ( ClassifierEvaluation evaluation : evaluations) {
	            	selection = evaluation.hasFeatureSelection() ? "Yes" : "No";
	            	printer.printRecord(proj, evaluation.getTrainingSize(), evaluation.getTrainingPercentage(),
	            			evaluation.getDefectInTrainingPercentage(), evaluation.getDefectInTestingPercentage(),
	            			evaluation.getClassifier(), evaluation.getSampling(), selection,
	            			evaluation.getEval().numTruePositives(1), evaluation.getEval().numFalsePositives(1),
	            			evaluation.getEval().numTrueNegatives(1), evaluation.getEval().numFalseNegatives(1),
	            			evaluation.getEval().falsePositiveRate(1), evaluation.getEval().trueNegativeRate(1),
	            			evaluation.getEval().falseNegativeRate(1), evaluation.getEval().precision(1), 
	            			evaluation.getEval().recall(1), evaluation.getEval().areaUnderROC(1), evaluation.getEval().kappa());
	            }
	            printer.flush();

				} catch (IOException e) {
					LOGGER.log(Level.SEVERE, e.getMessage(), e);
				}
	}
	
}
