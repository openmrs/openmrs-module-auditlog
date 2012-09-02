<%@ include file="/WEB-INF/template/include.jsp"%>

<%@ include file="/WEB-INF/template/header.jsp"%>

<%@ include file="/WEB-INF/view/module/auditlog/include.jsp"%>

<%@ include file="template/localHeader.jsp"%>

<openmrs:htmlInclude file="/scripts/jquery/dataTables/css/dataTables_jui.css"/>
<openmrs:htmlInclude file="/scripts/jquery/dataTables/js/jquery.dataTables.min.js"/>

<script type="text/javascript">
	var auditLogChangesMap = new Object();
	
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
			width:'900',
			height:'350',
			modal: true,
			beforeClose: function(event, ui){
				//remove all rows from previous displays except the header row
				$j("#${moduleId}-changes-table tr:gt(0)").remove();
			}
		});
	});
	
	function ${moduleId}_showChanges(auditLogUuid){
		text = "";
		auditLogChanges = auditLogChangesMap[auditLogUuid];
		changesTemp = $j.each(auditLogChanges, function(index){
			currentChange = auditLogChanges[index];
			$j("#${moduleId}-changes-table tr:last").after("<tr><td class=\"${moduleId}_align_text_left\" valign=\"top\">"+currentChange.propertyName+"</td>"+
			"<td class=\"${moduleId}_align_text_left\" valign=\"top\">"+currentChange.newValue+"</td>"+
			"<td class=\"${moduleId}_align_text_left\" valign=\"top\">"+currentChange.previousValue+"</td></tr>");
		});
		
		$j("#${moduleId}-changes-dialog").dialog('open');
	}
	
</script>

<div class="box">
<b class="boxHeader" style="width: auto;"><spring:message code="${moduleId}.auditlogs" /></b>
<br />
<table id="${moduleId}" width="100%" cellpadding="3" cellspacing="0" align="left">
	<thead>
		<tr>
			<th class="ui-state-default">Item (<spring:message code="${moduleId}.numberOfChanges" />)</th>
			<th class="ui-state-default">Uuid</th>
			<th class="ui-state-default">User</th>
			<th class="ui-state-default">Date Of Occurence</th>
		</tr>
	</thead>
	<tbody>
	<c:forEach items="${auditLogs}" var="auditLog">
		<c:if test="${auditLog.action == 'UPDATED' && fn:length(auditLog.changes) > 0}">		
		<script type="text/javascript">
		var changes${auditLog.auditLogId} = new Array();
		var change;
		<c:forEach items="${auditLog.changes}" var="entry">
			change = new Object();
			change.propertyName = "${entry.key}";
			change.newValue = "${entry.value[0]}";
			change.previousValue = "${entry.value[1]}";
			changes${auditLog.auditLogId}.push(change);
		</c:forEach>
		auditLogChangesMap['${auditLog.uuid}'] = changes${auditLog.auditLogId};
		</script>
		</c:if>
		<tr class="${moduleId}_${auditLog.action}"
		   <c:if test="${auditLog.action == 'UPDATED' && fn:length(auditLog.changes) > 0}">onclick="${moduleId}_showChanges('${auditLog.uuid}')"</c:if>>
   			<td>
   				${auditLog.className} 
   				<c:if test="${auditLog.action == 'UPDATED' && fn:length(auditLog.changes) > 0}"> (${fn:length(auditLog.changes)})</c:if>
   			</td>
   			<td>${auditLog.objectUuid}</td>
   			<td>${auditLog.user.personName}</td>
   			<td><openmrs:formatDate date="${auditLog.dateCreated}" type="long" /></td>
		</tr>
	</c:forEach>
	</tbody> 
</table>
</div>

<div id="${moduleId}-changes-dialog" title="<spring:message code="${moduleId}.changes" />" 
	style="b;">
	<table id="${moduleId}-changes-table" width="100%" cellpadding="3" cellspacing="0" border="1" bordercolor="#DDDDDD">
		<thead>
			<tr>
				<th class="${moduleId}_table_header ${moduleId}_align_text_center" width="26%"><spring:message code="${moduleId}.propertyName" /></th>
				<th class="${moduleId}_table_header ${moduleId}_align_text_center" width="37%"><spring:message code="${moduleId}.newValue" /></th>
				<th class="${moduleId}_table_header ${moduleId}_align_text_center" width="37%"><spring:message code="${moduleId}.previousValue" /></th>
			</tr>
		<thead>
	</table>
</div>

<%@ include file="/WEB-INF/template/footer.jsp"%>
