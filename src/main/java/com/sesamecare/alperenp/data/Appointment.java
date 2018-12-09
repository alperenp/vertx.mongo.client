package com.sesamecare.alperenp.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 
 * @author alperenp
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Appointment {

	/**
	 * Appointment ID
	 */
	String id;

	/**
	 * Appointment creation time in ms
	 */
	long createdTime;

	/**
	 * Appointment date in ms
	 */
	long appointmentDate;

	/**
	 * Appointment duration in ms
	 */
	long appointmentDuration;

	/**
	 * Doctor Name
	 */
	String doctorName;

	/**
	 * Appointment status
	 */
	Status status;

	/**
	 * Appointment price
	 */
	double price;

	/**
	 * Status of appointment
	 * <p>
	 * Current options: available/booked
	 * 
	 * @author alperenp
	 *
	 */
	public enum Status {
		AVAILABLE,

		BOOKED;
	}
}
