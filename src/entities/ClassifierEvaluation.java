package entities;

import weka.classifiers.Evaluation;

public class ClassifierEvaluation {
	
	private String classifier;
	private int trainingSize;
	private Evaluation eval;
	private boolean selection;
	private double trainingPercentage;
	private double defectInTrainingPercentage;
	private double defectInTestingPercentage;
	
	public ClassifierEvaluation(String classifier, int trainingSize, Evaluation eval, boolean selection, double trainingPercentage,
															double defectInTrainingPercentage, double defectInTestingPercentage) {
		this.classifier = classifier;
		this.trainingSize = trainingSize;
		this.eval = eval;
		this.selection = selection;
		this.trainingPercentage = trainingPercentage;
		this.defectInTrainingPercentage = defectInTrainingPercentage;
		this.defectInTestingPercentage = defectInTestingPercentage;
	}

	public String getClassifier() {
		return classifier;
	}

	public int getTrainingSize() {
		return trainingSize;
	}

	public Evaluation getEval() {
		return eval;
	}
	
	public boolean hasFeatureSelection(){
		return selection;
	}

	public double getTrainingPercentage() {
		return trainingPercentage;
	}

	public double getDefectInTrainingPercentage() {
		return defectInTrainingPercentage;
	}

	public double getDefectInTestingPercentage() {
		return defectInTestingPercentage;
	}
	
}
