var timerRef = null;

function StartReportStatusUpdateTimer(placeHolderId, statusPlaceHolderId, queueId) {
	timerRef = window.setInterval(function(){ getReportStatus(placeHolderId, statusPlaceHolderId, queueId); }, 5000);
}

function getReportStatus(placeHolderId, statusPlaceHolderId, queueId){
	jQuery.ajax({
        url: 'getReportStatusIdJson',
        async: false,
        type: 'POST', // If changed to GET will result in security error from OFBiz.
        data: { reportQueueId: queueId },
        success: function(data) {
        	jQuery('#' + statusPlaceHolderId).html(data.reportQueueStatusIdDisplay);
        	
        	if (data.reportQueueStatusId !== 'PENDING' && data.reportQueueStatusId !== 'RUNNING' && data.reportQueueStatusId !== 'QUEUED') {
        		StopReportStatusUpdateTimer();
        		getDownloadDisplay(placeHolderId, queueId);
        	}
        }
    });
}

function getDownloadDisplay(placeHolderId, queueId) {
	jQuery.ajax({
        url: 'getReportResultDisplay',
        async: false,
        type: 'GET',
        data: { reportQueueId: queueId },
        success: function(data) {
            jQuery('#' + placeHolderId).html(data);
        }
    });
}

function StopReportStatusUpdateTimer() {
	window.clearInterval(timerRef);
}