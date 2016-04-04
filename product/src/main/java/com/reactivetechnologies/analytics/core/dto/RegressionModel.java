package com.reactivetechnologies.analytics.core.dto;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.DataSerializable;
import com.hazelcast.util.HashUtil;
import com.hazelcast.util.MD5Util;
import com.reactivetechnologies.analytics.OperationFailedUnexpectedly;

import weka.classifiers.Classifier;
import weka.core.Instances;
import weka.core.xml.XStream;


public class RegressionModel implements DataSerializable,Serializable
{
	/**
   * 
   */
  private static final long serialVersionUID = -7364621200107984642L;

  private long generatedOn;
  
	public RegressionModel(){}
	
	public RegressionModel(Classifier trainedClassifier) {
    super();
    this.trainedClassifier = trainedClassifier;
  }
  public RegressionModel(String trainedClassifier) throws IOException {
    super();
    Assert.notNull(trainedClassifier, "Serialized classifier is null");
    deserializeClassifierFromJson(trainedClassifier);
  }
  public Classifier getTrainedClassifier()
	{
		return trainedClassifier;
	}

	private String classifierImpl;
	public void setTrainedClassifier(Classifier trainedClassifier)
	{
		this.trainedClassifier = trainedClassifier;
		classifierImpl = trainedClassifier.getClass().getName();
	}
	private long murmurHash;
	private String md5Hex;

	
	public Instances getTrainingSet()
	{
		return trainingSet;
	}

	public void setTrainingSet(Instances trainingSet)
	{
		this.trainingSet = trainingSet;
	}

	
	/* (non-Javadoc)
   * @see com.ericsson.fmt.forecasting.engine.impl.RegressionModel#getName()
   */
	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public double [] getResults()
	{
		return results;
	}

	public void setResults(double [] results)
	{
		this.results = results;
	}

	/* (non-Javadoc)
   * @see com.ericsson.fmt.forecasting.engine.impl.RegressionModel#writeClassifierAsXml()
   */
	public String serializeClassifierAsJson() throws IOException
	{
	  try {
      return XStream.serialize(trainedClassifier);
    } catch (Exception e) {
      if(e instanceof IOException)
        throw (IOException)e;
      else
        throw new OperationFailedUnexpectedly(e);
    }
	}
	/* (non-Javadoc)
   * @see com.ericsson.fmt.forecasting.engine.impl.RegressionModel#readClassifierAsXml(java.lang.String)
   */
	public Classifier deserializeClassifierFromJson(String xmlString) throws IOException
  {
    try 
    {
      if (!StringUtils.isEmpty(classifierImpl)) {
        Class.forName(classifierImpl);
        trainedClassifier = (Classifier) XStream.deSerialize(xmlString);
      }
      
    } catch (Exception e) {
      throw new IOException(e);
    }
    return trainedClassifier;
  }
	
	//private double iterationError;
  private Classifier  trainedClassifier;
  //private double    meanAbsErr;
  //private double    rootMeanSqErr;
  private transient Instances trainingSet;
  private String name;
  private transient double [] results;
  //private double pctIncorrect;
  //private int folds = 0;
  
  @Override
  public void writeData(ObjectDataOutput out) throws IOException {
    try {
      //out.writeDouble(getIterationError());
      //out.writeDouble(getMeanAbsErr());
      //out.writeDouble(getRootMeanSqErr());
      //out.writeDouble(getPctIncorrect());
      //out.writeInt(getFolds());
      out.writeUTF(getName());
      out.writeUTF(classifierImpl);
      out.writeUTF(serializeClassifierAsJson());
      out.writeLong(murmurHash);
      out.writeUTF(md5Hex);
      out.writeLong(getGeneratedOn());
    } catch (Exception e) {
      throw new IOException(e);
    }
    
  }

  @Override
  public void readData(ObjectDataInput in) throws IOException {
    
    try 
    {
      //setIterationError(in.readDouble());
      //setMeanAbsErr(in.readDouble());
      //setRootMeanSqErr(in.readDouble());
      //setPctIncorrect(in.readDouble());
      //setFolds(in.readInt());
      setName(in.readUTF());
      classifierImpl = in.readUTF();
      deserializeClassifierFromJson(in.readUTF());
      setLongId(in.readLong());
      setStringId(in.readUTF());
      setGeneratedOn(in.readLong());
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  /**
   * Generates different ids, by creating a murmur hash on the serialized string
   * form of the classifier.
   */
  public void generateId()
  {
    byte[] bytes = null;
    try 
    {
      bytes = serializeClassifierAsJson().getBytes(StandardCharsets.UTF_8);
      murmurHash = HashUtil.MurmurHash3_x64_64(bytes, 0, bytes.length);
      md5Hex = MD5Util.toMD5String(getTrainedClassifier().toString()+murmurHash);
    } catch (Exception e) {
      e.printStackTrace();
      murmurHash = -1;
    }
  }
  public void setLongId(Long id)
  {
    murmurHash = id;
  }
  public Long getLongId() {
    return murmurHash;
  }
  public String getStringId() {
    return md5Hex;
  }
  public void setStringId(String id) {
    md5Hex = id;
  }

  public long getGeneratedOn() {
    return generatedOn;
  }

  public void setGeneratedOn(long generatedOn) {
    this.generatedOn = generatedOn;
  }
}
