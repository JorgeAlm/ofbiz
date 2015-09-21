package org.ofbiz.reports;

import java.io.Serializable;

@SuppressWarnings("serial")
public class ReportMessage implements Serializable {
	
	private String Message;
	private ReportMessageSeverity Severity;
	
	public ReportMessage(){
	}
	
	public ReportMessage(String message, ReportMessageSeverity severity){
		setMessage(message);
		setSeverity(severity);
	}
	
	public String getMessage(){
		return Message;
	}
	public void setMessage(String message){
		Message = message;
	}
	public ReportMessageSeverity getSeverity(){
		return Severity;
	}
	public void setSeverity(ReportMessageSeverity severity){
		Severity = severity;
	}
}
