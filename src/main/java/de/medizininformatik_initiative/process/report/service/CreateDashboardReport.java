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
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.medizininformatik_initiative.process.report.ConstantsReport;
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.activity.AbstractServiceDelegate;
import dev.dsf.bpe.v1.variables.Target;
import dev.dsf.bpe.v1.variables.Variables;
import jakarta.ws.rs.core.MediaType;

public class CreateDashboardReport extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(CreateDashboardReport.class);

	private final String resourceVersion;

	public CreateDashboardReport(ProcessPluginApi api, String resourceVersion)
	{
		super(api);
		this.resourceVersion = resourceVersion;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();
		Objects.requireNonNull(resourceVersion, "resourceVersion");
	}

	@Override
	protected void doExecute(DelegateExecution execution, Variables variables)
	{
		Task task = variables.getStartTask();
		Target target = variables.getTarget();

		String ddpJson = variables.getString(ConstantsReport.BPMN_EXECUTION_VARIABLE_DASHBOARD_REPORT_DDP_JSON);
		variables.setString(ConstantsReport.BPMN_EXECUTION_VARIABLE_DASHBOARD_REPORT_DDP_JSON, "");
		try
		{
			boolean approvalRequired = variables
					.getBoolean(ConstantsReport.BPMN_EXECUTION_VARIABLE_DASHBOARD_REPORT_DDP_APPROVAL);
			String ref = storeBinary(ddpJson.getBytes(),
					approvalRequired ? api.getOrganizationProvider().getLocalOrganizationIdentifierValue().get()
							: target.getOrganizationIdentifierValue());
			variables.setString(ConstantsReport.BPMN_EXECUTION_VARIABLE_DASHBOARD_REPORT_DDP_JSON_RESPONSE_REFERENCE,
					ref);
		}
		catch (Exception exception)
		{
			logger.warn("Could not create report for HRP '{}' in Task with id '{}' - {}",
					target.getOrganizationIdentifierValue(), task.getId(), exception.getMessage());
			throw new RuntimeException("Could not create report for HRP '" + target.getOrganizationIdentifierValue()
					+ "' in Task with id '" + task.getId() + "' - " + exception.getMessage(), exception);
		}
	}

	private String storeBinary(byte[] content, String organizationIdentifier)
	{

		try
		{
			Binary binary = new Binary();
			binary.setContentType(MediaType.TEXT_PLAIN);
			binary.setData(content);
			api.getReadAccessHelper().addLocal(binary);
			if (organizationIdentifier != null)
			{
				api.getReadAccessHelper().addOrganization(binary, organizationIdentifier);
			}
			IdType created = api.getFhirWebserviceClientProvider().getLocalWebserviceClient().withMinimalReturn()
					.create(binary);
			return new IdType(api.getFhirWebserviceClientProvider().getLocalWebserviceClient().getBaseUrl(),
					ResourceType.Binary.name(), created.getIdPart(), created.getVersionIdPart()).getValue();
		}
		catch (Exception exception)
		{
			logger.warn("Could not create binary - {}", exception.getMessage());
			throw new RuntimeException("Could not create binary - " + exception.getMessage(), exception);
		}
	}

}
