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

package de.medizininformatik_initiative.process.report.spring.config;

import java.time.Duration;
import java.time.format.DateTimeParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import de.medizininformatik_initiative.process.report.ConstantsReport;
import de.medizininformatik_initiative.process.report.ReportProcessPluginDefinition;
import de.medizininformatik_initiative.process.report.listener.ApproveDashboardBinaryListener;
import de.medizininformatik_initiative.process.report.message.SendReceipt;
import de.medizininformatik_initiative.process.report.message.SendReport;
import de.medizininformatik_initiative.process.report.message.StartSendReport;
import de.medizininformatik_initiative.process.report.service.*;
import de.medizininformatik_initiative.process.report.util.ReportBackend;
import de.medizininformatik_initiative.process.report.util.ReportStatusGenerator;
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.documentation.ProcessDocumentation;

@Configuration
public class ReportConfig
{
	private static final Logger logger = LoggerFactory.getLogger(ReportConfig.class);

	@Autowired
	private ProcessPluginApi api;

	@ProcessDocumentation(processNames = {
			ConstantsReport.PROCESS_NAME_FULL_REPORT_SEND }, description = "The identifier of the HRP which should receive the report", recommendation = "Only configure if more than one HRP exists in your network", example = "netzwerk-universitaetsmedizin.de")
	@Value("${de.netzwerk.universitaetsmedizin.dashboard.report.ddp.dic.hrp.identifier:#{null}}")
	private String hrpIdentifier;
	@ProcessDocumentation(processNames = {
			ConstantsReport.PROCESS_NAME_FULL_REPORT_SEND }, description = "The identifier of the HRP which should receive the report", recommendation = "Only configure if more than one HRP exists in your network", example = "netzwerk-universitaetsmedizin.de")
	@Value("${de.netzwerk.universitaetsmedizin.dashboard.report.ddp.url:#{null}}")
	private String ddpUrl;
	@Value("${de.netzwerk.universitaetsmedizin.dashboard.report.ddp.user:#{null}}")
	private String ddpUsername;
	@Value("${de.netzwerk.universitaetsmedizin.dashboard.report.ddp.password:#{null}}")
	private String ddpPassword;
	@Value("${de.netzwerk.universitaetsmedizin.dashboard.report.ddp.approval:false}")
	private String ddpApproval;
	@Value("${de.netzwerk.universitaetsmedizin.dashboard.report.ddp.timeout:PT4H}")
	private String ddpTimeout;
	@Value("${de.netzwerk.universitaetsmedizin.dashboard.report.backend:TEST}")
	private String dashboardBackend;
	@Value("${de.netzwerk.universitaetsmedizin.dashboard.report.backend.url.produktiv:https://numdashboard.ukbonn.de}")
	private String dashboardBackendUrlProduktiv;
	@Value("${de.netzwerk.universitaetsmedizin.dashboard.report.backend.url.test:https://numdashboard-test.ukbonn.de}")
	private String dashboardBackendUrlTest;
	@Value("${de.netzwerk.universitaetsmedizin.dashboard.report.backend.url.development:https://numdashboard-dev.ukbonn.de}")
	private String dashboardBackendUrlDevelopment;

	// all Processes

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public ReportStatusGenerator reportStatusGenerator()
	{
		return new ReportStatusGenerator();
	}

	// reportAutostart Process

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public SetTimer setTimer()
	{
		return new SetTimer(api);
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public StartSendReport startSendReport()
	{
		return new StartSendReport(api);
	}

	// reportSend Process

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public SelectTargetHrp selectTargetHrp()
	{
		return new SelectTargetHrp(api, hrpIdentifier);
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public CreateDashboardReport createDashboardReport()
	{
		String resourceVersion = new ReportProcessPluginDefinition().getResourceVersion();
		return new CreateDashboardReport(api, resourceVersion);
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public SendReport sendReport()
	{
		return new SendReport(api, reportStatusGenerator());
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public StoreReceipt storeReceipt()
	{
		return new StoreReceipt(api, reportStatusGenerator());
	}

	// reportReceive Process

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public DownloadReport downloadReport()
	{
		return new DownloadReport(api, reportStatusGenerator());
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public InsertReport insertReport()
	{
		ReportBackend reportBackend = new ReportBackend(dashboardBackendUrlProduktiv, dashboardBackendUrlTest,
				dashboardBackendUrlDevelopment, dashboardBackend);
		return new InsertReport(api, reportStatusGenerator(), reportBackend);
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public HandleError handleError()
	{
		return new HandleError(api);
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public CreateJson createJson()
	{
		// Parse the timeout duration
		long timeoutMillis = 100000L; // Default to 100 seconds in case parsing fails
		try
		{
			Duration duration = Duration.parse(ddpTimeout);
			timeoutMillis = duration.toMillis();
		}
		catch (DateTimeParseException e)
		{
			logger.warn("Could not parse Timeout - {}", e.getMessage());
		}
		boolean approvalRequired = !(ddpApproval.equals("false") || ddpApproval.equals("0"));
		return new CreateJson(api, ddpUrl, ddpUsername, ddpPassword, approvalRequired, timeoutMillis);
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public SelectTargetDic selectTargetDic()
	{
		return new SelectTargetDic(api);
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public SendReceipt sendReceipt()
	{
		return new SendReceipt(api, reportStatusGenerator());
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public DeleteDdpData deleteDdpData()
	{
		return new DeleteDdpData(api);
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public SaveUserApproval saveUserApproval()
	{
		return new SaveUserApproval(api);
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public ApproveDashboardBinaryListener approveDashboardBinaryListener()
	{
		return new ApproveDashboardBinaryListener(api);
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public SendEMailDataDelivered sendEMailDataDelivered()
	{
		return new SendEMailDataDelivered(api);
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public SendEMailDataNotDeliveredNo sendEMailDataNotDeliveredNo()
	{
		return new SendEMailDataNotDeliveredNo(api);
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public SendEMailDataNotDeliveredWait sendEMailDataNotDeliveredWait()
	{
		return new SendEMailDataNotDeliveredWait(api);
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public SendEMailDataNotDeliveredTimeoutApproval sendEMailDataNotDeliveredTimeoutApproval()
	{
		return new SendEMailDataNotDeliveredTimeoutApproval(api);
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public SendEMailDataNotDeliveredTimeoutDDP sendEMailDataNotDeliveredTimeoutDDP()
	{
		return new SendEMailDataNotDeliveredTimeoutDDP(api);
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public ReportBackend reportBackend()
	{
		return new ReportBackend(dashboardBackendUrlProduktiv, dashboardBackendUrlTest, dashboardBackendUrlDevelopment,
				dashboardBackend);
	}
}
