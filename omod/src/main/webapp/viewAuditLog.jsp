<%@ include file="/WEB-INF/template/include.jsp"%>

<%@ include file="/WEB-INF/template/header.jsp"%>

<%@ include file="/WEB-INF/view/module/auditlog/include.jsp"%>

<%@ include file="template/localHeader.jsp"%>

<openmrs:htmlInclude file="/scripts/jquery/dataTables/css/dataTables_jui.css"/>
<openmrs:htmlInclude file="/scripts/jquery/dataTables/js/jquery.dataTables.min.js"/>
<openmrs:htmlInclude file="/dwr/interface/DWRAuditLogService.js"/>

<script type="text/javascript">
	var auditLogDetailsMap = new Object();
	
	$j(document).ready(function() {
		$j('#${moduleId}').dataTable({
		    sPaginationType: "full_numbers",
		    iDisplayLength: 15,
		    bJQueryUI: true,
		    bSort:false,
		    //sDom: 'flt<"ui-helper-clearfix"ip>',
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
	    		$j('table#${moduleId} tr.odd').removeClass('odd');
	    		$j('table#${moduleId} tr.even').removeClass('even');
	        }
		});
		
		//set the dialog to display update changes for each row
		$j("#${moduleId}-changes-dialog").dialog({
			autoOpen: false,
			width:'1000',
			height:'450',
			modal: true,
			beforeClose: function(event, ui){
				//reset
				$j("#${moduleId}-changes-objectId").html("");
				$j("#${moduleId}-changes-summary").html("");
				$j("#${moduleId}-changes-objectUuid").html("");
				//remove all rows from previous displays except the header row
				$j("#${moduleId}-changes-table tr:gt(0)").remove();
				$j("#${moduleId}-details .${moduleId}-changes-element").hide();
			}
		});
	});
	
	function ${moduleId}_showDetails(auditLogUuid){
		existingLogDetails = auditLogDetailsMap[auditLogUuid];
		if(!existingLogDetails){
			DWRAuditLogService.getAuditLogDetails(auditLogUuid, function(detailsResponse){
				if(detailsResponse){
					displayLogDetails(detailsResponse);
					auditLogDetailsMap[auditLogUuid] = detailsResponse;
				}
			});
		}else{
			displayLogDetails(existingLogDetails);
		}
	}
	
	function displayLogDetails(logDetails){
		if(logDetails){
			if(logDetails.objectExists == true)
				$j("#${moduleId}-changes-summary").html(logDetails.displayString);
			else if(logDetails.action != 'DELETED')
				$j("#${moduleId}-changes-summary").html("<span style='color:red'><spring:message code="${moduleId}.objectDoesnotExist" /></span>");
			
			if(logDetails.objectId)
				$j("#${moduleId}-changes-objectId").html(logDetails.objectId);
			
			if(logDetails.objectUuid)
				$j("#${moduleId}-changes-objectUuid").html(logDetails.objectUuid);
			
			if(logDetails.classname)
				$j("#${moduleId}-changes-classname").html(logDetails.classname);
			
			if(logDetails.changes){
				auditLogChanges = logDetails.changes;
				$j.each(auditLogChanges, function(propertyName){
					currentChange = auditLogChanges[propertyName];
					$j("#${moduleId}-changes-table tr:last").after("<tr><td class=\"${moduleId}_align_text_left\" valign=\"top\">"+propertyName+"</td>"+
					"<td class=\"${moduleId}_align_text_left\" valign=\"top\">"+currentChange[0]+"</td>"+
					"<td class=\"${moduleId}_align_text_left\" valign=\"top\">"+currentChange[1]+"</td></tr>");
				});
				$j("#${moduleId}-details .${moduleId}-changes-element").show();
			}
		
			$j("#${moduleId}-changes-dialog").dialog('open');
		}
	}
	
</script>

