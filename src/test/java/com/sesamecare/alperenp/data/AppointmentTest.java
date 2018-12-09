package com.sesamecare.alperenp.data;

import org.junit.jupiter.api.Test;

import io.vertx.core.json.DecodeException;
import io.vertx.core.json.EncodeException;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class AppointmentTest {

	@Test
	void serializeAppointmentTest() {
		Appointment app = new Appointment("someID", 100, 200, 300, "Dr. Hannibal", Appointment.Status.AVAILABLE,
				100.58);
		try {
			String json = Json.encode(app);
			log.info("Serialize Appointment Test succeeded! Result: {}", json);
		} catch (EncodeException e) {
			log.error("Serialize Appointment Test failed!");
		}
	}

	@Test
	void deserializeTest() {
		String appJson = "{\"id\":\"someID\",\"createdTime\":100,\"appointmentDate\":200,\"appointmentDuration\":300,\"doctorName\":\"Dr. Hannibal\",\"status\":\"AVAILABLE\",\"price\":100.58}";
		try {
			Appointment app = Json.decodeValue(new JsonObject(appJson).toBuffer(), Appointment.class);
			log.info("Deserialize Appointment Test succeeded! Appointment ID: {}", app.getId());
		} catch (DecodeException e) {
			log.error("Deserialize Appointment Test failed!");
		}
	}
}
