package com.secura.dnft.generic.bean;

public enum Cycle {
	    MONTHLY(1),
	    QUARTERLY(3),
	    HALF_YEARLY(6),
	    YEARLY(12);

	    int months;
	    

	    Cycle(int months) {
	        this.months = months;
	    }

	    public int getMonths() {
	        return months;
	    }
	    
	    public static Cycle fromString(String value	) { 

	        if (value == null) {
	            throw new IllegalArgumentException("Cycle cannot be null");
	        }

	        switch (value.toUpperCase()) {

	            case "MONTHLY":
	            case "ONCE":
	                return MONTHLY;

	            case "QUARTERLY":
	                return QUARTERLY;

	            case "HALF_YEARLY":
	            case "HALFYEARLY":
	                return HALF_YEARLY;

	            case "YEARLY":
	                return YEARLY;

	            default:
	                throw new IllegalArgumentException("Invalid cycle: " + value);
	        }
	    }
	    	
}
