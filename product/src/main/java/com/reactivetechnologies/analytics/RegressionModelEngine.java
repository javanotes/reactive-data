package com.reactivetechnologies.analytics;

import java.util.List;

import com.reactivetechnologies.analytics.core.ClassifiedModel;
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
	 * Combine generated models (ensembling/voting/stacking or anything else)
	 * @param models
	 * @return
	 */
	RegressionModel combineModels(List<RegressionModel> models) throws EngineException;
	/**
   * Run model to get a classification
   * @param instance
   * @return
   */
  ClassifiedModel classify(Dataset unclassified) throws EngineException;
	
}
