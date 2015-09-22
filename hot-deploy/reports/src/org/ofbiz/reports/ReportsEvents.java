package org.ofbiz.reports;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.calendar.RecurrenceRule;

public class ReportsEvents {
	public static final String module = ReportsEvents.class.getName();

	public static String GenerateReport(HttpServletRequest request,
			HttpServletResponse response) throws ParserConfigurationException,
			TransformerException, GeneralException {

		HttpSession session = request.getSession();
        GenericValue userLogin = (GenericValue)session.getAttribute("userLogin");
        LocalDispatcher dispatcher = (LocalDispatcher)request.getAttribute("dispatcher");
        Locale locale = UtilHttp.getLocale(request);
        TimeZone timeZone = UtilHttp.getTimeZone(request);
        Delegator delegator = dispatcher.getDelegator();
        Object customTimePeriodIdParameter = request.getParameter("timePeriod");
        
        if(UtilValidate.isEmpty(customTimePeriodIdParameter)){
			return "redirectToForm";
		}
        
		String customTimePeriodId = customTimePeriodIdParameter.toString();
		String taxAuthGeoId = request.getParameter("taxAuthGeoId").toString();
		String postalAddressPurposeTypeId = request.getParameter("postalAddressPurposeTypeId").toString();
		String phonePurposeTypeId = request.getParameter("phonePurposeTypeId").toString();
		String faxPurposeTypeId = request.getParameter("faxPurposeTypeId").toString();
		String emailPurposeTypeId = request.getParameter("emailPurposeTypeId").toString();
		String websitePurposeTypeId = request.getParameter("websitePurposeTypeId").toString();

		Map<String, Object> mapIn = FastMap.newInstance();
		mapIn.put("timePeriod", customTimePeriodId);
		mapIn.put("taxAuthGeoId", taxAuthGeoId);
		mapIn.put("postalAddressPurposeTypeId", postalAddressPurposeTypeId);
		mapIn.put("phonePurposeTypeId", phonePurposeTypeId);
		mapIn.put("faxPurposeTypeId", faxPurposeTypeId);
		mapIn.put("emailPurposeTypeId", emailPurposeTypeId);
		mapIn.put("websitePurposeTypeId", websitePurposeTypeId);
		
		mapIn.put("locale", locale);
        mapIn.put("userLogin", userLogin);
        mapIn.put("timeZone", timeZone);
		
		UUID uniqueId = UUID.randomUUID();
		String jobName = "SAFT-"+uniqueId.toString();
		Timestamp original = UtilDateTime.nowTimestamp();
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(original.getTime());
        cal.add(Calendar.SECOND, 10);
        Timestamp later = new Timestamp(cal.getTime().getTime());
        mapIn.put("jobName", jobName);
		
        dispatcher.schedule(jobName, "pool", "generateSaft", mapIn, later.getTime(), RecurrenceRule.HOURLY, 1, 1, 0, 2);
        
        EntityListIterator eli = delegator.find("JobSandbox", EntityCondition.makeCondition(UtilMisc.toMap("jobName", jobName)), null, UtilMisc.toSet("jobId"), null, null);
		GenericValue job = eli.next();
		request.setAttribute("jobId", job.getString("jobId"));

		return "success";
	}
	
	public static String DownloadReport(HttpServletRequest request,
			HttpServletResponse response) throws ParserConfigurationException,
			TransformerException, GeneralException {
		LocalDispatcher dispatcher = (LocalDispatcher)request.getAttribute("dispatcher");
		Delegator delegator = dispatcher.getDelegator();
		
		String jobId = request.getParameter("jobId").toString();
		String xmlString = "";
		
		GenericValue job = delegator.findOne("JobSandbox", UtilMisc.toMap("jobId", jobId), false);
		GenericValue jobResult = delegator.getRelated("JobSandboxResult", null, null, job, false).get(0);
		
		xmlString = jobResult.getString("jobResult");
		
		// set the Xml content type
		response.setContentType("application/xml");
		response.setHeader("Content-Disposition",
				"attachment; filename=\"Report.xml\"");
		// jsonStr.length is not reliable for unicode characters
		try {
			response.setContentLength(xmlString.getBytes("UTF8").length);
		} catch (UnsupportedEncodingException e) {
			Debug.logError("Problems with XML encoding: " + e, module);
		}

		// return the JSON String
		ServletOutputStream out;
		try {
			out = response.getOutputStream();
			out.write(xmlString.getBytes());
			out.flush();
		} catch (IOException e) {
			Debug.logError(e, module);
		}
		
		return "success";
	}
}
