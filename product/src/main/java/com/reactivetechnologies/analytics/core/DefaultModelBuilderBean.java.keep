package com.reactivetechnologies.data.analytics.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;
import org.springframework.objenesis.ObjenesisHelper;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.core.Instances;
import weka.core.WekaException;

class DefaultModelBuilderBean
{
	/**
	 * 
	 * @param classifier
	 * @param trainingDataSet
	 * @param testDataSet
	 * @param maxNumFolds
	 * @return
	 * @throws WekaException
	 */
	static RegressionModel buildCrossValidatedModel(Classifier classifier,
			Instances trainingDataSet, Instances testDataSet, int maxNumFolds) throws Exception
	{

		Evaluation evaluator = null;
				
		double error = Double.MAX_VALUE;
		RegressionModel model = null;
		for (int numFolds = maxNumFolds; numFolds >= 2; numFolds--)
		{
			
			// Make a copy of the data we can reorder
			Instances trainingData = new Instances(trainingDataSet);
			Random random = new Random();
			trainingData.randomize(random );
			if (trainingData.classAttribute().isNominal())
			{
				trainingData.stratify(numFolds);
			}
			
			for (int i = 0; i < numFolds; i++)
			{
				Instances train = trainingData.trainCV(numFolds, i, random);
				try
				{
					evaluator = new Evaluation(train);
					//evaluator.setPriors(train);
					Classifier copiedClassifier = Classifier.makeCopy(classifier);
					copiedClassifier.buildClassifier(train);
					log.debug("Cross validation pass- "+numFolds);
					Instances testData = new Instances(testDataSet);
					evaluator.evaluateModel(copiedClassifier, testData);
					if (evaluator.rootMeanSquaredError() < error)
					{
						error = evaluator.rootMeanSquaredError();
						
						model = new RegressionModel();
						model.setMeanAbsErr(evaluator.meanAbsoluteError());
						model.setRootMeanSqErr(evaluator.rootMeanSquaredError());
						model.setPctIncorrect(evaluator.pctIncorrect());
						model.setTrainedClassifier(copiedClassifier);
						model.setTrainingSet(train);
						
						model.setFolds(numFolds);
						log.debug(model);
						
					}
				}
				catch (Exception e)
				{
					log.warn(e);
					log.debug(e.getMessage(), e);
				}

			}
		}
		return model;

	}
	
	
	private static final Logger log = Logger.getLogger(DefaultModelBuilderBean.class);
	/**
	 * @deprecated
	 * @param classifier
	 * @param trainingDataSet
	 * @param maxFolds
	 * @param testDataSet
	 * @return
	 * @throws Exception
	 */
	private static RegressionModel evaluateClassifier(Classifier classifier, Instances trainingDataSet, int maxFolds, Instances testDataSet) throws Exception
	{
		RegressionModel best = null;
		double MAD = Double.MAX_VALUE;
		Classifier copiedClassifier = Classifier.makeCopy(classifier);
		RegressionModel modelOnFolds = buildCrossValidatedModel(classifier, trainingDataSet, testDataSet, maxFolds);
		
		copiedClassifier = Classifier.makeCopy(classifier);
		RegressionModel modelOnFullSet = validateModel(copiedClassifier, trainingDataSet, testDataSet);
										
		if(modelOnFolds.getRootMeanSqErr() <=  modelOnFullSet.getRootMeanSqErr())
		{
			
			if(modelOnFolds.getRootMeanSqErr() < MAD)
			{
				log.debug(modelOnFolds.toString());
				MAD = modelOnFolds.getRootMeanSqErr();
				best = modelOnFolds;
				log.info("Better Fit [folds= " + modelOnFolds.getFolds() + "] "+ classifier.getClass().getSimpleName());
			}
		}
		else
		{
			
			if(modelOnFullSet.getRootMeanSqErr() < MAD)
			{
				log.debug(modelOnFullSet.toString());
				MAD = modelOnFullSet.getRootMeanSqErr();
				best = modelOnFullSet;
				log.info("Better Fit [folds= " + modelOnFullSet.getFolds() + "] "+ classifier.getClass().getSimpleName());
			}
		}
		return best;
	}
	
	public RegressionModel buildCrossValidatedModel(Classifier classifier,
			Instances trainingDataSet, int maxNumFolds) throws Exception
	{
		log.debug("Cross validating without test data set");
		Evaluation evaluator = null;
				
		double error = Double.MAX_VALUE;
		RegressionModel model = null;
		for (int numFolds = maxNumFolds; numFolds >= 2; numFolds--)
		{
							
			Instances trainingData = new Instances(trainingDataSet);
			Random random = new Random();
			trainingData.randomize(random );
			if (trainingData.classAttribute().isNominal())
			{
				trainingData.stratify(numFolds);
			}
			
			for (int i = 0; i < numFolds; i++)
			{
				Instances train = trainingData.trainCV(numFolds, i, random);
				try
				{
					evaluator = new Evaluation(train);
					//evaluator.setPriors(train);
					Classifier copiedClassifier = Classifier.makeCopy(classifier);
					copiedClassifier.buildClassifier(train);
					
					Instances test = trainingData.testCV(numFolds, i);
					log.debug("Cross validation pass- "+numFolds);
					evaluator.evaluateModel(copiedClassifier, test);
					if (evaluator.rootMeanSquaredError() < error)
					{
						error = evaluator.rootMeanSquaredError();
						
						model = new RegressionModel();
						model.setMeanAbsErr(evaluator.meanAbsoluteError());
						model.setRootMeanSqErr(evaluator.rootMeanSquaredError());
						model.setPctIncorrect(evaluator.pctIncorrect());
						model.setTrainedClassifier(copiedClassifier);
						model.setTrainingSet(train);
						model.setFolds(numFolds);
						
						log.debug(model);
					}
				}
				catch (Exception e)
				{
					throw e;
				}

			}
		}
		return model;

	}
	
