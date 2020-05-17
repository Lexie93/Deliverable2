package entities;

import weka.classifiers.Evaluation;

public class ClassifierEvaluation {
	
	private String classifier;
	private int trainingSize;
	private Evaluation eval;
	
	public ClassifierEvaluation(String classifier, int trainingSize, Evaluation eval) {
		this.classifier = classifier;
		this.trainingSize = trainingSize;
		this.eval = eval;
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
	
}
