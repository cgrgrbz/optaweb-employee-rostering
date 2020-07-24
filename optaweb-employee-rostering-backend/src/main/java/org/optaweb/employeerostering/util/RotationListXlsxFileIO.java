/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
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
package org.optaweb.employeerostering.util;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.optaweb.employeerostering.domain.rotation.ShiftTemplate;
import org.optaweb.employeerostering.domain.rotation.view.ShiftTemplateView;
import org.optaweb.employeerostering.domain.shift.ShiftType;
import org.optaweb.employeerostering.domain.skill.Skill;
import org.optaweb.employeerostering.domain.skill.view.SkillView;
import org.optaweb.employeerostering.service.roster.RosterService;
import org.optaweb.employeerostering.service.skill.SkillService;
import org.optaweb.employeerostering.service.spot.SpotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RotationListXlsxFileIO {

	private final SpotService spotService;

	private final SkillService skillService;
		
	private final RosterService rosterService;
	
	private DataFormatter df = new DataFormatter();

	@Autowired
	public RotationListXlsxFileIO(SpotService spotService, SkillService skillService, RosterService rosterService) {
		this.spotService = spotService;
		this.skillService = skillService;
		this.rosterService = rosterService;
	}

	public List<ShiftTemplateView> getShiftTemplateListFromExcelFile(Integer tenantId, InputStream excelFileStream)
			throws IOException {
		try (Workbook workbook = new XSSFWorkbook(excelFileStream)) {

			Sheet worksheet = workbook.getSheetAt(0);
			
			List<ShiftTemplateView> out = new ArrayList<>(worksheet.getPhysicalNumberOfRows() - 1);

			Map<String, Skill> skillMap = skillService.getSkillList(tenantId).stream()
					.collect(Collectors.toMap(s -> s.getName().toLowerCase(), Function.identity()));

			for (int i = 1; i <= worksheet.getLastRowNum(); i++) {

				// excel file columns
				// col1 col2 col3 col4 col5 col6 col7 col7
				// Spot requiredSkillSet - AND requiredSkillSet2 - OR startDayOffset startTime endDayOffset endTime type 

				Row row = worksheet.getRow(i);

				// assuming the rows entered in a sequence without any blank but anyway
				if (row == null || row.getCell(0) == null) {
					continue;
				}
				
				int rotationLength = rosterService.getRosterState(tenantId).getRotationLength();
				
				ShiftTemplate shiftTemplate = new ShiftTemplate();
				
				shiftTemplate.setTenantId(tenantId);

				String spotCode = df.formatCellValue(row.getCell(0));
				shiftTemplate.setSpot(spotService.findSpotByCode(tenantId, spotCode));

				int startDayOffset = (int) row.getCell(3).getNumericCellValue();
				shiftTemplate.setStartDayOffset(startDayOffset);
				
				int endDayOffset = (int) row.getCell(5).getNumericCellValue();
				shiftTemplate.setEndDayOffset(endDayOffset);

				//get time as string, convert it to LocalTime to set it
				
				LocalTime startTime = LocalTime.parse(row.getCell(4).getStringCellValue());
				shiftTemplate.setStartTime(startTime);
				
				LocalTime endTime = LocalTime.parse(row.getCell(6).getStringCellValue());
				shiftTemplate.setEndTime(endTime);
				
				//give rotation employee and vehicle null..

				shiftTemplate.setRotationEmployee(null);
				shiftTemplate.setRotationVehicle(null);
				
				int type = (int) row.getCell(7).getNumericCellValue();
				shiftTemplate.setType(ShiftType.values()[type]);

				String skillListString = (row.getCell(1) != null) ? row.getCell(1).getStringCellValue() : "";
				String skillListString2 = (row.getCell(2) != null) ? row.getCell(2).getStringCellValue() : "";

				shiftTemplate.setRequiredSkillSet(
						Arrays.stream(skillListString.split(",")).map(String::trim).filter(s -> !s.isEmpty())
								.map(skillName -> skillMap.computeIfAbsent(skillName.toLowerCase(), lowercaseName -> {
									SkillView skillView = new SkillView();
									skillView.setTenantId(tenantId);
									skillView.setName(skillName);
									return skillService.createSkill(tenantId, skillView);
								})).collect(Collectors.toCollection(HashSet::new)));
				
				shiftTemplate.setRequiredSkillSet2(
						Arrays.stream(skillListString2.split(",")).map(String::trim).filter(s -> !s.isEmpty())
								.map(skillName -> skillMap.computeIfAbsent(skillName.toLowerCase(), lowercaseName -> {
									SkillView skillView = new SkillView();
									skillView.setTenantId(tenantId);
									skillView.setName(skillName);
									return skillService.createSkill(tenantId, skillView);
								})).collect(Collectors.toCollection(HashSet::new)));

				ShiftTemplateView shiftTemplateView = new ShiftTemplateView(rotationLength, shiftTemplate);
				out.add(shiftTemplateView);
				System.out.println(i + " of " + worksheet.getLastRowNum());
			}
			return out;
		}
	}
}
