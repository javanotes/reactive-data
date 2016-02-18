package com.reactivetechnologies.analytics;

import com.reactivetechnologies.analytics.core.Dataset;

public interface IncrementalModelEngine
{
  /**
   * Load from saved state if present
   * @return
   */
  boolean loadAndInitializeModel();
  
  /**
   * Update and train model
   * @param nextInstance
   * @throws Exception
   */
	void incrementModel(Dataset nextInstance) throws Exception;
	
}
