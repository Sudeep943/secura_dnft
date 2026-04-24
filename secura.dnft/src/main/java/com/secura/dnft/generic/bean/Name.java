package com.secura.dnft.generic.bean;

import java.util.Optional;

import com.secura.dnft.dao.ProfileRepository;
import com.secura.dnft.entity.Profile;
import com.secura.dnft.service.GenericService;

public class Name {

	
	private String firstName;
	private String middleName;
	private String lastName;
	
	
	public String getFirstName() {
		return firstName;
	}
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
	public String getMiddleName() {
		return middleName;
	}
	public void setMiddleName(String middleName) {
		this.middleName = middleName;
	}
	public String getLastName() {
		return lastName;
	}
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	
	@Override
	public String toString() {
		return firstName +" "+  middleName +" "+lastName;
	}

	/**
	 * Builds a display string for the given Name and profileId.
	 * If the Name object is null or has no non-empty fields, the name is resolved
	 * from the prfl_name column of the profile table using the profileId.
	 * The result is formatted as:
	 *   "FirstName MiddleName LastName (profileId)"
	 * where any null or empty name parts are omitted.
	 */
	public static String toStringWithProfileID(String profileId, Name nameObject,
			ProfileRepository profileRepository, GenericService genericService) {
		Name resolvedName = nameObject;
		if (!hasNameContent(nameObject) && profileId != null && !profileId.trim().isEmpty()
				&& profileRepository != null) {
			try {
				Optional<Profile> profileOpt = profileRepository.findById(profileId.trim());
				if (profileOpt.isPresent() && profileOpt.get().getPrflName() != null
						&& genericService != null) {
					resolvedName = genericService.fromJson(profileOpt.get().getPrflName(), Name.class);
				}
			} catch (RuntimeException e) {
				resolvedName = null;
			}
		}
		StringBuilder sb = new StringBuilder();
		if (resolvedName != null) {
			if (hasText(resolvedName.getFirstName())) {
				sb.append(resolvedName.getFirstName().trim());
			}
			if (hasText(resolvedName.getMiddleName())) {
				if (sb.length() > 0) {
					sb.append(" ");
				}
				sb.append(resolvedName.getMiddleName().trim());
			}
			if (hasText(resolvedName.getLastName())) {
				if (sb.length() > 0) {
					sb.append(" ");
				}
				sb.append(resolvedName.getLastName().trim());
			}
		}
		if (profileId != null && !profileId.trim().isEmpty()) {
			if (sb.length() > 0) {
				sb.append(" ");
			}
			sb.append("(").append(profileId.trim()).append(")");
		}
		return sb.toString();
	}

	private static boolean hasNameContent(Name name) {
		if (name == null) {
			return false;
		}
		return hasText(name.getFirstName()) || hasText(name.getMiddleName()) || hasText(name.getLastName());
	}

	private static boolean hasText(String value) {
		return value != null && !value.trim().isEmpty();
	}
	
	
	
}
