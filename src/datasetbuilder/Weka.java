package datasetbuilder;

import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.lazy.IBk;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.converters.CSVLoader;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import entities.ClassifierEvaluation;


public class Weka {
	
	private static final Logger LOGGER = Logger.getLogger(Weka.class.getName());
	
	private Weka(){
		//not called
	}

	public static List<ClassifierEvaluation> walkForwardEvaluations(String datasetPath){
		int totalRevs;
		int totalTrainRevs;
		int lastTrainInst;
		int lastTestInst = 0;
		Instances training;
		Instances testing;
		Evaluation eval;
		NaiveBayes classifierNB;
		RandomForest classifierRF;
		IBk classifierIBk;
		ArrayList<ClassifierEvaluation> evaluations = new ArrayList<>();
		try {
			// load CSV
			CSVLoader loader = new CSVLoader();
			loader.setSource(new File(datasetPath));
			Instances data = loader.getDataSet();//get instances object
			totalRevs = data.numDistinctValues(0);
			totalTrainRevs = (int) Math.round((float) totalRevs * 0.66);
			for(int i=0; i<totalTrainRevs - 1; i++){
				lastTrainInst = lastTestInst;
				for(int j=lastTestInst; j<data.numInstances(); j++){
					if (data.get(j).value(0) - 1 <= i)
						lastTrainInst++;
					else
						break;
				}
				lastTestInst = lastTrainInst;
				for(int j=lastTrainInst; j<data.numInstances(); j++){
					if (data.get(j).value(0) - 1 <= i + 1)
						lastTestInst++;
					else
						break;
				}
				training = new Instances(data, 0, lastTrainInst);
				testing = new Instances(data, lastTrainInst, lastTestInst - lastTrainInst);
				training.setClassIndex(training.numAttributes() - 1);
				testing.setClassIndex(testing.numAttributes() - 1);	    	
				classifierNB = new NaiveBayes();
				classifierRF = new RandomForest();
				classifierIBk = new IBk();
				classifierNB.buildClassifier(training);
				classifierRF.buildClassifier(training);
				classifierIBk.buildClassifier(training);
				eval = new Evaluation(testing);
				eval.evaluateModel(classifierNB, testing); 
				evaluations.add(new ClassifierEvaluation("NaiveBayes", i+1, eval));
				eval = new Evaluation(testing);
				eval.evaluateModel(classifierRF, testing);
				evaluations.add(new ClassifierEvaluation("RandomForest", i+1, eval));
				eval = new Evaluation(testing);
				eval.evaluateModel(classifierIBk, testing);
				evaluations.add(new ClassifierEvaluation("IBk", i+1, eval));
			}
	    } catch(Exception e){
	    	LOGGER.log(Level.SEVERE, e.getMessage(), e);
	    }
	    return evaluations;
	}
	
}
