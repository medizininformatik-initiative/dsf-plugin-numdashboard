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
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Type;

import de.medizininformatik_initiative.process.report.ConstantsReport;
import de.medizininformatik_initiative.process.report.util.ReportStatusGenerator;
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.activity.AbstractTaskMessageSend;
import dev.dsf.bpe.v1.variables.Variables;

public class SendReceipt extends AbstractTaskMessageSend
{
	private final ReportStatusGenerator statusGenerator;

	public SendReceipt(ProcessPluginApi api, ReportStatusGenerator statusGenerator)
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
	protected Stream<Task.ParameterComponent> getAdditionalInputParameters(DelegateExecution execution,
			Variables variables)
	{
		if (variables.getString(ConstantsReport.BPMN_EXECUTION_VARIABLE_REPORT_RECEIVE_ERROR) != null)
			return createReceiptError(variables);
		else
			return createReceiptOk();
	}

	private Stream<Task.ParameterComponent> createReceiptError(Variables variables)
	{
		return statusGenerator.transformOutputToInputComponent(variables.getStartTask())
				.map(this::receiveToReceiptStatus);
	}

	private Task.ParameterComponent receiveToReceiptStatus(Task.ParameterComponent parameterComponent)
	{
		Type value = parameterComponent.getValue();
		if (value instanceof Coding coding
				&& ConstantsReport.CODESYSTEM_REPORT_STATUS_VALUE_RECEIVE_ERROR.equals(coding.getCode()))
		{
			coding.setCode(ConstantsReport.CODESYSTEM_REPORT_STATUS_VALUE_RECEIPT_ERROR);
		}

		return parameterComponent;
	}

	private Stream<Task.ParameterComponent> createReceiptOk()
	{
		Task.ParameterComponent parameterComponent = new Task.ParameterComponent();
		parameterComponent.getType().addCoding().setSystem(ConstantsReport.CODESYSTEM_REPORT)
				.setCode(ConstantsReport.CODESYSTEM_REPORT_VALUE_REPORT_STATUS);
		parameterComponent.setValue(new Coding().setSystem(ConstantsReport.CODESYSTEM_REPORT_STATUS)
				.setCode(ConstantsReport.CODESYSTEM_REPORT_STATUS_VALUE_RECEIPT_OK));

		return Stream.of(parameterComponent);
	}
}
