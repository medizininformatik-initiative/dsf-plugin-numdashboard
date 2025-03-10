/*
 * Copyright (C) 2021 University Hospital Bonn - All Rights Reserved You may use, distribute and
 * modify this code under the GPL 3 license. THERE IS NO WARRANTY FOR THE PROGRAM, TO THE EXTENT
 * PERMITTED BY APPLICABLE LAW. EXCEPT WHEN OTHERWISE STATED IN WRITING THE COPYRIGHT HOLDERS AND/OR
 * OTHER PARTIES PROVIDE THE PROGRAM “AS IS” WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR
 * IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE. THE ENTIRE RISK AS TO THE QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH
 * YOU. SHOULD THE PROGRAM PROVE DEFECTIVE, YOU ASSUME THE COST OF ALL NECESSARY SERVICING, REPAIR
 * OR CORRECTION. IN NO EVENT UNLESS REQUIRED BY APPLICABLE LAW OR AGREED TO IN WRITING WILL ANY
 * COPYRIGHT HOLDER, OR ANY OTHER PARTY WHO MODIFIES AND/OR CONVEYS THE PROGRAM AS PERMITTED ABOVE,
 * BE LIABLE TO YOU FOR DAMAGES, INCLUDING ANY GENERAL, SPECIAL, INCIDENTAL OR CONSEQUENTIAL DAMAGES
 * ARISING OUT OF THE USE OR INABILITY TO USE THE PROGRAM (INCLUDING BUT NOT LIMITED TO LOSS OF DATA
 * OR DATA BEING RENDERED INACCURATE OR LOSSES SUSTAINED BY YOU OR THIRD PARTIES OR A FAILURE OF THE
 * PROGRAM TO OPERATE WITH ANY OTHER PROGRAMS), EVEN IF SUCH HOLDER OR OTHER PARTY HAS BEEN ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGES. You should have received a copy of the GPL 3 license with *
 * this file. If not, visit http://www.gnu.de/documents/gpl-3.0.en.html
 */

package de.medizininformatik_initiative.process.report.message;

import java.util.Objects;
import java.util.stream.Stream;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.r4.model.*;

import de.medizininformatik_initiative.process.report.ConstantsReport;
import de.medizininformatik_initiative.process.report.util.ReportBackend;
import de.medizininformatik_initiative.process.report.util.ReportStatusGenerator;
import de.medizininformatik_initiative.processes.common.util.ConstantsBase;
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.activity.AbstractTaskMessageSend;
import dev.dsf.bpe.v1.variables.Variables;
import dev.dsf.fhir.client.FhirWebserviceClient;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

public class SendReport extends AbstractTaskMessageSend
{
	private final ReportStatusGenerator statusGenerator;
	private final ReportBackend reportBackend;

	public SendReport(ProcessPluginApi api, ReportStatusGenerator statusGenerator, ReportBackend reportBackend)
	{
		super(api);
		this.statusGenerator = statusGenerator;
		this.reportBackend = reportBackend;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();
		Objects.requireNonNull(statusGenerator, "statusGenerator");
	}

	@Override
	protected Stream<Task.ParameterComponent> getAdditionalInputParameters(DelegateExecution execution,
			Variables variables)
	{
		String ddpBinaryUrl = variables
				.getString(ConstantsReport.BPMN_EXECUTION_VARIABLE_DASHBOARD_REPORT_DDP_JSON_RESPONSE_REFERENCE);
		// String ddpBackendType = variables
		// .getString(ConstantsReport.CODESYSTEM_BACKEND_TYPE_VALUE_TYPE);
		String ddpBackendType = variables.getString(reportBackend.getType());
		// update this binary resource if approval was required to allow access
		if (variables.getBoolean(ConstantsReport.BPMN_EXECUTION_VARIABLE_DASHBOARD_REPORT_DDP_APPROVAL))
		{
			IdType binaryId = new IdType(ddpBinaryUrl);
			String id = binaryId.getIdPart();
			Binary binary = api.getFhirWebserviceClientProvider().getLocalWebserviceClient().read(Binary.class, id);
			api.getReadAccessHelper().addOrganization(binary, variables.getTarget().getOrganizationIdentifierValue());
			IdType updated = api.getFhirWebserviceClientProvider().getLocalWebserviceClient().withMinimalReturn()
					.update(binary);
			ddpBinaryUrl = new IdType(api.getFhirWebserviceClientProvider().getLocalWebserviceClient().getBaseUrl(),
					ResourceType.Binary.name(), updated.getIdPart(), updated.getVersionIdPart()).getValue();
		}

		Task.ParameterComponent parameterComponent = new Task.ParameterComponent();
		parameterComponent.getType().addCoding().setSystem(ConstantsReport.CODESYSTEM_REPORT)
				.setCode(ConstantsReport.CODESYSTEM_REPORT_VALUE_SEARCH_BUNDLE_RESPONSE_REFERENCE);
		parameterComponent.setValue(new Reference(ddpBinaryUrl).setType(ResourceType.Binary.name()));

		Task.ParameterComponent parameterComponent2 = new Task.ParameterComponent();
		parameterComponent2.getType().addCoding().setSystem(ConstantsReport.CODESYSTEM_BACKEND_TYPE)
				.setCode(reportBackend.getActiveURL(ddpBackendType));

		return Stream.of(parameterComponent, parameterComponent2);
	}

	@Override
	protected IdType doSend(FhirWebserviceClient client, Task task)
	{
		return client.withMinimalReturn()
				.withRetry(ConstantsBase.DSF_CLIENT_RETRY_6_TIMES, ConstantsBase.DSF_CLIENT_RETRY_INTERVAL_5MIN)
				.create(task);
	}

	@Override
	protected void handleIntermediateThrowEventError(DelegateExecution execution, Variables variables,
			Exception exception, String errorMessage)
	{
		Task task = variables.getStartTask();

		if (task != null)
		{
			String statusCode = ConstantsReport.CODESYSTEM_REPORT_STATUS_VALUE_NOT_REACHABLE;
			if (exception instanceof WebApplicationException webApplicationException
					&& webApplicationException.getResponse() != null
					&& webApplicationException.getResponse().getStatus() == Response.Status.FORBIDDEN.getStatusCode())
			{
				statusCode = ConstantsReport.CODESYSTEM_REPORT_STATUS_VALUE_NOT_ALLOWED;
			}

			task.addOutput(statusGenerator.createReportStatusOutput(statusCode, "Send report failed"));
			variables.updateTask(task);
		}

		super.handleIntermediateThrowEventError(execution, variables, exception, errorMessage);
	}

	@Override
	protected void addErrorMessage(Task task, String errorMessage)
	{
		// Override in order not to add error message of AbstractTaskMessageSend
	}
}
