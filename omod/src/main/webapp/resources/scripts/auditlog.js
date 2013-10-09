var auditLogDetailsMap = new Object();
var changesLabel = '<spring:message code="${moduleId}.changes" />';
var lastKnownStateLabel = '<spring:message code="${moduleId}.lastKnownState" />';

function auditlog_showDetails(auditLogUuid, isChildLog){
    var existingLogDetails = auditLogDetailsMap[auditLogUuid];
    if(!existingLogDetails){
        DWRAuditLogService.getAuditLogDetails(auditLogUuid, function(detailsResponse){
            if(detailsResponse){
                displayLogDetails(detailsResponse, isChildLog);
                auditLogDetailsMap[auditLogUuid] = detailsResponse;
            }
        });
    }else{
        displayLogDetails(existingLogDetails, isChildLog);
    }
}

function displayLogDetails(logDetails, isChildLog){
    var idPart = (isChildLog) ? "-child" : "";
    $j("auditlog"+idPart+"-changes-table").hide();
    $j("auditlog"+idPart+"-delete-otherData-table").hide();
    if(logDetails){
        if(logDetails.objectExists == true){
            $j("auditlog"+idPart+"-changes-summary").html(logDetails.displayString);
        }
        else if(logDetails.action != 'DELETED'){
            $j("auditlog"+idPart+"-changes-summary").html("<span class='${moduleid}_deleted'><spring:message code="${moduleId}.objectDoesnotExist" /></span>");
        }

        if(logDetails.objectId)
            $j("auditlog"+idPart+"-changes-objectId").html(logDetails.objectId);

        if(logDetails.objectUuid)
            $j("auditlog"+idPart+"-changes-objectUuid").html(logDetails.objectUuid);

        if(logDetails.classname)
            $j("auditlog"+idPart+"-changes-classname").html(logDetails.classname);

        if(logDetails.openmrsVersion)
            $j("auditlog"+idPart+"-changes-openmrsVersion").html(logDetails.openmrsVersion);

        if(logDetails.changes){
            var auditLogChanges = logDetails.changes;
            var isUpdate = logDetails.action == 'UPDATED' ? true : false;
            var otherDataCount = 0;
            $j.each(auditLogChanges, function(propertyName){
                otherDataCount++;
                var currentProperty = auditLogChanges[propertyName];
                if(isUpdate){
                    $j("auditlog"+idPart+"-changes-table tr:last").after(
                        "<tr>" +
                            "<td class=\"${moduleId}_align_text_left\" valign=\"top\">"+propertyName+"</td>"+
                            "<td class=\"${moduleId}_align_text_left\" valign=\"top\">"+currentProperty[0]+"</td>"+
                            "<td class=\"${moduleId}_align_text_left\" valign=\"top\">"+currentProperty[1]+"</td>" +
                            "</tr>");
                }else{
                    $j("auditlog"+idPart+"-delete-otherData-table tr:last").after(
                        "<tr>" +
                            "<td class=\"${moduleId}_align_text_left\" valign=\"top\">"+propertyName+"</td>"+
                            "<td class=\"${moduleId}_align_text_left\" valign=\"top\">"+currentProperty+"</td>" +
                            "</tr>");
                }
            });

            if(otherDataCount > 0){
                var otherDataLabel = "";
                if(isUpdate){
                    otherDataLabel = changesLabel;
                    $j("auditlog"+idPart+"-changes-table").show();

                }else{
                    otherDataLabel = lastKnownStateLabel;
                    $j("auditlog"+idPart+"-delete-otherData-table").show();
                }

                $j("auditlog"+idPart+"-otherDataLabel").html("<b>"+otherDataLabel+"</b>");
                $j("auditlog"+idPart+"-details .${moduleId}-changes-element").show();
            }
        }

        if(logDetails.childAuditLogDetails){
            childAuditLogDetails = logDetails.childAuditLogDetails;
            $j("auditlog"+idPart+"-childLogCount").html(childAuditLogDetails.length);
            $j.each(childAuditLogDetails, function(index, detail){
                $j("auditlog"+idPart+"-childAuditLogDetails-table tr:last").after(
                    "<tr class=\"${moduleId}_"+detail.action+" ${moduleId}_child_log\" onclick=\"auditlog_showDetails('"+detail.uuid+"', true)\">"+
                        "<td class=\"${moduleId}_align_text_left\" valign=\"top\">"+detail.classname+"</td></tr>");
            });
            $j("auditlog"+idPart+"-details .${moduleId}-childAuditLogDetails-element").show();
        }

        if(isChildLog){
            childDialogObj.dialog('open');
        }else{
            dialogObj.dialog('open');
        }
    }
}