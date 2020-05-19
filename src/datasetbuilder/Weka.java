package datasetbuilder;

import weka.attributeSelection.BestFirst;
import weka.attributeSelection.CfsSubsetEval;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.lazy.IBk;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.converters.CSVLoader;
import weka.filters.Filter;
import weka.filters.supervised.attribute.AttributeSelection;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import entities.ClassifierEvaluation;


public class Weka {
	
	private static final Logger LOGGER = Logger.getLogger(Weka.class.getName());
	private static final double SPLIT_PERCENTAGE = 0.66;
	
	private Weka(){
		//not called
	}
	
	private static Evaluation evaluateNaiveBayes(Instances training, Instances testing){
		NaiveBayes classifierNB = new NaiveBayes();
		Evaluation eval;
		try{
			classifierNB.buildClassifier(training);
			eval = new Evaluation(testing);
			eval.evaluateModel(classifierNB, testing);
			return eval;
		}catch(Exception e){
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
		return null;
	}
	
	private static Evaluation evaluateRandomForest(Instances training, Instances testing){
		RandomForest classifierRF = new RandomForest();
		Evaluation eval;
		try{
			classifierRF.buildClassifier(training);
			eval = new Evaluation(testing);
			eval.evaluateModel(classifierRF, testing);
			return eval;
		}catch(Exception e){
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
		return null;
	}
	
	private static Evaluation evaluateIBk(Instances training, Instances testing){
		IBk classifierIBk = new IBk();
		Evaluation eval;
		try{
			classifierIBk.buildClassifier(training);
			eval = new Evaluation(testing);
			eval.evaluateModel(classifierIBk, testing);
			return eval;
		}catch(Exception e){
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
		return null;
	}

	public static List<ClassifierEvaluation> walkForwardEvaluations(String datasetPath){
		int totalRevs;
		int totalTrainRevs;
		int lastTrainInst;
		int lastTestInst = 0;
		Instances training;
		Instances testing;
		Instances filteredTraining;
		Instances filteredTesting;
		ArrayList<ClassifierEvaluation> evaluations = new ArrayList<>();
		AttributeSelection filter = new AttributeSelection();
		CfsSubsetEval eval = new CfsSubsetEval();
	    BestFirst search = new BestFirst();
	    filter.setEvaluator(eval);
	    filter.setSearch(search);
		try {
			CSVLoader loader = new CSVLoader();
			loader.setSource(new File(datasetPath));
			Instances data = loader.getDataSet();
			filter.setInputFormat(data);
			totalRevs = data.numDistinctValues(0);
			totalTrainRevs = (int) Math.round((float) totalRevs * SPLIT_PERCENTAGE);
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
				filteredTraining = Filter.useFilter(training, filter);
				filteredTesting = Filter.useFilter(testing, filter);
				filteredTraining.setClassIndex(filteredTraining.numAttributes() - 1);
				filteredTesting.setClassIndex(filteredTesting.numAttributes() - 1);
				
				evaluations.add(new ClassifierEvaluation("NaiveBayes", i+1, evaluateNaiveBayes(training, testing), false));
				evaluations.add(new ClassifierEvaluation("NaiveBayes", i+1, evaluateNaiveBayes(filteredTraining, filteredTesting), true));
				evaluations.add(new ClassifierEvaluation("RandomForest", i+1, evaluateRandomForest(training, testing), false));
				evaluations.add(new ClassifierEvaluation("RandomForest", i+1, evaluateRandomForest(filteredTraining, filteredTesting), true));
				evaluations.add(new ClassifierEvaluation("IBk", i+1, evaluateIBk(training, testing), false));
				evaluations.add(new ClassifierEvaluation("IBk", i+1, evaluateIBk(filteredTraining, filteredTesting), true));
			}
	    } catch(Exception e){
	    	LOGGER.log(Level.SEVERE, e.getMessage(), e);
	    }
	    return evaluations;
	}
	
}
