#!/bin/bash

rm ../openmrs-core/webapp/src/main/webapp/WEB-INF/view/module/auditlog/*.jsp
cp omod/src/main/webapp/*.jsp ../openmrs-core/webapp/src/main/webapp/WEB-INF/view/module/auditlog

echo Done reloading GSPs
