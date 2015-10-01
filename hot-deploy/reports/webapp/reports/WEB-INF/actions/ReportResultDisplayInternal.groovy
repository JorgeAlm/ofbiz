import java.util.*;
import java.lang.*;
import org.ofbiz.entity.*;
import org.ofbiz.entity.condition.*;
import org.ofbiz.base.util.*;
import org.ofbiz.base.util.UtilMisc;

context.report = null;
context.reportValidationMessages = null;
context.reportQueue = null;

if (parameters.reportQueueId != null){
	context.reportQueue = delegator.findOne("ReportQueue", false, UtilMisc.toMap("reportQueueId", parameters.reportQueueId));
	if(context.reportQueue != null)
	{
		reports = context.reportQueue.getRelated("Report", null, null, false);
		
		if(reports != null && reports.size() > 0){
			context.report = reports.get(0);
		}
	}
	
} else {
	context.report = delegator.findOne("Report", false, UtilMisc.toMap("reportId", parameters.reportId));
	if(context.report != null) 
	{
		reportQueues = context.report.getRelated("ReportQueue", null, null, false);
		
		if(reportQueues != null && reportQueues.size() > 0){
			context.reportQueue = reportQueues.get(0);
		}
	}
}

if(context.report != null){
	context.reportValidationMessages = context.report.getRelated("ReportValidationMsgs", null, null, false);
}