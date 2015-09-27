package org.ofbiz.reports;

import java.io.Serializable;
import java.util.Map;

@SuppressWarnings("serial")
public class ReportMessage implements Serializable {
	
	private String messageMapName;
	private Map<String, String> messageParameters;
	private ReportMessageSeverity severity;
	
	public ReportMessage(){
	}
	
	public ReportMessage(String messageMapName, Map<String, String> messageParameters, ReportMessageSeverity severity){
		this.setMessageMapName(messageMapName);
		this.setMessageParameters(messageParameters);
		this.setSeverity(severity);
	}
	
	public String getMessageMapName(){
		return this.messageMapName;
	}
	public void setMessageMapName(String messageMapName){
		this.messageMapName = messageMapName;
	}
	public Map<String, ?> getMessageParameters(){
		return this.messageParameters;
	}
	public void setMessageParameters(Map<String, String> messageParameters){
		this.messageParameters = messageParameters;
	}
	public ReportMessageSeverity getSeverity(){
		return this.severity;
	}
	public void setSeverity(ReportMessageSeverity severity){
		this.severity = severity;
	}
}
