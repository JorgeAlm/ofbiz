var timerRef = null;

function StartJobStatusUpdateTimer(placeHolderId, statusPlaceHolderId, jobId) {
	timerRef = window.setInterval(function(){ getJobStatus(placeHolderId, statusPlaceHolderId, jobId); }, 5000);
}

function getJobStatus(placeHolderId, statusPlaceHolderId, jobId){
	jQuery.ajax({
        url: 'getJobStatusIdJson',
        async: false,
        type: 'POST', // If changed to GET will result in security error from OFBiz.
        data: { jobId: jobId },
        success: function(data) {
        	jQuery('#' + statusPlaceHolderId).html(data.jobStatusId);
        	
        	if (data.jobStatusId !== 'SERVICE_PENDING' && data.jobStatusId !== 'SERVICE_RUNNING' && data.jobStatusId !== 'SERVICE_QUEUED') {
        		StopJobStatusUpdateTimer();
        		getDownloadDisplay(placeHolderId, jobId);
        	}
        }
    });
}

function getDownloadDisplay(placeHolderId, jobId) {
	jQuery.ajax({
        url: 'getJobResultDisplay',
        async: false,
        type: 'GET',
        data: { jobId: jobId },
        success: function(data) {
            jQuery('#' + placeHolderId).html(data);
        }
    });
}

function StopJobStatusUpdateTimer() {
	window.clearInterval(timerRef);
}