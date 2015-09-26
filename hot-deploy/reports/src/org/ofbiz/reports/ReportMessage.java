package org.ofbiz.reports;

import java.io.Serializable;
import java.util.Map;

@SuppressWarnings("serial")
public class ReportMessage implements Serializable {
	
	private String MessageMapName;
	private Map<String, String> MessageParameters;
	private ReportMessageSeverity Severity;
	
	public ReportMessage(){
	}
	
	public ReportMessage(String messageMapName, Map<String, String> messageParameters, ReportMessageSeverity severity){
		setMessageMapName(messageMapName);
		setMessageParameters(messageParameters);
		setSeverity(severity);
	}
	
	public String getMessageMapName(){
		return MessageMapName;
	}
	public void setMessageMapName(String messageMapName){
		MessageMapName = messageMapName;
	}
	public Map<String, ?> getMessageParameters(){
		return MessageParameters;
	}
	public void setMessageParameters(Map<String, String> messageParameters){
		MessageParameters = messageParameters;
	}
	public ReportMessageSeverity getSeverity(){
		return Severity;
	}
	public void setSeverity(ReportMessageSeverity severity){
		Severity = severity;
	}
}
