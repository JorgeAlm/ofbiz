import java.util.*;
import java.lang.*;
import org.ofbiz.entity.*;
import org.ofbiz.entity.condition.*;
import org.ofbiz.base.util.*;
import org.ofbiz.base.util.UtilMisc;

context.job = delegator.findOne("JobSandbox", false, UtilMisc.toMap("jobId", parameters.jobId));
context.jobResults = context.job.getRelated("JobSandboxResult", null, null, false);
context.jobResult = null;
context.jobResultMessages = null;

if(context.jobResults != null && context.jobResults.size() > 0){
	context.jobResult = context.jobResults.get(0);
	context.jobResultMessages = context.jobResult.getRelated("JobSandboxResultMsg", null, null, false);
}