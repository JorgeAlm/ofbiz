import java.util.*;
import java.lang.*;
import org.ofbiz.entity.*;
import org.ofbiz.entity.condition.*;
import org.ofbiz.base.util.*;
import org.ofbiz.base.util.UtilMisc;

context.reportQueue = delegator.findOne("ReportQueue", false, UtilMisc.toMap("reportQueueId", parameters.reportQueueId));
context.reports = context.reportQueue.getRelated("Report", null, null, false);

context.report = null;
context.reportValidationMessages = null;

if(context.reports != null && context.reports.size() > 0){
	context.report = context.reports.get(0);
	context.reportValidationMessages = context.report.getRelated("ReportValidationMsgs", null, null, false);
}