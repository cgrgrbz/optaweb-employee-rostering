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
import org.optaweb.employeerostering.domain.skill.Skill;
import org.optaweb.employeerostering.domain.skill.view.SkillView;
import org.optaweb.employeerostering.domain.spot.view.SpotView;
import org.optaweb.employeerostering.service.skill.SkillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SpotListXlsxFileIO {

	private final SkillService skillService;

	private DataFormatter df = new DataFormatter();
	
	@Autowired
	public SpotListXlsxFileIO(SkillService skillService) {
		this.skillService = skillService;
	}

	public List<SpotView> getSpotListFromExcelFile(Integer tenantId, InputStream excelFileStream) throws IOException {
		try (Workbook workbook = new XSSFWorkbook(excelFileStream)) {

			Sheet worksheet = workbook.getSheetAt(0);

			List<SpotView> out = new ArrayList<>(worksheet.getPhysicalNumberOfRows() - 1);

			Map<String, Skill> skillMap = skillService.getSkillList(tenantId).stream()
					.collect(Collectors.toMap(s -> s.getName().toLowerCase(), Function.identity()));

			for (int i = 1; i <= worksheet.getLastRowNum(); i++) {

				// excel file columns
				// col1 col2 col3 col4 col5 col6
				// code name nameDetail length description skill

				Row row = worksheet.getRow(i);

				// assuming the rows entered in a sequence without any blank but anyway
				if (row == null || row.getCell(0) == null) {
					continue;
				}

				SpotView spot = new SpotView();

				spot.setTenantId(tenantId);
								
				String code = df.formatCellValue(row.getCell(0));
				String name = df.formatCellValue(row.getCell(1));
				String nameDetail = df.formatCellValue(row.getCell(2));
				Double length = row.getCell(3).getNumericCellValue();
				String description = df.formatCellValue(row.getCell(4));
				
				spot.setCode(code);
				spot.setName(name);
				spot.setNameDetail(nameDetail);
				spot.setLength(length);
				spot.setDescription(description);
				
				String skillListString = (row.getCell(5) != null) ? row.getCell(5).getStringCellValue() : "";

				spot.setRequiredSkillSet(
						Arrays.stream(skillListString.split(",")).map(String::trim).filter(s -> !s.isEmpty())
								.map(skillName -> skillMap.computeIfAbsent(skillName.toLowerCase(), lowercaseName -> {
									SkillView skillView = new SkillView();
									skillView.setTenantId(tenantId);
									skillView.setName(skillName);
									return skillService.createSkill(tenantId, skillView);
								})).collect(Collectors.toCollection(HashSet::new)));

				out.add(spot);
			}
			return out;
		}
	}
}
