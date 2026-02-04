package com.sociolab.surehealth.testdata;

import com.sociolab.surehealth.enums.Speciality;
import com.sociolab.surehealth.model.Doctor;
import com.sociolab.surehealth.model.User;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test Data Builder for Doctor entity.
 * Builds a Doctor with an embedded User (not persisted).
 */
public final class DoctorBuilder {

    private static final AtomicInteger SEQ = new AtomicInteger(1);

    private User user;
    private String licenseNumber;
    private Speciality speciality;
    private int experienceYears;
    private boolean verified;

    private DoctorBuilder() {
        int n = SEQ.getAndIncrement();
        this.user = UserBuilder.aUser().withRole(com.sociolab.surehealth.enums.Role.DOCTOR).build();
        this.licenseNumber = "LIC-" + System.currentTimeMillis() + "-" + n;
        this.speciality = Speciality.ORTHOPEDIC;
        this.experienceYears = 5;
        this.verified = false;
    }

    public static DoctorBuilder aDoctor() {
        return new DoctorBuilder();
    }

    public DoctorBuilder withUser(User user) {
        this.user = user;
        return this;
    }

    public DoctorBuilder withLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
        return this;
    }

    public DoctorBuilder withSpeciality(Speciality speciality) {
        this.speciality = speciality;
        return this;
    }

    public DoctorBuilder withExperienceYears(int years) {
        this.experienceYears = years;
        return this;
    }

    public DoctorBuilder verified(boolean verified) {
        this.verified = verified;
        return this;
    }

    public Doctor build() {
        Doctor d = new Doctor();
        d.setUser(this.user);
        d.setLicenseNumber(this.licenseNumber);
        d.setSpeciality(this.speciality);
        d.setExperienceYears(this.experienceYears);
        d.setVerified(this.verified);
        return d;
    }
}

