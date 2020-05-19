package entities;

import weka.classifiers.Evaluation;

public class ClassifierEvaluation {
	
	private String classifier;
	private int trainingSize;
	private Evaluation eval;
	private boolean selection;
	
	public ClassifierEvaluation(String classifier, int trainingSize, Evaluation eval, boolean selection) {
		this.classifier = classifier;
		this.trainingSize = trainingSize;
		this.eval = eval;
		this.selection = selection;
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
	
}
