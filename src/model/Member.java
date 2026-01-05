package model;

import java.util.Objects;

public class Member implements Comparable<Member> {

    private String nationalId;
    private String name;
    private String email;
    private String phone;
    private String password;
    private String profession;
    private double yearlyIncome;
    private double donation;
    private boolean hasDonated;
    private boolean isApproved;

    private Address address;
    private Role role;
    private CommitteeLevel committeeLevel;

    public Member(String nationalId,String name,String email,String phone,String password,String profession,double yearlyIncome,boolean hasDonated,boolean isApproved, Address address, Role role, CommitteeLevel committeeLevel) {
        this.nationalId = nationalId;
        this.name=name;
        this.email=email;
        this.phone=phone;
        this.password=password;
        this.profession=profession;
        this.yearlyIncome=yearlyIncome;
        this.donation=calculateDonation(yearlyIncome);
        this.hasDonated=hasDonated;
        this.isApproved=isApproved;
        this.address=address;
        this.role=role;
        this.committeeLevel=committeeLevel;
    }

    public Member(String nationalId, String name, String email, String phone,String password, String profession, double yearlyIncome, double donation, boolean hasDonated,boolean isApproved, Address address, Role role, CommitteeLevel committeeLevel) {
        this.nationalId = nationalId;
        this.name=name;
        this.email=email;
        this.phone=phone;
        this.password=password;
        this.profession=profession;
        this.yearlyIncome=yearlyIncome;
        this.donation=donation;
        this.hasDonated=hasDonated;
        this.isApproved=isApproved;
        this.address=address;
        this.role=role;
        this.committeeLevel=committeeLevel;
    }

    public Member(Member other) {
        this.nationalId = other.nationalId;
        this.name=other.name;
        this.email=other.email;
        this.phone=other.phone;
        this.password=other.password;
        this.profession=other.profession;
        this.donation=other.donation;
        this.yearlyIncome=other.yearlyIncome;
        this.hasDonated=other.hasDonated;
        this.isApproved=other.isApproved;
        this.address=other.address;
        this.role=other.role;
        this.committeeLevel=other.committeeLevel;
    }

    public static double calculateDonation(double yearlyIncome){
        return Math.round(yearlyIncome*0.05*100.0)/100.0;
    }

    public String getNationalId(){
    	return nationalId;
    	}
    public void setNationalId(String nationalId){
    	this.nationalId = nationalId; 
    	}
    public String getName(){
    	return name;
    	}
    public void setName(String name) { 
    	this.name = name;
    	}
    public String getEmail() {
    	return email;
    	}
    public void setEmail(String email) {
    	this.email = email;
    	}
    public String getPhone() { 
    	return phone; 
    	}
    public void setPhone(String phone) { 
    	this.phone = phone;
    	}
    public String getPassword() { 
    	return password; 
    	}
    public void setPassword(String password) {
    	this.password = password;
    	}
    public String getProfession() {
    	return profession; 
    	}
    public void setProfession(String profession) { 
    	this.profession = profession;
    	}
    public double getDonation() {
    	return donation; 
    	}
    public void setDonation(double donation) {
    	this.donation = donation;
    	}
    public double getYearlyIncome() {
    	return yearlyIncome; 
    	}
    public void setYearlyIncome(double yearlyIncome) { 
    	this.yearlyIncome = yearlyIncome; }
    
    public boolean hasDonated() {
    	return hasDonated; 
    	}
    public void setHasDonated(boolean hasDonated) {
    	this.hasDonated = hasDonated; 
    	}
    public boolean isApproved() { 
    	return isApproved;
    	}
    public void setApproved(boolean isApproved) {
    	this.isApproved = isApproved; 
    	}
    public Address getAddress() { 
    	return address; 
    	}
    public void setAddress(Address address) {
    	this.address = address;
    	}
    public Role getRole() { 
    	return role;
    	}
    public void setRole(Role role) {
    	this.role = role; 
    	}
    public CommitteeLevel getCommitteeLevel() {
    	return committeeLevel;
    	}
    public void setCommitteeLevel(CommitteeLevel committeeLevel) {
    	this.committeeLevel = committeeLevel; 
    	}

    @Override
    public String toString(){
        return "Name='"+name
                + "', Role=" + role
                + ", CommitteeLevel=" + committeeLevel
                + ", Profession='" + profession
                + "', yearlyIncome=" + yearlyIncome
                + ", nationalId='" + nationalId
                + "', email='" + email
                + "', phone='" + phone
                + "', donation=" + donation
                + ", hasDonated=" + hasDonated
                + ", isApproved=" + isApproved
                + ", address=" + address;
    }

    public String ProfessionalInfo() {
        return "Name='" + name + "\n"
                + "Role=" + role
                + ", CommitteeLevel=" + committeeLevel
                + ", Profession='" + profession
                + ", YearlyIncome=" + yearlyIncome + "\n";
    }

    @Override
    public int compareTo(Member other) {
        if (other == null) return 1;

        if (this.committeeLevel != null && other.committeeLevel != null) {
            int committeeComparison = Integer.compare(this.committeeLevel.getCommitteLevelOrder(), other.committeeLevel.getCommitteLevelOrder());
            if (committeeComparison != 0) return committeeComparison;
        } else if (this.committeeLevel != null) return -1;
        else if (other.committeeLevel != null) return 1;

        if (this.role != null && other.role != null) {
            int roleComparison = Integer.compare(this.role.gerRoleOrder(), other.role.gerRoleOrder());
            if (roleComparison != 0) return roleComparison;
        } else if (this.role != null) return -1;
        else if (other.role != null) return 1;

        if (this.address != null && other.address != null) {
            int divisionComparison = Integer.compare(this.address.getDivision().getDivisionOrder(), other.address.getDivision().getDivisionOrder());
            if (divisionComparison != 0) return divisionComparison;
            return Integer.compare(this.address.getDistrict().getDistrictOrder(), other.address.getDistrict().getDistrictOrder());
        } else if (this.address != null) return -1;
        else if (other.address != null) return 1;

        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Member)) return false;
        Member member = (Member) o;
        return Objects.equals(nationalId, member.nationalId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nationalId);
    }
}
