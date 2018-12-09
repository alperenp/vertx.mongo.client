package com.sesamecare.alperenp.service;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.sesamecare.alperenp.data.Appointment;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author alperenp
 *
 */
@Slf4j
@ExtendWith(VertxExtension.class)
public class AppointmentServiceTest {
	String hostname = "localhost";
	int port = 8080;

	/**
	 * Deploys service and creates initial set of entries for each test case
	 * 
	 * @param vertx
	 * @param testContext
	 * @throws Throwable
	 */
	@BeforeEach
	void initialize(Vertx vertx, VertxTestContext testContext) throws Throwable {
		JsonObject serviceConf = new JsonObject().put("http.port", port).put("db_name", "DB_APP")
				.put("mongo_collection", "appointments");
		DeploymentOptions options = new DeploymentOptions().setConfig(serviceConf).setInstances(1);
		AppointmentService server = new AppointmentService();
		vertx.deployVerticle(server, options, ar -> {
			log.info("Service deployed with id {}", ar.result());
			initialEntries(vertx, testContext);
			testContext.completeNow();
		});
	}

	/**
	 * Defines initial set of entries to be used in tests
	 * 
	 * @param vertx
	 * @param testContext
	 */
	private void initialEntries(Vertx vertx, VertxTestContext testContext) {
		log.info("");
		log.info("---------------- add initial entries ----------------");
		long now = System.currentTimeMillis();
		long oneweek = 604800000;
		long oneHour = 3600000;
		long tomorrow = now + (oneHour * 24);
		long nextweek = now + oneweek;
		long nextnextweek = now + (oneweek * 2);
		Appointment a1 = new Appointment("1", now, tomorrow, oneHour, "Dr. A", Appointment.Status.BOOKED, 450.8);
		Appointment a2 = new Appointment("2", now, nextweek, oneHour * 2, "Dr. B", Appointment.Status.AVAILABLE, 950.4);
		Appointment a3 = new Appointment("3", now, nextweek, oneHour / 2, "Dr. C", Appointment.Status.BOOKED, 1950);
		Appointment a4 = new Appointment("4", now, nextweek + oneHour, oneHour, "Dr. D", Appointment.Status.AVAILABLE,
				410);
		Appointment a5 = new Appointment("5", now, nextnextweek, oneHour * 2, "Dr. B", Appointment.Status.BOOKED, 750);
		Appointment a6 = new Appointment("6", now, tomorrow, oneHour * 2, "Dr. K", Appointment.Status.BOOKED, 530);
		Appointment a7 = new Appointment("7", now, nextweek + oneHour * 2, oneHour * 2, "Dr. K",
				Appointment.Status.BOOKED, 430);
		List<Appointment> list = new LinkedList<>();
		list.add(a1);
		list.add(a2);
		list.add(a3);
		list.add(a4);
		list.add(a5);
		list.add(a6);
		list.add(a7);
		String url = "/rest/insertAppointment";
		WebClient client = WebClient.create(vertx);
		Checkpoint responsesReceived = testContext.checkpoint(list.size());
		list.forEach(appointment -> insertRequest(client, url, new JsonObject(Json.encode(appointment)), testContext,
				responsesReceived));
	}

	/**
	 * Removes all data existing in db through service
	 * 
	 * @param vertx
	 * @param testContext
	 */
	@AfterEach
	void tearDown(Vertx vertx, VertxTestContext testContext) {
		Checkpoint responsesReceived = testContext.checkpoint();
		vertx.createHttpClient().getNow(port, hostname, "/rest/deleteAllAppointments",
				response -> testContext.verify(() -> {
					response.handler(body -> {
						try {
							JsonObject result = new JsonObject(body);
							if (result.getBoolean("result")) {
								log.info("Delete all succeed");
							} else {
								log.error("Delete all fail.");
							}
						} catch (DecodeException e) {
							log.error("Tear down fail");
						}
						responsesReceived.flag();
					});
				}));
	}

	/**
	 * Checks whether service is deployed properly and responds to REST end point
	 * "/"
	 * 
	 * @param vertx
	 * @param testContext
	 */
	@Test
	void serviceWorkTest(Vertx vertx, VertxTestContext testContext) {
		log.info("");
		log.info("---------------- service working test ----------------");
		Checkpoint responsesReceived = testContext.checkpoint();
		vertx.createHttpClient().getNow(port, hostname, "/", response -> testContext.verify(() -> {
			response.handler(body -> {
				if (body.toString().contains("Hello")) {
					log.info("Basic test success");
				} else {
					log.error("Basic test fail");
				}
				responsesReceived.flag();
			});
		}));
	}