	/**
	 * @deprecated
	 * @param trainingDataSet
	 * @param maxFolds
	 * @return
	 */
	
	public RegressionModel getBestFitModelWithinFoldRange(final Instances trainingDataSet, final int maxFolds)
	{
	  RegressionModel best = null;
		double MAD = Double.MAX_VALUE;

		List<Future<RegressionModel>> results = new ArrayList<>();
		log.info("Evaluating best fit weka algorithm. Using trainingDataSet for evaluation");
		ExecutorService threads = Executors.newFixedThreadPool(4);
		for (final String className : getAvailableClassifiers())
		{
			
				final Classifier classifier;
				try
				{
					classifier = (Classifier) ObjenesisHelper.newInstance(Class.forName(className));
				}
				catch (Exception e)
				{
					log.warn(e);
					continue;
				}
									
				Future<RegressionModel> result = threads.submit(new Callable<RegressionModel>()
				{

					@Override
					public RegressionModel call() throws Exception
					{

						log.info("Evaluating classifier: " + classifier.getClass().getSimpleName());
						Classifier copiedClassifier = Classifier.makeCopy(classifier);
						return buildCrossValidatedModel(copiedClassifier, trainingDataSet, maxFolds);
						
						
					}
				});
				
				results.add(result);
					
			
		}
		
		for(Future<RegressionModel> future : results)
		{
			RegressionModel classifierModel;
			try
			{
				classifierModel = future.get(15, TimeUnit.MINUTES);
				if(classifierModel.getRootMeanSqErr() < MAD)
				{
					MAD = classifierModel.getRootMeanSqErr();
					best = classifierModel;
					log.info(classifierModel.getTrainedClassifier().getClass().getSimpleName() + " current BEST FIT");
				}
			}
			catch (InterruptedException | ExecutionException | TimeoutException e)
			{
				log.warn(e);
				log.debug(e.getMessage(), e);
			}
					
		}
		threads.shutdownNow();
		
		return best;

	}
	
	/**
	 * @deprecated
	 * @param classifier
	 * @param testDataSet
	 * @return RMS error
	 * @throws Exception
	 */
	private static RegressionModel validateModel(Classifier classifier, Instances trainingDataSet, Instances testDataSet) throws Exception
	{
		
		RegressionModel model = new RegressionModel();
		classifier.buildClassifier(trainingDataSet);
		model.setTrainedClassifier(classifier);
		model.setTrainingSet(trainingDataSet);
		
		Evaluation eval = new Evaluation(trainingDataSet);
		//eval.setDiscardPredictions(true);
		eval.evaluateModel(classifier, testDataSet);
		
		model.setFolds(1);
		model.setMeanAbsErr(eval.meanAbsoluteError());
		model.setPctIncorrect(eval.pctIncorrect());
		model.setRootMeanSqErr(eval.rootMeanSquaredError());
		return model;
	}
	
	private final List<String>	availableClassifiers	= new ArrayList<String>();
	
	/**
	 * @deprecated
	 * @param trainingDataSet
	 * @param maxFolds
	 * @param testDataSet
	 * @return
	 */
	
	public RegressionModel getBestFitModelWithinFoldRange(final Instances trainingDataSet, final int maxFolds, final Instances testDataSet)
	{
	  RegressionModel best = null;
		double MAD = Double.MAX_VALUE;

		List<Future<RegressionModel>> results = new ArrayList<>();
		log.info("Evaluating best fit weka algorithm. Using testDataSet for evaluation");
		ExecutorService threads = Executors.newFixedThreadPool(4);
		for (final String className : getAvailableClassifiers())
		{
			
				final Classifier classifier;
				try
				{
					classifier = (Classifier) ObjenesisHelper.newInstance(Class.forName(className));
				}
				catch (Exception e)
				{
					log.warn(e);
					continue;
				}
									
				Future<RegressionModel> result = threads.submit(new Callable<RegressionModel>()
				{

					@Override
					public RegressionModel call() throws Exception
					{

						log.info("Evaluating classifier: " + classifier.getClass().getSimpleName());
						return evaluateClassifier(classifier, trainingDataSet, maxFolds, testDataSet);
						
					}
				});
				
				results.add(result);
					
			
		}
		
		for(Future<RegressionModel> future : results)
		{
			RegressionModel classifierModel;
			try
			{
				classifierModel = future.get(15, TimeUnit.MINUTES);
								
				if(classifierModel.getRootMeanSqErr() < MAD)
				{
					MAD = classifierModel.getRootMeanSqErr();
					best = classifierModel;
					log.info(classifierModel.getTrainedClassifier().getClass().getSimpleName() + " current BEST FIT");
				}
			}
			catch (InterruptedException | ExecutionException | TimeoutException e)
			{
				log.warn(e);
				log.debug(e.getMessage(), e);
			}
					
		}
		threads.shutdownNow();
		
		return best;

	}

  public List<String> getAvailableClassifiers() {
    return availableClassifiers;
  }

}
