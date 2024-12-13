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

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import de.medizininformatik_initiative.process.report.ConstantsReport;
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.activity.AbstractServiceDelegate;
import dev.dsf.bpe.v1.variables.Variables;

public class CreateJson extends AbstractServiceDelegate
{

	private static final Logger logger = LoggerFactory.getLogger(CreateJson.class);

	private final String ddpUrl;
	private final String ddpUsername;
	private final String ddpPassword;
	private final boolean ddpApprovalRequired;
	private final long ddpTimeout;

	public CreateJson(ProcessPluginApi api, String ddpUrl, String ddpUsername, String ddpPassword,
			boolean ddpApprovalRequired, long ddpTimeout)
	{
		super(api);
		this.ddpUrl = ddpUrl;
		this.ddpUsername = ddpUsername;
		this.ddpPassword = ddpPassword;
		this.ddpApprovalRequired = ddpApprovalRequired;
		this.ddpTimeout = ddpTimeout;
	}

	@Override
	protected void doExecute(DelegateExecution delegateExecution, Variables variables) throws BpmnError, Exception
	{


		// Configure RestTemplate with timeout
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout((int) ddpTimeout);
		requestFactory.setReadTimeout((int) ddpTimeout);

		RestTemplate restTemplate = new RestTemplate(requestFactory);

		// Set up the headers, including the Authorization header for basic auth
		HttpHeaders headers = new HttpHeaders();
		String auth = ddpUsername + ":" + ddpPassword;
		String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
		String authHeader = "Basic " + encodedAuth;
		headers.set("Authorization", authHeader);

		// Create an HttpEntity with the headers
		HttpEntity<String> entity = new HttpEntity<>(headers);

		try
		{
			// Send the request using exchange to include headers and get the response
			ResponseEntity<String> response = restTemplate.exchange(ddpUrl, HttpMethod.GET, entity, String.class);
			// Print the response body
			// Save response
			variables.setString(ConstantsReport.BPMN_EXECUTION_VARIABLE_DASHBOARD_REPORT_DDP_JSON, response.getBody());
		}
		catch (ResourceAccessException e)
		{
			// Handle timeout
			logger.warn("The request to {} timed out after {} milliseconds", ddpUrl, ddpTimeout);
			throw new BpmnError(ConstantsReport.BPMN_EXECUTION_VARIABLE_DASHBOARD_REPORT_DDP_TIMEOUT_ERROR,
					"The request to " + ddpUrl + " timed out after " + ddpTimeout + " milliseconds");
		}

		// Set approval variable for next step
		variables.setBoolean(ConstantsReport.BPMN_EXECUTION_VARIABLE_DASHBOARD_REPORT_DDP_APPROVAL,
				ddpApprovalRequired);

	}
}
