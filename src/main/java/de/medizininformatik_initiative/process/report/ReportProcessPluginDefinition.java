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

package de.medizininformatik_initiative.process.report;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import de.medizininformatik_initiative.process.report.spring.config.ReportConfig;
import dev.dsf.bpe.v1.ProcessPluginDefinition;

public class ReportProcessPluginDefinition implements ProcessPluginDefinition
{
	public static final String VERSION = "1.0.0.0";
	public static final String NAME = "num-process-dashboard-report";
	public static final LocalDate RELEASE_DATE = LocalDate.of(2024, 12, 13);

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getVersion()
	{
		return VERSION;
	}

	@Override
	public LocalDate getReleaseDate()
	{
		return RELEASE_DATE;
	}

	@Override
	public List<String> getProcessModels()
	{
		return List.of("bpe/report-autostart.bpmn", "bpe/report-send.bpmn", "bpe/report-receive.bpmn");
	}

	@Override
	public List<Class<?>> getSpringConfigurations()
	{
		return List.of(ReportConfig.class);
	}

	public Map<String, List<String>> getFhirResourcesByProcessId()
	{
		var aAutostart = "fhir/ActivityDefinition/report-autostart.xml";
		var aReceive = "fhir/ActivityDefinition/report-receive.xml";
		var aSend = "fhir/ActivityDefinition/report-send.xml";

		var cReport = "fhir/CodeSystem/report.xml";
		var cReportStatus = "fhir/CodeSystem/report-status.xml";

		var eReportStatusError = "fhir/StructureDefinition/extension-report-status-error.xml";

		var sAutostartStart = "fhir/StructureDefinition/task-report-autostart-start.xml";
		var sAutostartStop = "fhir/StructureDefinition/task-report-autostart-stop.xml";
		var sReceive = "fhir/StructureDefinition/task-report-receive.xml";
		var sSend = "fhir/StructureDefinition/task-report-send.xml";
		var sSendStart = "fhir/StructureDefinition/task-report-send-start.xml";

		var tAutostartStart = "fhir/Task/task-report-autostart-start.xml";
		var tAutostartStop = "fhir/Task/task-report-autostart-stop.xml";
		var tSendStart = "fhir/Task/task-report-send-start.xml";

		var vReport = "fhir/ValueSet/report.xml";
		var vReportStatusReceive = "fhir/ValueSet/report-status-receive.xml";
		var vReportStatusSend = "fhir/ValueSet/report-status-send.xml";

		var qApproveDashboardBinary = "fhir/Questionnaire/approve-dashboard-binary.xml";

		return Map.of(ConstantsReport.PROCESS_NAME_FULL_REPORT_AUTOSTART,
				List.of(aAutostart, cReport, sAutostartStart, sAutostartStop, tAutostartStart, tAutostartStop, vReport),
				ConstantsReport.PROCESS_NAME_FULL_REPORT_RECEIVE,
				List.of(aReceive, cReport, cReportStatus, eReportStatusError, sSend, vReport, vReportStatusReceive),
				ConstantsReport.PROCESS_NAME_FULL_REPORT_SEND,
				List.of(aSend, cReport, cReportStatus, eReportStatusError, sReceive, sSendStart, tSendStart, vReport,
						vReportStatusSend, qApproveDashboardBinary));
	}
}
