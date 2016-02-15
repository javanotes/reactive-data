package com.reactivetechnologies.analytics;

import java.util.List;

import com.reactivetechnologies.analytics.core.RegressionModel;
import com.reactivetechnologies.analytics.core.TrainModel;

public interface Regression
{
  /**
   * 
   * @param nextInstance
   * @throws Exception
   */
	void incrementModel(TrainModel nextInstance) throws Exception;
	/**
	 * 
	 * @return
	 */
	RegressionModel generateModelSnapshot();
	/**
	 * 
	 * @param models
	 * @return
	 */
	RegressionModel ensembleModels(List<RegressionModel> models);
}
