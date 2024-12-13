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

import java.util.List;
import java.util.Objects;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.medizininformatik_initiative.process.report.ConstantsReport;
import de.medizininformatik_initiative.process.report.util.ReportStatusGenerator;
import de.medizininformatik_initiative.processes.common.util.ConstantsBase;
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.activity.AbstractServiceDelegate;
import dev.dsf.bpe.v1.variables.Variables;
import dev.dsf.fhir.client.BasicFhirWebserviceClient;

public class DownloadReport extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(DownloadReport.class);

	private final ReportStatusGenerator statusGenerator;

	public DownloadReport(ProcessPluginApi api, ReportStatusGenerator statusGenerator)
	{
		super(api);
		this.statusGenerator = statusGenerator;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();
		Objects.requireNonNull(statusGenerator, "reportStatusGenerator");
	}

	@Override
	protected void doExecute(DelegateExecution execution, Variables variables)
	{
		Task task = variables.getStartTask();
		IdType reportReference = getReportReference(task);

		variables.setString(ConstantsReport.BPMN_EXECUTION_VARIABLE_DASHBOARD_REPORT_DDP_JSON_RESPONSE_REFERENCE,
				reportReference.getValue());

		logger.info("Downloading report with id '{}' from organization '{}' referenced in Task with id '{}'",
				reportReference.getValue(), task.getRequester().getIdentifier().getValue(), task.getId());

		try
		{
			Binary reportBinary = downloadReportBinary(reportReference);
			variables.setResource(ConstantsReport.BPMN_EXECUTION_VARIABLE_DASHBOARD_REPORT_DDP_BINARY, reportBinary);
		}
		catch (Exception exception)
		{
			task.setStatus(Task.TaskStatus.FAILED);
			task.addOutput(statusGenerator.createReportStatusOutput(
					ConstantsReport.CODESYSTEM_REPORT_STATUS_VALUE_RECEIVE_ERROR, "Download report failed"));
			variables.updateTask(task);

			variables.setString(ConstantsReport.BPMN_EXECUTION_VARIABLE_REPORT_RECEIVE_ERROR_MESSAGE,
					"Download report failed");

			logger.warn(
					"Downloading report with id '{}' from organization '{}' referenced in Task with id '{}' failed - {}",
					reportReference.getValue(), task.getRequester().getIdentifier().getValue(), task.getId(),
					exception.getMessage());
			throw new BpmnError(ConstantsReport.BPMN_EXECUTION_VARIABLE_REPORT_RECEIVE_ERROR,
					"Download report - " + exception.getMessage());
		}
	}

	private IdType getReportReference(Task task)
	{
		List<String> reportReferences = api.getTaskHelper()
				.getInputParameterValues(task, ConstantsReport.CODESYSTEM_REPORT,
						ConstantsReport.CODESYSTEM_REPORT_VALUE_SEARCH_BUNDLE_RESPONSE_REFERENCE, Reference.class)
				.filter(Reference::hasReference).map(Reference::getReference).toList();

		if (reportReferences.size() < 1)
			throw new IllegalArgumentException("No report reference present in Task with id '" + task.getId() + "'");

		if (reportReferences.size() > 1)
			logger.warn("Found {} report references in task with id '{}', using only the first",
					reportReferences.size(), task.getId());

		return new IdType(reportReferences.get(0));
	}

	private Binary downloadReportBinary(IdType reportReference)
	{
		BasicFhirWebserviceClient client = api.getFhirWebserviceClientProvider()
				.getWebserviceClient(reportReference.getBaseUrl())
				.withRetry(ConstantsBase.DSF_CLIENT_RETRY_6_TIMES, ConstantsBase.DSF_CLIENT_RETRY_INTERVAL_5MIN);

		if (reportReference.hasVersionIdPart())
			return client.read(Binary.class, reportReference.getIdPart(), reportReference.getVersionIdPart());
		else
			return client.read(Binary.class, reportReference.getIdPart());
	}
}
