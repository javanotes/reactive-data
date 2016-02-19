package com.reactivetechnologies.analytics;

import java.util.List;

import com.reactivetechnologies.analytics.core.ClassifiedModel;
import com.reactivetechnologies.analytics.core.CombinerType;
import com.reactivetechnologies.analytics.core.Dataset;
import com.reactivetechnologies.analytics.core.RegressionModel;

public interface RegressionModelEngine extends IncrementalModelEngine
{
  
	/**
	 * Get current model
	 * @return
	 */
	RegressionModel generateModelSnapshot();
	/**
	 * Choose a best fit using a preset evaluation sample based on the {@linkplain CombinerType},
   * for the generated snapshot models
	 * @param models
	 * @param combiner
	 * @param evaluationSet
	 * @return
	 * @throws EngineException
	 */
	RegressionModel findBestFitModel(List<RegressionModel> models, CombinerType combiner, Dataset evaluationSet) throws EngineException;
	/**
	 * Run model to get a classification. Can use a BootstrapAGGregatING technique to vote
	 * @param unclassified
	 * @param bagging
	 * @return
	 * @throws EngineException
	 */
  ClassifiedModel classify(Dataset unclassified, boolean bagging) throws EngineException;
	
}
