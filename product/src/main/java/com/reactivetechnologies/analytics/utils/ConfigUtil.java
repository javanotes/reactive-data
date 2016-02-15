package com.reactivetechnologies.analytics.utils;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

public class ConfigUtil
{
  public static final String WEKA_IN_MAP = "WEKAEVENT";
  public static final String WEKA_MODEL_CACHE_MAP = "WEKAMODELCACHE";
  public static final String WEKA_MODEL_SNAPSHOT_SET = "WEKAMODELSNAP";
  public static final String WEKA_MODEL_PERSIST_MAP = "WEKAMODELENS";
  public static final String WEKA_COMMUNICATION_TOPIC = "WEKAINTERCOMM";
  public static final String WEKA_IN_BEAN_NAME = "Weka-Inbound";
	/**
	 * Formats a xml string
	 * @param input
	 * @param indent
	 * @return
	 */
	public static String prettyFormatXml(String input, int indent)
	{
		try
		{
			Source xmlInput = new StreamSource(new StringReader(input));
			StringWriter stringWriter = new StringWriter();
			StreamResult xmlOutput = new StreamResult(stringWriter);
			TransformerFactory transformerFactory = TransformerFactory
					.newInstance();
			transformerFactory.setAttribute("indent-number", indent);
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.transform(xmlInput, xmlOutput);
			
			return xmlOutput.getWriter().toString();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return input;
	}

}
