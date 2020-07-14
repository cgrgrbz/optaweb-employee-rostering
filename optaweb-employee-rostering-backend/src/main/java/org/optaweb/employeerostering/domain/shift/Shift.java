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

package org.optaweb.employeerostering.domain.shift;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;

import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.entity.PlanningPin;
import org.optaplanner.core.api.domain.variable.PlanningVariable;
import org.optaweb.employeerostering.domain.common.AbstractPersistable;
import org.optaweb.employeerostering.domain.employee.Employee;
import org.optaweb.employeerostering.domain.shift.view.ShiftView;
import org.optaweb.employeerostering.domain.skill.Skill;
import org.optaweb.employeerostering.domain.spot.Spot;
import org.optaweb.employeerostering.domain.vehicle.Vehicle;

@Entity
@PlanningEntity(pinningFilter = PinningShiftFilter.class)
public class Shift extends AbstractPersistable {

    private final AtomicLong lengthInMinutes = new AtomicLong(-1);
    
    @ManyToOne
    private Employee rotationEmployee;
    
    @NotNull
    @ManyToOne
    private Spot spot;
    
    private ShiftType type;
    
    @NotNull
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "ShiftRequiredSkillSet",
            joinColumns = @JoinColumn(name = "shiftId", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "skillId", referencedColumnName = "id")
    )
    private Set<Skill> requiredSkillSet;
    
    @NotNull
    private OffsetDateTime startDateTime;
    
    @NotNull
    private OffsetDateTime endDateTime;
    
    @PlanningPin
    private boolean pinnedByUser = false;

    @ManyToOne
    @PlanningVariable(valueRangeProviderRefs = "employeeRange", nullable = true)
    private Employee employee = null;

    @ManyToOne
    private Employee originalEmployee = null;
    
    
    ////VEHICLE START
    @ManyToOne
    private Vehicle rotationVehicle;
    
    @PlanningPin
    private boolean pinnedVehicleByUser = false;

    @ManyToOne
    @PlanningVariable(valueRangeProviderRefs = "vehicleRange", nullable = true)
    private Vehicle vehicle = null;

    @ManyToOne
    private Vehicle originalVehicle = null;
    
    ////VEHICLE END

    @SuppressWarnings("unused")
    public Shift() {
    }

    public Shift(Integer tenantId, Spot spot, OffsetDateTime startDateTime, OffsetDateTime endDateTime) {
        this(tenantId, spot, startDateTime, endDateTime, null, null, null);
    }

    public Shift(Integer tenantId, Spot spot, OffsetDateTime startDateTime, OffsetDateTime endDateTime,
            Employee rotationEmployee, Vehicle rotationVehicle, ShiftType type) {
        this(tenantId, spot, startDateTime, endDateTime, rotationEmployee, new HashSet<>(), null, rotationVehicle, null, null);
    }

    public Shift(Integer tenantId, Spot spot, OffsetDateTime startDateTime, OffsetDateTime endDateTime,
            Employee rotationEmployee, Set<Skill> requiredSkillSet, Employee originalEmployee,
            Vehicle rotationVehicle, Vehicle originalVehicle, ShiftType type) {
        super(tenantId);
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.spot = spot;
        this.rotationEmployee = rotationEmployee;
        this.requiredSkillSet = requiredSkillSet;
        this.originalEmployee = originalEmployee;

        this.rotationVehicle = rotationVehicle;
        this.originalVehicle = originalVehicle;
        
        this.type = type;        
    }

    public Shift(ZoneId zoneId, ShiftView shiftView, Spot spot) {
        this(zoneId, shiftView, spot, null, null, null);
    }

    public Shift(ZoneId zoneId, ShiftView shiftView, Spot spot, Employee rotationEmployee, Vehicle rotationVehicle, ShiftType type) {
        this(zoneId, shiftView, spot, rotationEmployee, new HashSet<>(), null, null, null, null);
    }

    public Shift(ZoneId zoneId, ShiftView shiftView, Spot spot,
    		Employee rotationEmployee, Set<Skill> requiredSkillSet, Employee originalEmployee,
    		Vehicle rotationVehicle, Vehicle originalVehicle, ShiftType type) {
        super(shiftView);
        this.startDateTime = OffsetDateTime.of(shiftView.getStartDateTime(),
                zoneId.getRules().getOffset(shiftView.getStartDateTime()));
        this.endDateTime = OffsetDateTime.of(shiftView.getEndDateTime(),
                zoneId.getRules().getOffset(shiftView.getEndDateTime()));
        this.spot = spot;
        this.pinnedByUser = shiftView.isPinnedByUser();
        this.rotationEmployee = rotationEmployee;
        this.requiredSkillSet = requiredSkillSet;
        this.originalEmployee = originalEmployee;
        
        this.rotationVehicle = rotationVehicle;
        this.originalVehicle = originalVehicle;
        this.type = type;
    }

    @Override
    public String toString() {
        return spot + " " + startDateTime + "-" + endDateTime;
    }

    public boolean follows(Shift other) {
        return !startDateTime.isBefore(other.endDateTime);
    }

    public boolean precedes(Shift other) {
        return !endDateTime.isAfter(other.startDateTime);
    }

    public long getLengthInMinutes() { // Thread-safe cache.
        long currentLengthInMinutes = lengthInMinutes.get();
        if (currentLengthInMinutes >= 0) {
            return currentLengthInMinutes;
        }
        long newLengthInMinutes = startDateTime.until(endDateTime, ChronoUnit.MINUTES);
        lengthInMinutes.set(newLengthInMinutes);
        return newLengthInMinutes;
    }

    public boolean isMoved() {
        return originalEmployee != null && originalEmployee != employee;
    }

    //check if employee has spot skills??
    public boolean hasRequiredSkills() {
        return employee.getSkillProficiencySet().containsAll(spot.getRequiredSkillSet());
    }

    //check if the vehicle has required skills??
    public boolean hasRequiredVehicleSkills() {
    	return vehicle.getSkillProficiencySet().containsAll(requiredSkillSet);
    }
    // ************************************************************************
    // Simple getters and setters
    // ************************************************************************

    public Spot getSpot() {
        return spot;
    }

    public void setSpot(Spot spot) {
        this.spot = spot;
    }

    public OffsetDateTime getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(OffsetDateTime startDateTime) {
        this.startDateTime = startDateTime;
        this.lengthInMinutes.set(-1);
    }

    public OffsetDateTime getEndDateTime() {
        return endDateTime;
    }

    public void setEndDateTime(OffsetDateTime endDateTime) {
        this.endDateTime = endDateTime;
        this.lengthInMinutes.set(-1);
    }

    public boolean isPinnedByUser() {
        return pinnedByUser;
    }

    public void setPinnedByUser(boolean lockedByUser) {
        this.pinnedByUser = lockedByUser;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public Employee getRotationEmployee() {
        return rotationEmployee;
    }

    public void setRotationEmployee(Employee rotationEmployee) {
        this.rotationEmployee = rotationEmployee;
    }

    public Employee getOriginalEmployee() {
        return originalEmployee;
    }

    public void setOriginalEmployee(Employee originalEmployee) {
        this.originalEmployee = originalEmployee;
    }

    public Set<Skill> getRequiredSkillSet() {
        return requiredSkillSet;
    }

    public void setRequiredSkillSet(Set<Skill> requiredSkillSet) {
        this.requiredSkillSet = requiredSkillSet;
    }

    public Vehicle getRotationVehicle() {
		return rotationVehicle;
	}

	public void setRotationVehicle(Vehicle rotationVehicle) {
		this.rotationVehicle = rotationVehicle;
	}

	public boolean isPinnedVehicleByUser() {
		return pinnedVehicleByUser;
	}

	public void setPinnedVehicleByUser(boolean pinnedVehicleByUser) {
		this.pinnedVehicleByUser = pinnedVehicleByUser;
	}

	public Vehicle getVehicle() {
		return vehicle;
	}

	public void setVehicle(Vehicle vehicle) {
		this.vehicle = vehicle;
	}

	public Vehicle getOriginalVehicle() {
		return originalVehicle;
	}

	public void setOriginalVehicle(Vehicle originalVehicle) {
		this.originalVehicle = originalVehicle;
	}

	public ShiftType getType() {
		return type;
	}

	public void setType(ShiftType type) {
		this.type = type;
	}

	public Shift inTimeZone(ZoneId zoneId) {
        Shift out = new Shift(zoneId, new ShiftView(zoneId, this), getSpot(), 
        		getRotationEmployee(), getRequiredSkillSet(), getOriginalEmployee(), 
        		getRotationVehicle(), getOriginalVehicle(), getType());
        out.setEmployee(getEmployee());
        out.setVehicle(getVehicle());
        return out;
    }
}
