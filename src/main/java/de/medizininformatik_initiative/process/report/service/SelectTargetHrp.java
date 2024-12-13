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
import java.util.Optional;
import java.util.function.Supplier;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.medizininformatik_initiative.process.report.ConstantsReport;
import de.medizininformatik_initiative.processes.common.util.ConstantsBase;
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.activity.AbstractServiceDelegate;
import dev.dsf.bpe.v1.constants.NamingSystems;
import dev.dsf.bpe.v1.variables.Target;
import dev.dsf.bpe.v1.variables.Variables;

public class SelectTargetHrp extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(SelectTargetHrp.class);

	private final String hrpIdentifierEnvVariable;

	public SelectTargetHrp(ProcessPluginApi api, String hrpIdentifierEnvVariable)
	{
		super(api);
		this.hrpIdentifierEnvVariable = hrpIdentifierEnvVariable;
	}

	@Override
	protected void doExecute(DelegateExecution execution, Variables variables)
	{
		Task startTask = variables.getStartTask();

		Identifier parentIdentifier = NamingSystems.OrganizationIdentifier.withValue(
				ConstantsReport.NAMINGSYSTEM_DSF_ORGANIZATION_IDENTIFIER_NETWORK_OF_UNIVERSITY_MEDICINE_CONSORTIUM);
		Coding hrpRole = new Coding().setSystem(ConstantsBase.CODESYSTEM_DSF_ORGANIZATION_ROLE)
				.setCode(ConstantsBase.CODESYSTEM_DSF_ORGANIZATION_ROLE_VALUE_HRP);

		// 1. use hrp-identifier provided from task, if not present
		// 2. use hrp-identifier provided from ENV variable, if not present
		// 3. search hrp-identifier for mii-parent-organization and use first found
		String hrpIdentifier = extractHrpIdentifierFromTask(startTask)
				.or(extractHrpIdentifierFromEnv(hrpIdentifierEnvVariable))
				.orElse(searchHrpIdentifier(parentIdentifier, hrpRole, startTask));

		Endpoint endpoint = getHrpEndpoint(parentIdentifier, hrpIdentifier, hrpRole);
		String endpointIdentifier = extractEndpointIdentifier(endpoint);

		Target target = variables.createTarget(hrpIdentifier, endpointIdentifier, endpoint.getAddress());
		variables.setTarget(target);
	}

	private Optional<String> extractHrpIdentifierFromTask(Task task)
	{
		Optional<String> hrpIdentifier = api.getTaskHelper()
				.getFirstInputParameterValue(task, ConstantsReport.CODESYSTEM_REPORT,
						ConstantsReport.CODESYSTEM_REPORT_VALUE_HRP_IDENTIFIER, Reference.class)
				.filter(Reference::hasIdentifier).map(Reference::getIdentifier)
				.filter(i -> NamingSystems.OrganizationIdentifier.SID.equals(i.getSystem())).map(Identifier::getValue);

		hrpIdentifier.ifPresent(
				hrp -> logger.info("Using HRP '{}' from Task with id '{}' as report target", hrp, task.getId()));

		return hrpIdentifier;
	}

	private Supplier<Optional<String>> extractHrpIdentifierFromEnv(String hrpIdentifierEnvVariable)
	{
		return () ->
		{
			if (hrpIdentifierEnvVariable != null)
			{
				logger.info("Using HRP '{}' from ENV variable as report target", hrpIdentifierEnvVariable);
				return Optional.of(hrpIdentifierEnvVariable);
			}
			else
				return Optional.empty();
		};
	}

	private String searchHrpIdentifier(Identifier parentIdentifier, Coding hrpRole, Task task)
	{
		logger.debug(
				"HRP not defined in Task with id '{}' or ENV variable - searching HRP for mii-consortium as report target",
				task.getId());

		Organization organization = getHrpOrganization(parentIdentifier, hrpRole);
		return extractHrpIdentifierFromOrganization(organization);
	}

	private Organization getHrpOrganization(Identifier parentIdentifier, Coding role)
	{
		List<Organization> hrps = api.getOrganizationProvider().getOrganizations(parentIdentifier, role);

		if (hrps.size() < 1)
			throw new RuntimeException("Could not find any organization with role '" + role.getCode()
					+ "' and parent organization '" + parentIdentifier.getValue() + "'");

		if (hrps.size() > 1)
			logger.warn(
					"Found more than 1 ({}) organization with role '{}' and parent organization '{}', using the first ('{}')",
					hrps.size(), role.getCode(), parentIdentifier.getValue(),
					hrps.get(0).getIdentifierFirstRep().getValue());

		return hrps.get(0);
	}

	private String extractHrpIdentifierFromOrganization(Organization organization)
	{
		return NamingSystems.OrganizationIdentifier.findFirst(organization)
				.orElseThrow(() -> new RuntimeException("organization with id '" + organization.getId()
						+ "' is missing identifier with system '" + NamingSystems.OrganizationIdentifier.SID + "'"))
				.getValue();
	}

	private Endpoint getHrpEndpoint(Identifier parentIdentifier, String organizationIdentifierValue, Coding role)
	{


		Identifier organizationIdentifier = NamingSystems.OrganizationIdentifier.withValue(organizationIdentifierValue);
		return api.getEndpointProvider().getEndpoint(parentIdentifier, organizationIdentifier, role)
				.orElseThrow(() -> new RuntimeException("Could not find any endpoint of '" + role.getCode()
						+ "' with identifier '" + organizationIdentifier.getValue() + "'"));
	}

	private String extractEndpointIdentifier(Endpoint endpoint)
	{
		return endpoint.getIdentifier().stream().filter(i -> NamingSystems.EndpointIdentifier.SID.equals(i.getSystem()))
				.map(Identifier::getValue).findFirst()
				.orElseThrow(() -> new RuntimeException("Endpoint with id '" + endpoint.getId()
						+ "' is missing identifier with system '" + NamingSystems.EndpointIdentifier.SID + "'"));
	}
}
