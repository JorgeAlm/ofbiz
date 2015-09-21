package org.ofbiz.reports;

import java.util.ArrayList;
import java.util.List;

public class ReportResult {
	
	private Object Result;
	private List<ReportMessage> Messages;
	
	public ReportResult(){
		this.setMessages(new ArrayList<ReportMessage>());
	}
	
	public Boolean getSuccess() {
		for(ReportMessage message : Messages){
			if(message.getSeverity() == ReportMessageSeverity.Error){
				return false;
			}
		}
		
		return true;
	}
	public Object getResult() {
		return Result;
	}
	public void setResult(Object result) {
		Result = result;
	}
	public List<ReportMessage> getMessages() {
		return Messages;
	}
	public void setMessages(List<ReportMessage> messages) {
		Messages = messages;
	}
	public void addMessage(ReportMessage message){
		Messages.add(message);
	}
	public void addMessage(String message, ReportMessageSeverity severity){
		Messages.add(new ReportMessage(message, severity));
	}
	public void addAll(List<ReportMessage> messages){
		Messages.addAll(messages);
	}
}