	/**
	 * Checks whether service is inserting new entries properly and responds to REST
	 * end point "/rest/insertAppointment"
	 * 
	 * @param vertx
	 * @param testContext
	 */
	@Test
	void addEntriesTest(Vertx vertx, VertxTestContext testContext) {
		log.info("");
		log.info("---------------- add entries test ----------------");
		long now = System.currentTimeMillis();
		long oneweek = 604800000;
		long oneHour = 3600000;
		long tomorrow = now + (oneHour * 24);
		long nextweek = now + oneweek;
		Appointment a8 = new Appointment("8", now, tomorrow + oneHour, oneHour, "Dr. A", Appointment.Status.BOOKED,
				4150.8);
		Appointment a9 = new Appointment("9", now, nextweek + oneHour, oneHour * 2, "Dr. B",
				Appointment.Status.AVAILABLE, 1950.4);
		List<Appointment> list = new LinkedList<>();
		list.add(a8);
		list.add(a9);
		String url = "/rest/insertAppointment";
		WebClient client = WebClient.create(vertx);
		Checkpoint responsesReceived = testContext.checkpoint(list.size());
		list.forEach(appointment -> insertRequest(client, url, new JsonObject(Json.encode(appointment)), testContext,
				responsesReceived));
	}

	/**
	 * Common method for sending insert request
	 * 
	 * @param client
	 * @param url
	 * @param appointmentJson
	 * @param testContext
	 * @param responsesReceived
	 */
	private void insertRequest(WebClient client, String url, JsonObject appointmentJson, VertxTestContext testContext,
			Checkpoint responsesReceived) {
		client.post(port, hostname, url).sendJsonObject(appointmentJson, asyncResult -> testContext.verify(() -> {
			if (asyncResult.succeeded()) {
				String body = asyncResult.result().bodyAsString();
				try {
					JsonObject result = new JsonObject(body);
					if (result.getBoolean("result")) {
						log.info("Insert single entry succeed");
					} else {
						log.error("Insert single entry fail");
					}
					Assertions.assertTrue(result.getBoolean("result"));
				} catch (DecodeException e) {
					log.error("Insert test fail");
				}
				responsesReceived.flag();
			} else {
				log.error("Insert test fail");
			}
		}));
	}

	/**
	 * Test for retrieving all entries exist in DB from service
	 * 
	 * @param vertx
	 * @param testContext
	 */
	@Test
	void getAllTest(Vertx vertx, VertxTestContext testContext) {
		log.info("");
		log.info("---------------- get all entries test ----------------");
		Checkpoint responsesReceived = testContext.checkpoint();
		String url = "/rest/allAppointments";
		vertx.createHttpClient().getNow(port, hostname, url, response -> testContext.verify(() -> {
			response.handler(body -> {
				log.info("body: {}", body);
				try {
					JsonArray result = new JsonArray(body);
					log.info("DB has {} entry/entries", result.size());
					Assertions.assertFalse(result.size() == 0);
				} catch (DecodeException e) {
					log.error("Get all appointments test");
				}
				responsesReceived.flag();
			});
		}));
	}

	/**
	 * Test for removing single entry from DB using service
	 * 
	 * @param vertx
	 * @param testContext
	 */
	@Test
	void deleteEntryTest(Vertx vertx, VertxTestContext testContext) {
		log.info("");
		log.info("---------------- delete entry test ----------------");
		Checkpoint responsesReceived = testContext.checkpoint();
		String url = "/rest/deleteAppointment";
		WebClient client = WebClient.create(vertx);
		JsonObject json = new JsonObject().put("id", "2");
		client.delete(port, hostname, url).sendJsonObject(json, asyncResult -> testContext.verify(() -> {
			if (asyncResult.succeeded()) {
				String body = asyncResult.result().bodyAsString();
				try {
					JsonObject result = new JsonObject(body);
					if (result.getBoolean("result")) {
						log.info("Delete single entry succeed");
					} else {
						log.error("Delete single entry fail. Entry may not exist in DB");
					}
					Assertions.assertTrue(result.getBoolean("result"));
				} catch (DecodeException e) {
					log.error("Delete test fail");
				}
				responsesReceived.flag();
			} else {
				log.error("Delete test fail");
			}
		}));
	}