<div class="box">
<b class="boxHeader" style="width: auto;"><spring:message code="${moduleId}.auditlogs" /></b>
<br />
<table id="${moduleId}" width="100%" cellpadding="3" cellspacing="0" align="left">
	<thead>
		<tr>
			<th class="ui-state-default"></th>
			<th class="ui-state-default">Item (<spring:message code="${moduleId}.numberOfChangedProperties" />)</th>
			<th class="ui-state-default">User (username)</th>
			<th class="ui-state-default">Date Of Occurence</th>
		</tr>
	</thead>
	<tbody>
	<c:forEach items="${auditLogs}" var="auditLog">
		<tr class="${moduleId}_${auditLog.action}" onclick="${moduleId}_showDetails('${auditLog.uuid}')">
			<td style="width: 17px !important;" align="center">
   				<img class="${moduleId}_action_image" align="top" 
   				 	src="<openmrs:contextPath />/moduleResources/${moduleId}/images/${auditLog.action}.gif" />
			</td>
   			<td>
   				${auditLog.simpleClassname}
   				<c:if test="${auditLog.action == 'UPDATED' && fn:length(auditLog.changes) > 0}"> (${fn:length(auditLog.changes)})</c:if>
   			</td>
   			<td>
   				<c:choose>
   					<%-- If this is a scheduled task, something done by daemon thread or at start up --%>
   					<c:when test="${auditLog.user == null || auditLog.user.uuid == 'A4F30A1B-5EB9-11DF-A648-37A07F9C90FB'}">
   						<spring:message code="${moduleId}.systemAction" />
   					</c:when>
   					<c:otherwise>
   						${auditLog.user.personName} <c:if test="${fn:trim(auditLog.user.username) != ''}">[${auditLog.user.username}]</c:if>
   					</c:otherwise>
   				</c:choose>
   			</td>
   			<td><openmrs:formatDate date="${auditLog.dateCreated}" type="long" /></td>
		</tr>
	</c:forEach>
	</tbody> 
</table>
</div>

<div id="${moduleId}-changes-dialog" class="${moduleId}_align_text_left" title="<spring:message code="${moduleId}.logDetails" />">
	<br />
	<table id="${moduleId}-details" width="100%" cellpadding="0" cellspacing="5">
		<tr>
			<th valign="top" class="${moduleId}_align_text_left" style="width:15%"><spring:message code="${moduleId}.uuid" /></th>
			<td id="${moduleId}-changes-objectUuid" width="100%"></td>
		</tr>
		<tr>
			<th valign="top" class="${moduleId}_align_text_left"><spring:message code="${moduleId}.id" /></th>
			<td id="${moduleId}-changes-objectId" width="100%"></td>
		</tr>
		<tr>
			<th valign="top" class="${moduleId}_align_text_left"><spring:message code="${moduleId}.classname" /></th>
			<td id="${moduleId}-changes-classname" width="100%"></td>
		</tr>
		<tr>
			<th valign="top" class="${moduleId}_align_text_left"><spring:message code="${moduleId}.summary" /></th>
			<td id="${moduleId}-changes-summary"></td>
		</tr>
		<tr class="${moduleId}-changes-element"><td colspan="2">&nbsp;</td></tr>
		<tr class="${moduleId}-changes-element"><td valign="top" colspan="2" class="${moduleId}_align_text_center"><b><spring:message code="${moduleId}.changes" /></b></td></tr><tr>
		<tr class="${moduleId}-changes-element">
			<td valign="top" colspan="2" class="${moduleId}_align_text_left">
				<table id="${moduleId}-changes-table" width="100%" cellpadding="3" cellspacing="0" border="1" bordercolor="#ADACAC">
					<thead>
						<tr>
							<th class="${moduleId}_table_header ${moduleId}_align_text_center" width="26%">
								<spring:message code="${moduleId}.propertyName" />
							</th>
							<th class="${moduleId}_table_header ${moduleId}_align_text_center" width="37%">
								<spring:message code="${moduleId}.newValue" />
							</th>
							<th class="${moduleId}_table_header ${moduleId}_align_text_center" width="37%">
								<spring:message code="${moduleId}.previousValue" />
							</th>
						</tr>
					<thead>
				</table>
			</td>
		</tr>
	</table>
</div>

<%@ include file="/WEB-INF/template/footer.jsp"%>
