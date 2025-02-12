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

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import de.medizininformatik_initiative.process.report.ConstantsReport;
import de.medizininformatik_initiative.process.report.util.ReportBackend;
import de.medizininformatik_initiative.process.report.util.ReportStatusGenerator;
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.activity.AbstractServiceDelegate;
import dev.dsf.bpe.v1.variables.Variables;

public class InsertReport extends AbstractServiceDelegate
{

	private static final Logger logger = LoggerFactory.getLogger(InsertReport.class);

	private final ReportStatusGenerator statusGenerator;
	private final ReportBackend reportBackend;

	public InsertReport(ProcessPluginApi api, ReportStatusGenerator statusGenerator, ReportBackend reportBackend)
	{
		super(api);
		this.statusGenerator = statusGenerator;
		this.reportBackend = reportBackend;
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
		String sendingOrganization = task.getRequester().getIdentifier().getValue();

		Binary report = variables.getResource(ConstantsReport.BPMN_EXECUTION_VARIABLE_DASHBOARD_REPORT_DDP_BINARY);

		// api.getReadAccessHelper().addLocal(report);
		// api.getReadAccessHelper().addOrganization(report, task.getRequester().getIdentifier().getValue());

		try
		{
			String organization = sendingOrganization.replaceAll("\\.", "");
			sendDataToDashboard(report, organization);

			task.addOutput(statusGenerator
					.createReportStatusOutput(ConstantsReport.CODESYSTEM_REPORT_STATUS_VALUE_RECEIVE_OK));
			variables.updateTask(task);
		}
		catch (Exception exception)
		{
			task.setStatus(Task.TaskStatus.FAILED);
			task.addOutput(statusGenerator.createReportStatusOutput(
					ConstantsReport.CODESYSTEM_REPORT_STATUS_VALUE_RECEIVE_ERROR, "Insert report failed"));
			variables.updateTask(task);

			variables.setString(ConstantsReport.BPMN_EXECUTION_VARIABLE_REPORT_RECEIVE_ERROR_MESSAGE,
					"Insert report failed");

			logger.warn("Storing report from organization '{}' for Task with id '{}' failed - {}", sendingOrganization,
					task.getId(), exception.getMessage());
			throw new BpmnError(ConstantsReport.BPMN_EXECUTION_VARIABLE_REPORT_RECEIVE_ERROR,
					"Insert report - " + exception.getMessage());
		}
	}

	protected String sendDataToDashboard(Binary report, String organization)
	{
		byte[] decodedData = report.getData();
		String decodedString = new String(decodedData);

		// Create an instance of RestTemplate
		RestTemplate restTemplate = new RestTemplate();
		// Set up the headers, including the Authorization header for basic auth
		HttpHeaders headers = new HttpHeaders();
		// String auth = this.DASHBOARD_BACKEND_USER + ":" + this.DASHBOARD_BACKEND_PASSWORD;
		// String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
		// String authHeader = "Basic " + encodedAuth;
		// headers.set("Authorization", authHeader);
		headers.setContentType(MediaType.APPLICATION_JSON); // Set content type as JSON

		// Create an HttpEntity with the headers and request body
		HttpEntity<String> entity = new HttpEntity<>(decodedString, headers);
		// Send the request using exchange to include headers and request body, and get the response
		ResponseEntity<String> response = restTemplate.exchange(
				reportBackend.getActiveURL() + "/internal/" + organization + "/put", HttpMethod.PUT, entity,
				String.class);

		return response.getBody();
	}
}
