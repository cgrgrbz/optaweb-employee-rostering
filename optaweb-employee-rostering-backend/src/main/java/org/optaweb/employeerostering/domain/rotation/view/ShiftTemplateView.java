/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.optaweb.employeerostering.domain.rotation.view;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.DurationSerializer;
import org.optaweb.employeerostering.domain.common.AbstractPersistable;
import org.optaweb.employeerostering.domain.rotation.ShiftTemplate;
import org.optaweb.employeerostering.domain.shift.ShiftType;
import org.optaweb.employeerostering.domain.skill.Skill;

public class ShiftTemplateView extends AbstractPersistable {

	private Long spotId;
	private List<Long> requiredSkillSetIdList;
	private List<Long> requiredSkillSet2IdList;

	private Long rotationEmployeeId;
	private Duration durationBetweenRotationStartAndTemplateStart;
	private Duration shiftTemplateDuration;

	private Long rotationVehicleId;

	private ShiftType type;
	
	public ShiftTemplateView() {
	}

	public ShiftTemplateView(Integer rotationLength, ShiftTemplate shiftTemplate) {
		super(shiftTemplate);
		this.spotId = shiftTemplate.getSpot().getId();
		this.rotationEmployeeId = (shiftTemplate.getRotationEmployee() != null)
				? shiftTemplate.getRotationEmployee().getId()
				: null;
		this.durationBetweenRotationStartAndTemplateStart = Duration.ofDays(shiftTemplate.getStartDayOffset())
				.plusSeconds(shiftTemplate.getStartTime().toSecondOfDay());
		this.shiftTemplateDuration = Duration
				.ofDays((shiftTemplate.getEndDayOffset() < shiftTemplate.getStartDayOffset()) ? rotationLength : 0)
				.plusDays(shiftTemplate.getEndDayOffset() - shiftTemplate.getStartDayOffset())
				.plusSeconds(shiftTemplate.getEndTime().toSecondOfDay())
				.minusSeconds(shiftTemplate.getStartTime().toSecondOfDay());

		this.requiredSkillSetIdList = shiftTemplate.getRequiredSkillSet().stream().map(Skill::getId).sorted()
				.collect(Collectors.toCollection(ArrayList::new));

		this.requiredSkillSet2IdList = shiftTemplate.getRequiredSkillSet2().stream().map(Skill::getId).sorted()
				.collect(Collectors.toCollection(ArrayList::new));

		this.rotationVehicleId = (shiftTemplate.getRotationVehicle() != null)
				? shiftTemplate.getRotationVehicle().getId()
				: null;

		this.type = (type == null) ? ShiftType.ONEWAY : type;
	}

	public ShiftTemplateView(Integer tenantId, Long spotId, Duration durationBetweenRotationStartAndTemplateStart,
			Duration shiftTemplateDuration, Long rotationEmployeeId, List<Long> requiredSkillSetIdList, List<Long> requiredSkillSet2IdList,
			Long rotationVehicleId, ShiftType type) {
		super(tenantId);
		this.spotId = spotId;
		this.durationBetweenRotationStartAndTemplateStart = durationBetweenRotationStartAndTemplateStart;
		this.shiftTemplateDuration = shiftTemplateDuration;
		this.rotationEmployeeId = rotationEmployeeId;
		this.requiredSkillSetIdList = requiredSkillSetIdList;
		this.requiredSkillSet2IdList = requiredSkillSet2IdList;

		this.rotationVehicleId = rotationVehicleId;
		
		this.type = type;
	}

	public Long getSpotId() {
		return spotId;
	}

	public void setSpotId(Long spotId) {
		this.spotId = spotId;
	}

	public Long getRotationEmployeeId() {
		return rotationEmployeeId;
	}

	public void setRotationEmployeeId(Long rotationEmployeeId) {
		this.rotationEmployeeId = rotationEmployeeId;
	}

	@JsonSerialize(using = DurationSerializer.class)
	public Duration getDurationBetweenRotationStartAndTemplateStart() {
		return durationBetweenRotationStartAndTemplateStart;
	}

	public void setDurationBetweenRotationStartAndTemplateStart(Duration durationBetweenRotationStartAndTemplateStart) {
		this.durationBetweenRotationStartAndTemplateStart = durationBetweenRotationStartAndTemplateStart;
	}

	@JsonSerialize(using = DurationSerializer.class)
	public Duration getShiftTemplateDuration() {
		return shiftTemplateDuration;
	}

	public void setShiftTemplateDuration(Duration shiftTemplateDuration) {
		this.shiftTemplateDuration = shiftTemplateDuration;
	}

	public List<Long> getRequiredSkillSetIdList() {
		return requiredSkillSetIdList;
	}

	public void setRequiredSkillSetIdList(List<Long> requiredSkillSetIdList) {
		this.requiredSkillSetIdList = requiredSkillSetIdList;
	}

	public List<Long> getRequiredSkillSet2IdList() {
		return requiredSkillSet2IdList;
	}

	public void setRequiredSkillSet2IdList(List<Long> requiredSkillSet2IdList) {
		this.requiredSkillSet2IdList = requiredSkillSet2IdList;
	}

	public Long getRotationVehicleId() {
		return rotationVehicleId;
	}

	public void setRotationVehicleId(Long rotationVehicleId) {
		this.rotationVehicleId = rotationVehicleId;
	}

	public ShiftType getType() {
		return type;
	}

	public void setType(ShiftType type) {
		this.type = type;
	}
}
