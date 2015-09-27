package org.ofbiz.reports;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReportResult {
	
	private Object result;
	private List<ReportMessage> messages;
	
	public ReportResult(){
		this.setMessages(new ArrayList<ReportMessage>());
	}
	
	public Boolean getSuccess() {
		for(ReportMessage message : messages){
			if(message.getSeverity() == ReportMessageSeverity.Error){
				return false;
			}
		}
		
		return true;
	}
	public Object getResult() {
		return this.result;
	}
	public void setResult(Object result) {
		this.result = result;
	}
	public List<ReportMessage> getMessages() {
		return this.messages;
	}
	public void setMessages(List<ReportMessage> messages) {
		this.messages = messages;
	}
	public void addMessage(ReportMessage message){
		this.messages.add(message);
	}
	public void addMessage(String message, Map<String, String> messageParameters, ReportMessageSeverity severity){
		this.messages.add(new ReportMessage(message, messageParameters, severity));
	}
	public void addAll(List<ReportMessage> messages){
		this.messages.addAll(messages);
	}
}