	/**
	 * Update Test for updating single entry from DB using service
	 * 
	 * @param vertx
	 * @param testContext
	 */
	@Test
	void updateEntryTest(Vertx vertx, VertxTestContext testContext) {
		log.info("");
		log.info("---------------- update entry test ----------------");
		Checkpoint responsesReceived = testContext.checkpoint();
		String url = "/rest/updateAppointment";
		WebClient client = WebClient.create(vertx);
		long now = System.currentTimeMillis();
		long oneweek = 604800000;
		long oneHour = 3600000;
		Appointment newa3 = new Appointment("3", now, now + (oneweek * 2), oneHour / 2, "Dr. C",
				Appointment.Status.AVAILABLE, 1950);
		JsonObject json = new JsonObject(Json.encode(newa3));
		client.put(port, hostname, url).sendJsonObject(json, asyncResult -> testContext.verify(() -> {
			if (asyncResult.succeeded()) {
				String body = asyncResult.result().bodyAsString();
				log.info(body);
				try {
					JsonObject result = new JsonObject(body);
					if (result.getBoolean("result")) {
						log.info("Update single entry succeed");
					} else {
						log.error("Update single entry fail. Entry may not exist in DB");
					}
					Assertions.assertTrue(result.getBoolean("result"));
				} catch (DecodeException e) {
					log.error("Update test fail");
				}
				responsesReceived.flag();
			} else {
				log.error("Update test fail");
			}
		}));
	}

	/**
	 * Test for finding specified entry in DB using service
	 * 
	 * @param vertx
	 * @param testContext
	 */
	@Test
	void findEntryTest(Vertx vertx, VertxTestContext testContext) {
		log.info("");
		log.info("---------------- find entry test ----------------");
		Checkpoint responsesReceived = testContext.checkpoint();
		String url = "/rest/findAppointment";
		WebClient client = WebClient.create(vertx);
		JsonObject json = new JsonObject().put("id", "1");
		client.get(port, hostname, url).sendJsonObject(json, asyncResult -> testContext.verify(() -> {
			if (asyncResult.succeeded()) {
				try {
					JsonObject result = asyncResult.result().bodyAsJsonObject();
					if (result.isEmpty()) {
						log.info("Find single entry failed. Given entry does not exist in DB");
					} else {
						log.info("Find single entry succeed. Entry: {}", result);
					}

				} catch (DecodeException e) {
					log.error("Find test fail");
				}
				responsesReceived.flag();
			} else {
				log.error("Find test fail");
			}
		}));
	}

	/**
	 * Test for finding entries with given criteria from DB using service
	 * <p>
	 * Criteria: entries from [previous month - next month] AND
	 * {@link Appointment.Status#BOOKED}
	 * <p>
	 * Results are tested whether they are sorted in ascending order as well
	 * 
	 * @param vertx
	 * @param testContext
	 */
	@Test
	void findEntriesInRangeTest(Vertx vertx, VertxTestContext testContext) {
		log.info("");
		log.info("---------------- find entries in range and sorted test ----------------");
		Checkpoint responsesReceived = testContext.checkpoint();
		String url = "/rest/findAppointmentsInRange";
		WebClient client = WebClient.create(vertx);

		long now = System.currentTimeMillis();
		long onemonth = 2592000000L;
		JsonObject json = new JsonObject().put("start", now - onemonth).put("end", now + onemonth * 2);
		client.get(port, hostname, url).sendJsonObject(json, asyncResult -> testContext.verify(() -> {
			if (asyncResult.succeeded()) {
				try {
					JsonArray jsonArray = asyncResult.result().bodyAsJsonArray();
					Double previousPrice = Double.valueOf(0);
					Iterator<Object> i = jsonArray.iterator();
					while (i.hasNext()) {
						JsonObject entry = (JsonObject) i.next();
						Appointment app = Json.decodeValue(entry.toBuffer(), Appointment.class);
						Assertions.assertFalse(app.getPrice() < previousPrice);
						previousPrice = app.getPrice();
						log.info(entry.toString());
					}
				} catch (DecodeException e) {
					log.error("Find test fail");
				}
				responsesReceived.flag();
			} else {
				log.error("Find test fail");
			}
		}));
	}
}
