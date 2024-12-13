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

package de.medizininformatik_initiative.process.report.util;

import java.util.stream.Stream;

import org.hl7.fhir.r4.model.BackboneElement;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.ParameterComponent;
import org.hl7.fhir.r4.model.Task.TaskOutputComponent;

import de.medizininformatik_initiative.process.report.ConstantsReport;

public class ReportStatusGenerator
{
	public ParameterComponent createReportStatusInput(String statusCode)
	{
		return createReportStatusInput(statusCode, null);
	}

	public ParameterComponent createReportStatusInput(String statusCode, String errorMessage)
	{
		ParameterComponent input = new ParameterComponent();
		input.setValue(new Coding().setSystem(ConstantsReport.CODESYSTEM_REPORT_STATUS).setCode(statusCode));
		input.getType().addCoding().setSystem(ConstantsReport.CODESYSTEM_REPORT)
				.setCode(ConstantsReport.CODESYSTEM_REPORT_VALUE_REPORT_STATUS);

		if (errorMessage != null)
			addErrorExtension(input, errorMessage);

		return input;
	}

	public TaskOutputComponent createReportStatusOutput(String statusCode)
	{
		return createReportStatusOutput(statusCode, null);
	}

	public TaskOutputComponent createReportStatusOutput(String statusCode, String errorMessage)
	{
		TaskOutputComponent output = new TaskOutputComponent();
		output.setValue(new Coding().setSystem(ConstantsReport.CODESYSTEM_REPORT_STATUS).setCode(statusCode));
		output.getType().addCoding().setSystem(ConstantsReport.CODESYSTEM_REPORT)
				.setCode(ConstantsReport.CODESYSTEM_REPORT_VALUE_REPORT_STATUS);

		if (errorMessage != null)
			addErrorExtension(output, errorMessage);

		return output;
	}

	private void addErrorExtension(BackboneElement element, String errorMessage)
	{
		element.addExtension().setUrl(ConstantsReport.EXTENSION_REPORT_STATUS_ERROR_URL)
				.setValue(new StringType(errorMessage));
	}

	public void transformInputToOutput(Task inputTask, Task outputTask)
	{
		transformInputToOutputComponents(inputTask).forEach(outputTask::addOutput);
	}

	public Stream<Task.TaskOutputComponent> transformInputToOutputComponents(Task inputTask)
	{
		return inputTask.getInput().stream()
				.filter(i -> i.getType().getCoding().stream()
						.anyMatch(c -> ConstantsReport.CODESYSTEM_REPORT.equals(c.getSystem())
								&& ConstantsReport.CODESYSTEM_REPORT_VALUE_REPORT_STATUS.equals(c.getCode())))
				.map(this::toTaskOutputComponent);
	}

	private TaskOutputComponent toTaskOutputComponent(ParameterComponent inputComponent)
	{
		TaskOutputComponent outputComponent = new TaskOutputComponent().setType(inputComponent.getType())
				.setValue(inputComponent.getValue().copy());
		outputComponent.setExtension(inputComponent.getExtension());

		return outputComponent;
	}

	public void transformOutputToInput(Task outputTask, Task inputTask)
	{
		transformOutputToInputComponent(outputTask).forEach(inputTask::addInput);
	}

	public Stream<ParameterComponent> transformOutputToInputComponent(Task outputTask)
	{
		return outputTask.getOutput().stream()
				.filter(i -> i.getType().getCoding().stream()
						.anyMatch(c -> ConstantsReport.CODESYSTEM_REPORT.equals(c.getSystem())
								&& ConstantsReport.CODESYSTEM_REPORT_VALUE_REPORT_STATUS.equals(c.getCode())))
				.map(this::toTaskInputComponent);
	}

	private ParameterComponent toTaskInputComponent(TaskOutputComponent outputComponent)
	{
		ParameterComponent inputComponent = new ParameterComponent().setType(outputComponent.getType())
				.setValue(outputComponent.getValue().copy());
		inputComponent.setExtension(outputComponent.getExtension());

		return inputComponent;
	}
}
