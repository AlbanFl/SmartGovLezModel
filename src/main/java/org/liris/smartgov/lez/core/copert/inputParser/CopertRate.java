package org.liris.smartgov.lez.core.copert.inputParser;

public class CopertRate {
	
	private String value;
	private double rate;
	private CopertProfile subProfile;
	
	public CopertRate() {
		
	}
	
	public CopertRate(String value, float rate, CopertProfile subProfile) {
		super();
		this.value = value;
		this.rate = rate;
		this.subProfile = subProfile;
	}

	public String getValue() {
		return value;
	}

	public double getRate() {
		return rate;
	}

	public CopertProfile getSubProfile() {
		return subProfile;
	}

}
