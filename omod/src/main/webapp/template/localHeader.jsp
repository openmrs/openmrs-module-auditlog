<ul id="menu">
	<li class="first">
		<a href="${pageContext.request.contextPath}/admin"><spring:message code="admin.title.short" /></a>
	</li>

	<li <c:if test='<%= request.getRequestURI().contains("/settings") %>'>class="active"</c:if>>
		<a href="${pageContext.request.contextPath}/module/${moduleId}/settings.form">
			<spring:message	code="${moduleId}.manage.settings" />
		</a>
	</li>
	
	<%-- Add further links here --%>
</ul>

<h2><spring:message code="${moduleId}.title" /></h2>