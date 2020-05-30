package datasetbuilder;

import weka.attributeSelection.BestFirst;
import weka.attributeSelection.CfsSubsetEval;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.lazy.IBk;
import weka.classifiers.meta.FilteredClassifier;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.converters.CSVLoader;
import weka.filters.Filter;
import weka.filters.supervised.attribute.AttributeSelection;
import weka.filters.supervised.instance.SpreadSubsample;
import weka.filters.supervised.instance.Resample;
import weka.filters.supervised.instance.SMOTE;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import entities.ClassifierEvaluation;


public class Weka {
	
	private static final Logger LOGGER = Logger.getLogger(Weka.class.getName());
	private static final double SPLIT_PERCENTAGE = 1;
	private static final String NAIVEBAYES = "NaiveBayes";
	private static final String RANDOMFOREST = "RandomForest";
	private static final String IBK = "IBk";
	private static final String NO_SAMPLING = "noSampling";
	private static final String UNDERSAMPLING = "underSampling";
	private static final String OVERSAMPLING = "overSampling";
	private static final String SMOTE = "smote";
	private static final String INVALIDSAMPLING = "Invalid sampling parameter";
	private static final String INVALIDCLASSIFIER = "invalid classifier parameter";
	
	private Weka(){
		//not called
	}
	
