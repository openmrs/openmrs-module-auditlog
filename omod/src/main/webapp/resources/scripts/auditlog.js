var auditLogDetailsMap = new Object();
var dialogObj;
var childDialogObj;
var dialogWidth = 1140;
var dialogHeight = 640;
var childDialogWidth = dialogWidth*0.9;
var childDialogHeight = dialogHeight*0.9;

function auditlog_initTable(){
    $j('#'+auditlog_moduleId).dataTable({
        sPaginationType: "full_numbers",
        iDisplayLength: 15,
        bJQueryUI: true,
        bSort:false,
        sDom: '<fl>t<"ui-helper-clearfix"ip>',
        oLanguage: {
            "sInfo": omsgs.sInfoLabel,
            "oPaginate": {"sFirst": omsgs.first, "sPrevious": omsgs.previous, "sNext": omsgs.next, "sLast": omsgs.last},
            "sZeroRecords": omsgs.noMatchesFound,
            "sInfoEmpty": " ",
            "sLengthMenu": omsgs.showNumberofEntries
        },
        fnDrawCallback: function( oSettings ) {
            //remove jquery row striping so that we have our custom green for created, pink for deleted etc.
            $j('table#'+auditlog_moduleId+' tr.odd').removeClass('odd');
            $j('table#'+auditlog_moduleId+' tr.even').removeClass('even');
        }
    });

    //setup the dialog to display update changes for each row
    dialogObj = $j("#"+auditlog_moduleId+"-changes-dialog");
    dialogObj.dialog({
        autoOpen: false,
        width: dialogWidth+'px',
        height: dialogHeight,
        modal: true,
        beforeClose: function(event, ui){
            //reset
            $j("#"+auditlog_moduleId+"-changes-objectId").html("");
            $j("#"+auditlog_moduleId+"-changes-summary").html("");
            $j("#"+auditlog_moduleId+"-changes-objectUuid").html("");
            $j("#"+auditlog_moduleId+"-changes-openmrsVersion").html("");
            $j("#"+auditlog_moduleId+"-childLogCount").html("");
            //remove all rows from previous displays except the header rows
            $j("#"+auditlog_moduleId+"-changes-table tr:gt(0)").remove();
            $j("#"+auditlog_moduleId+"-delete-otherData-table tr:gt(0)").remove();
            $j("#"+auditlog_moduleId+"-childAuditLogDetails-table tr:gt(0)").remove();
            $j("#"+auditlog_moduleId+"-details .auditlog-changes-element").hide();
            $j("#"+auditlog_moduleId+"-details .auditlog-childAuditLogDetails-element").hide()
        }
    });

    childDialogObj = $j("#"+auditlog_moduleId+"-child-changes-dialog");
    childDialogObj.dialog({
        autoOpen: false,
        width: childDialogWidth+'px',
        height: childDialogHeight,
        modal: true,
        beforeClose: function(event, ui){
            //reset
            $j("#"+auditlog_moduleId+"-child-changes-objectId").html("");
            $j("#"+auditlog_moduleId+"-child-changes-summary").html("");
            $j("#"+auditlog_moduleId+"-child-changes-objectUuid").html("");
            $j("#"+auditlog_moduleId+"-child-changes-openmrsVersion").html("");
            $j("#"+auditlog_moduleId+"-child-childLogCount").html("");
            //remove all rows from previous displays except the header rows
            $j("#"+auditlog_moduleId+"-child-changes-table tr:gt(0)").remove();
            $j("#"+auditlog_moduleId+"-child-delete-otherData-table tr:gt(0)").remove();
            $j("#"+auditlog_moduleId+"-child-childAuditLogDetails-table tr:gt(0)").remove();
            $j("#"+auditlog_moduleId+"-child-details .auditlog-changes-element").hide();
            $j("#"+auditlog_moduleId+"-child-details .auditlog-childAuditLogDetails-element").hide()
        }
    });
}

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
    $j("#"+auditlog_moduleId+idPart+"-changes-table").hide();
    $j("#"+auditlog_moduleId+idPart+"-delete-otherData-table").hide();
    if(logDetails){
        if(logDetails.objectExists == true){
            $j("#"+auditlog_moduleId+idPart+"-changes-summary").html(logDetails.displayString);
        }
        else if(logDetails.action != 'DELETED'){
            $j("#"+auditlog_moduleId+idPart+"-changes-summary").html("<span class='auditlog_deleted'>"+auditlog_messages.objectDoesnotExit+"</span>");
        }

        if(logDetails.objectId)
            $j("#"+auditlog_moduleId+idPart+"-changes-objectId").html(logDetails.objectId);

        if(logDetails.objectUuid)
            $j("#"+auditlog_moduleId+idPart+"-changes-objectUuid").html(logDetails.objectUuid);

        if(logDetails.classname)
            $j("#"+auditlog_moduleId+idPart+"-changes-classname").html(logDetails.classname);

        if(logDetails.openmrsVersion)
            $j("#"+auditlog_moduleId+idPart+"-changes-openmrsVersion").html(logDetails.openmrsVersion);

        if(logDetails.changes){
            var auditLogChanges = logDetails.changes;
            var isUpdate = logDetails.action == 'UPDATED' ? true : false;
            var otherDataCount = 0;
            $j.each(auditLogChanges, function(propertyName){
                otherDataCount++;
                var currentProperty = auditLogChanges[propertyName];
                if(isUpdate){
                    $j("#"+auditlog_moduleId+idPart+"-changes-table tr:last").after(
                        "<tr>" +
                            "<td class=\"auditlog_align_text_left\" valign=\"top\">"+propertyName+"</td>"+
                            "<td class=\"auditlog_align_text_left\" valign=\"top\">"+currentProperty[0]+"</td>"+
                            "<td class=\"auditlog_align_text_left\" valign=\"top\">"+currentProperty[1]+"</td>" +
                            "</tr>");
                }else{
                    $j("#"+auditlog_moduleId+idPart+"-delete-otherData-table tr:last").after(
                        "<tr>" +
                            "<td class=\"auditlog_align_text_left\" valign=\"top\">"+propertyName+"</td>"+
                            "<td class=\"auditlog_align_text_left\" valign=\"top\">"+currentProperty+"</td>" +
                            "</tr>");
                }
            });

            if(otherDataCount > 0){
                var otherDataLabel = "";
                if(isUpdate){
                    otherDataLabel = auditlog_messages.changesLabel;
                    $j("#"+auditlog_moduleId+idPart+"-changes-table").show();

                }else{
                    otherDataLabel = auditlog_messages.lastKnownStateLabel;
                    $j("#"+auditlog_moduleId+idPart+"-delete-otherData-table").show();
                }

                $j("#"+auditlog_moduleId+idPart+"-otherDataLabel").html("<b>"+otherDataLabel+"</b>");
                $j("#"+auditlog_moduleId+idPart+"-details .auditlog-changes-element").show();
            }
        }

        if(logDetails.childAuditLogDetails){
            childAuditLogDetails = logDetails.childAuditLogDetails;
            $j("#"+auditlog_moduleId+idPart+"-childLogCount").html(childAuditLogDetails.length);
            $j.each(childAuditLogDetails, function(index, detail){
                $j("#"+auditlog_moduleId+idPart+"-childAuditLogDetails-table tr:last").after(
                    "<tr class=\"auditlog_"+detail.action+" auditlog_child_log\" onclick=\"auditlog_showDetails('"+detail.uuid+"', true)\">"+
                        "<td class=\"auditlog_align_text_left\" valign=\"top\">"+detail.classname+"</td></tr>");
            });
            $j("#"+auditlog_moduleId+idPart+"-details .auditlog-childAuditLogDetails-element").show();
        }

        if(isChildLog){
            childDialogObj.dialog('open');
        }else{
            dialogObj.dialog('open');
        }
    }
}