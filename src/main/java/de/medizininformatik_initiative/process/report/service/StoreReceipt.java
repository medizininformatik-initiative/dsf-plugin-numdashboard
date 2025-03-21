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

package de.medizininformatik_initiative.process.report.service;

import java.util.Objects;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.medizininformatik_initiative.process.report.ConstantsReport;
import de.medizininformatik_initiative.process.report.util.ReportStatusGenerator;
import de.medizininformatik_initiative.processes.common.util.ConstantsBase;
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.activity.AbstractServiceDelegate;
import dev.dsf.bpe.v1.variables.Target;
import dev.dsf.bpe.v1.variables.Variables;

public class StoreReceipt extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(StoreReceipt.class);

	private final ReportStatusGenerator statusGenerator;

	public StoreReceipt(ProcessPluginApi api, ReportStatusGenerator statusGenerator)
	{
		super(api);
		this.statusGenerator = statusGenerator;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();
		Objects.requireNonNull(statusGenerator, "statusGenerator");
	}

	@Override
	protected void doExecute(DelegateExecution execution, Variables variables)
	{
		String reportLocation = variables
				.getString(ConstantsReport.BPMN_EXECUTION_VARIABLE_DASHBOARD_REPORT_DDP_JSON_RESPONSE_REFERENCE);

		Task startTask = variables.getStartTask();
		Task currentTask = variables.getLatestTask();
		Target target = variables.getTarget();

		if (!currentTask.getId().equals(startTask.getId()))
			handleReceivedResponse(startTask, currentTask);
		else
			handleMissingResponse(startTask);

		writeStatusLogAndSendMail(startTask, reportLocation, target.getOrganizationIdentifierValue());

		variables.updateTask(startTask);

		if (Task.TaskStatus.FAILED.equals(startTask.getStatus()))
		{
			api.getFhirWebserviceClientProvider().getLocalWebserviceClient()
					.withRetry(ConstantsBase.DSF_CLIENT_RETRY_6_TIMES, ConstantsBase.DSF_CLIENT_RETRY_INTERVAL_5MIN)
					.update(startTask);
		}
	}

	private void handleReceivedResponse(Task startTask, Task currentTask)
	{
		statusGenerator.transformInputToOutput(currentTask, startTask);

		if (startTask.getOutput().stream().filter(Task.TaskOutputComponent::hasExtension)
				.flatMap(o -> o.getExtension().stream())
				.anyMatch(e -> ConstantsReport.EXTENSION_REPORT_STATUS_ERROR_URL.equals(e.getUrl())))
			startTask.setStatus(Task.TaskStatus.FAILED);
	}

	private void handleMissingResponse(Task startTask)
	{
		startTask.setStatus(Task.TaskStatus.FAILED);
		startTask.addOutput(statusGenerator
				.createReportStatusOutput(ConstantsReport.CODESYSTEM_REPORT_STATUS_VALUE_RECEIPT_MISSING));
	}

	private void writeStatusLogAndSendMail(Task startTask, String reportLocation, String hrpIdentifier)
	{
		startTask.getOutput().stream().filter(o -> o.getValue() instanceof Coding)
				.filter(o -> ConstantsReport.CODESYSTEM_REPORT_STATUS.equals(((Coding) o.getValue()).getSystem()))
				.forEach(o -> doWriteStatusLogAndSendMail(o, startTask.getId(), reportLocation, hrpIdentifier));
	}

	private void doWriteStatusLogAndSendMail(Task.TaskOutputComponent output, String startTaskId, String reportLocation,
			String hrpIdentifier)
	{
		Coding status = (Coding) output.getValue();
		String code = status.getCode();
		String error = output.hasExtension() ? output.getExtensionFirstRep().getValueAsPrimitive().getValueAsString()
				: "none";
		String errorLog = error.isBlank() ? "" : " - " + error;

		if (ConstantsReport.CODESYSTEM_REPORT_STATUS_VALUE_RECEIPT_OK.equals(code))
		{
			logger.info("Task with id '{}' has report-status code '{}' for HRP '{}'", startTaskId, code, hrpIdentifier);
			sendSuccessfulMail(reportLocation, code, hrpIdentifier);
		}
		else
		{
			logger.warn("Task with id '{}' has report-status code '{}'{} for HRP '{}'", startTaskId, code, errorLog,
					hrpIdentifier);
			sendErrorMail(startTaskId, reportLocation, code, error, hrpIdentifier);
		}
	}

	private void sendSuccessfulMail(String reportLocation, String code, String hrpIdentifier)
	{
		String subject = "New successful report in process '" + ConstantsReport.PROCESS_NAME_FULL_REPORT_SEND + "'";
		String message = "A new report has been successfully created and retrieved by the HRP '" + hrpIdentifier
				+ "' with status code '" + code + "' in process '" + ConstantsReport.PROCESS_NAME_FULL_REPORT_SEND
				+ "'.";

		api.getMailService().send(subject, message);
	}

	private void sendErrorMail(String startTaskId, String reportLocation, String code, String error,
			String hrpIdentifier)
	{
		String subject = "Error in process '" + ConstantsReport.PROCESS_NAME_FULL_REPORT_SEND + "'";

		String message = "HRP '" + hrpIdentifier + "' could not download or insert new report with reference '"
				+ reportLocation + "' in process '" + ConstantsReport.PROCESS_NAME_FULL_REPORT_SEND
				+ "' in Task with id '" + startTaskId + "':\n" + "- status code: " + code + "\n" + "- error: " + error;

		api.getMailService().send(subject, message);
	}
}