	private static Evaluation evaluateClassifier(String classif, Instances training, Instances testing, String sampling, double majorityClassPercentage){
		Classifier classifier;
		switch(classif){
		case NAIVEBAYES:
			classifier =  new NaiveBayes();
			break;
		case RANDOMFOREST:
			classifier = new RandomForest();
			break;
		case IBK:
			classifier = new IBk();
			break;
		default:
			LOGGER.log(Level.SEVERE, INVALIDCLASSIFIER);
			return null;
		}
		FilteredClassifier fc = new FilteredClassifier();
		fc.setClassifier(classifier);
		try {
			Evaluation eval = new Evaluation(testing);
			String[] opts;
			switch(sampling){
			case NO_SAMPLING:
				classifier.buildClassifier(training);
				eval.evaluateModel(classifier, testing);
				return eval;
			case UNDERSAMPLING:
				SpreadSubsample  spreadSubsample = new SpreadSubsample();
				opts = new String[]{ "-M", "1.0"};
				spreadSubsample.setOptions(opts);
				fc.setFilter(spreadSubsample);
				break;
			case OVERSAMPLING:
				Resample resample = new Resample();
				opts = new String[]{"-B", "1.0", "-Z", Double.toString(2 * majorityClassPercentage)};
				resample.setOptions(opts);
				fc.setFilter(resample);
				break;
			case SMOTE:
				SMOTE smote = new SMOTE();
				fc.setFilter(smote);
				break;
			default:
				LOGGER.log(Level.SEVERE, INVALIDSAMPLING);
				return null;
			}
			fc.buildClassifier(training);
			eval.evaluateModel(fc, testing);
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
		int defectInTraining = 0;
		int defectInTesting;
		double trainingPercentage;
		double defectInTrainingPercentage;
		double defectInTestingPercentage;
		double majorityClassPercentage;
		Instances training;
		Instances testing;
		Instances selectedTraining;
		Instances selectedTesting;
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
			data.setClassIndex(data.numAttributes() -1);
			filter.setInputFormat(data);
			totalRevs = data.numDistinctValues(0);
			totalTrainRevs = (int) Math.round((float) totalRevs * SPLIT_PERCENTAGE);
			
			for(int i=0; i<totalTrainRevs - 1; i++){
				defectInTesting = 0;
				
				lastTrainInst = lastTestInst;
				while( (lastTrainInst < data.numInstances()) && (data.get(lastTrainInst).value(0) -1 <= i) ){
					if (data.get(lastTrainInst).stringValue(data.classIndex()).equals("yes"))
						defectInTraining++;
					lastTrainInst++;
				}
				lastTestInst = lastTrainInst;
				while( (lastTestInst < data.numInstances()) && (data.get(lastTestInst).value(0) -1 <= i + 1) ){
					if (data.get(lastTestInst).stringValue(data.classIndex()).equals("yes"))
						defectInTesting++;
					lastTestInst++;
				}
				
				training = new Instances(data, 0, lastTrainInst);
				testing = new Instances(data, lastTrainInst, lastTestInst - lastTrainInst);
				training.setClassIndex(training.numAttributes() - 1);
				testing.setClassIndex(testing.numAttributes() - 1);
				selectedTraining = Filter.useFilter(training, filter);
				selectedTesting = Filter.useFilter(testing, filter);
				selectedTraining.setClassIndex(selectedTraining.numAttributes() - 1);
				selectedTesting.setClassIndex(selectedTesting.numAttributes() - 1);
				trainingPercentage = (double) training.numInstances() / data.numInstances() * 100;
				defectInTrainingPercentage = (double) defectInTraining / training.numInstances() * 100;
				defectInTestingPercentage = (double) defectInTesting / testing.numInstances() * 100;
				majorityClassPercentage = Math.max(defectInTrainingPercentage, 100 - defectInTrainingPercentage);
				
				evaluations.add(new ClassifierEvaluation(NAIVEBAYES, i+1, evaluateClassifier(NAIVEBAYES, training, testing, NO_SAMPLING, majorityClassPercentage), false, trainingPercentage, new double[] {defectInTrainingPercentage, defectInTestingPercentage}, NO_SAMPLING));
				evaluations.add(new ClassifierEvaluation(NAIVEBAYES, i+1, evaluateClassifier(NAIVEBAYES, selectedTraining, selectedTesting, NO_SAMPLING, majorityClassPercentage), true, trainingPercentage,  new double[] {defectInTrainingPercentage, defectInTestingPercentage}, NO_SAMPLING));
				evaluations.add(new ClassifierEvaluation(NAIVEBAYES, i+1, evaluateClassifier(NAIVEBAYES, training, testing, UNDERSAMPLING, majorityClassPercentage), false, trainingPercentage,  new double[] {defectInTrainingPercentage, defectInTestingPercentage}, UNDERSAMPLING));
				evaluations.add(new ClassifierEvaluation(NAIVEBAYES, i+1, evaluateClassifier(NAIVEBAYES, selectedTraining, selectedTesting, UNDERSAMPLING, majorityClassPercentage), true, trainingPercentage,  new double[] {defectInTrainingPercentage, defectInTestingPercentage}, UNDERSAMPLING));
				evaluations.add(new ClassifierEvaluation(NAIVEBAYES, i+1, evaluateClassifier(NAIVEBAYES, training, testing, OVERSAMPLING, majorityClassPercentage), false, trainingPercentage,  new double[] {defectInTrainingPercentage, defectInTestingPercentage}, OVERSAMPLING));
				evaluations.add(new ClassifierEvaluation(NAIVEBAYES, i+1, evaluateClassifier(NAIVEBAYES, selectedTraining, selectedTesting, OVERSAMPLING, majorityClassPercentage), true, trainingPercentage,  new double[] {defectInTrainingPercentage, defectInTestingPercentage}, OVERSAMPLING));
				evaluations.add(new ClassifierEvaluation(NAIVEBAYES, i+1, evaluateClassifier(NAIVEBAYES, training, testing, SMOTE, majorityClassPercentage), false, trainingPercentage,  new double[] {defectInTrainingPercentage, defectInTestingPercentage}, SMOTE));
				evaluations.add(new ClassifierEvaluation(NAIVEBAYES, i+1, evaluateClassifier(NAIVEBAYES, selectedTraining, selectedTesting, SMOTE, majorityClassPercentage), true, trainingPercentage,  new double[] {defectInTrainingPercentage, defectInTestingPercentage}, SMOTE));
				
				evaluations.add(new ClassifierEvaluation(RANDOMFOREST, i+1, evaluateClassifier(RANDOMFOREST, training, testing, NO_SAMPLING, majorityClassPercentage), false, trainingPercentage,  new double[] {defectInTrainingPercentage, defectInTestingPercentage}, NO_SAMPLING));
				evaluations.add(new ClassifierEvaluation(RANDOMFOREST, i+1, evaluateClassifier(RANDOMFOREST, selectedTraining, selectedTesting, NO_SAMPLING, majorityClassPercentage), true, trainingPercentage,  new double[] {defectInTrainingPercentage, defectInTestingPercentage}, NO_SAMPLING));
				evaluations.add(new ClassifierEvaluation(RANDOMFOREST, i+1, evaluateClassifier(RANDOMFOREST, training, testing, UNDERSAMPLING, majorityClassPercentage), false, trainingPercentage,  new double[] {defectInTrainingPercentage, defectInTestingPercentage}, UNDERSAMPLING));
				evaluations.add(new ClassifierEvaluation(RANDOMFOREST, i+1, evaluateClassifier(RANDOMFOREST, selectedTraining, selectedTesting, UNDERSAMPLING, majorityClassPercentage), true, trainingPercentage,  new double[] {defectInTrainingPercentage, defectInTestingPercentage}, UNDERSAMPLING));
				evaluations.add(new ClassifierEvaluation(RANDOMFOREST, i+1, evaluateClassifier(RANDOMFOREST, training, testing, OVERSAMPLING, majorityClassPercentage), false, trainingPercentage,  new double[] {defectInTrainingPercentage, defectInTestingPercentage}, OVERSAMPLING));
				evaluations.add(new ClassifierEvaluation(RANDOMFOREST, i+1, evaluateClassifier(RANDOMFOREST, selectedTraining, selectedTesting, OVERSAMPLING, majorityClassPercentage), true, trainingPercentage,  new double[] {defectInTrainingPercentage, defectInTestingPercentage}, OVERSAMPLING));
				evaluations.add(new ClassifierEvaluation(RANDOMFOREST, i+1, evaluateClassifier(RANDOMFOREST, training, testing, SMOTE, majorityClassPercentage), false, trainingPercentage,  new double[] {defectInTrainingPercentage, defectInTestingPercentage}, SMOTE));
				evaluations.add(new ClassifierEvaluation(RANDOMFOREST, i+1, evaluateClassifier(RANDOMFOREST, selectedTraining, selectedTesting, SMOTE, majorityClassPercentage), true, trainingPercentage,  new double[] {defectInTrainingPercentage, defectInTestingPercentage}, SMOTE));
				
				evaluations.add(new ClassifierEvaluation(IBK, i+1, evaluateClassifier(IBK, training, testing, NO_SAMPLING, majorityClassPercentage), false, trainingPercentage,  new double[] {defectInTrainingPercentage, defectInTestingPercentage}, NO_SAMPLING));
				evaluations.add(new ClassifierEvaluation(IBK, i+1, evaluateClassifier(IBK, selectedTraining, selectedTesting, NO_SAMPLING, majorityClassPercentage), true, trainingPercentage,  new double[] {defectInTrainingPercentage, defectInTestingPercentage}, NO_SAMPLING));
				evaluations.add(new ClassifierEvaluation(IBK, i+1, evaluateClassifier(IBK, training, testing, UNDERSAMPLING, majorityClassPercentage), false, trainingPercentage,  new double[] {defectInTrainingPercentage, defectInTestingPercentage}, UNDERSAMPLING));
				evaluations.add(new ClassifierEvaluation(IBK, i+1, evaluateClassifier(IBK, selectedTraining, selectedTesting, UNDERSAMPLING, majorityClassPercentage), true, trainingPercentage,  new double[] {defectInTrainingPercentage, defectInTestingPercentage}, UNDERSAMPLING));
				evaluations.add(new ClassifierEvaluation(IBK, i+1, evaluateClassifier(IBK, training, testing, OVERSAMPLING, majorityClassPercentage), false, trainingPercentage,  new double[] {defectInTrainingPercentage, defectInTestingPercentage}, OVERSAMPLING));
				evaluations.add(new ClassifierEvaluation(IBK, i+1, evaluateClassifier(IBK, selectedTraining, selectedTesting, OVERSAMPLING, majorityClassPercentage), true, trainingPercentage,  new double[] {defectInTrainingPercentage, defectInTestingPercentage}, OVERSAMPLING));
				evaluations.add(new ClassifierEvaluation(IBK, i+1, evaluateClassifier(IBK, training, testing, SMOTE, majorityClassPercentage), false, trainingPercentage,  new double[] {defectInTrainingPercentage, defectInTestingPercentage}, SMOTE));
				evaluations.add(new ClassifierEvaluation(IBK, i+1, evaluateClassifier(IBK, selectedTraining, selectedTesting, SMOTE, majorityClassPercentage), true, trainingPercentage,  new double[] {defectInTrainingPercentage, defectInTestingPercentage}, SMOTE));
				
				defectInTraining += defectInTesting;
			}
			
	    } catch(Exception e){
	    	LOGGER.log(Level.SEVERE, e.getMessage(), e);
	    }
	    return evaluations;
	}
	
}
