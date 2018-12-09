package com.sesamecare.alperenp.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.sesamecare.alperenp.data.Appointment;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Appointment Service
 * 
 * @author alperenp
 *
 */
@Slf4j
public class AppointmentService extends AbstractVerticle {

	/**
	 * Router for specifying URLs of HTTP requests
	 */
	Router restAPI;

	/**
	 * start method of service
	 */
	@Override
	public void start(Future<Void> fut) throws Exception {
		int port = config().getInteger("http.port", -1);
		startRestService(port).setHandler(started -> {
			if (started.succeeded()) {
				log.info("Service successfully deployed!");
				fut.complete();
			} else {
				fut.fail(started.cause());
			}
		});

	}

	/**
	 * Creates a router, creates an HTTP server with given router and given port
	 * number read in config
	 */
	private Future<HttpServer> startRestService(int port) {
		// Create the HTTP server and pass the "accept" method to the request handler.
		this.restAPI = configureRouter();
		return createHttpServer(restAPI, port);
	}

	/**
	 * Create http server for the REST service.
	 *
	 * @param router router instance
	 * @param host   http host
	 * @param port   http port
	 * @return async result of the procedure
	 */
	private Future<HttpServer> createHttpServer(@NonNull Router router, int port) {
		Future<HttpServer> httpServerFuture = Future.future();
		vertx.createHttpServer().requestHandler(router).listen(port, result -> {
			if (result.succeeded()) {
				log.info("Server Created!");
				httpServerFuture.complete(result.result());
			} else {
				log.info("Server cannot be created! {}", result.cause());
				httpServerFuture.fail(result.cause());
			}
		});
		return httpServerFuture;
	}

	/**
	 * Defines Rest end points of service
	 * 
	 * @see <a href=
	 *      "https://vertx.io/blog/some-rest-with-vert-x/">vertx-rest-api</a>
	 * @return
	 */
	private Router configureRouter() {
		// Create a router object.
		restAPI = Router.router(vertx);

		// REST API for base web page
		restAPI.get("/").handler(this::baseWebPage);

		// REST API to delete appointment
		restAPI.route("/rest/deleteAppointment/*").handler(BodyHandler.create());
		restAPI.delete("/rest/deleteAppointment").handler(this::deleteAppointment);

		// REST API to insert appointment
		restAPI.route("/rest/insertAppointment/*").handler(BodyHandler.create());
		restAPI.post("/rest/insertAppointment").handler(this::insertAppointment);

		// REST API to update appointment
		restAPI.route("/rest/updateAppointment/*").handler(BodyHandler.create());
		restAPI.put("/rest/updateAppointment").handler(this::updateAppointment);

		// REST API to get appointment
		restAPI.route("/rest/findAppointment/*").handler(BodyHandler.create());
		restAPI.get("/rest/findAppointment").handler(this::findAppointment);

		// REST API to get appointment
		restAPI.route("/rest/findAppointmentsInRange/*").handler(BodyHandler.create());
		restAPI.get("/rest/findAppointmentsInRange").handler(this::findAppointmentsWithRangeAndSort);

		// REST API to get all appointments
		restAPI.get("/rest/allAppointments/").handler(this::getAllAppointments);

		// REST API to delete all appointments
		restAPI.get("/rest/deleteAllAppointments/").handler(this::deleteAllAppointments);

		return restAPI;
	}

	/**
	 * Default Web Page
	 * 
	 * @return html {@link String}
	 */
	private void baseWebPage(RoutingContext routingContext) {
		serviceCallMessage(routingContext);
		List<String> services = new ArrayList<>();
		for (Route route : restAPI.getRoutes()) {
			services.add(route.getPath());
		}
		String webPage = "<h1>Hello from Vert.x AppointmentService</h1>" + "<br>" + "<h2>Available services: "
				+ services.toString() + "</h2>";
		sendResponseToClient(routingContext, 200, "text/html", webPage);
	}

	/**
	 * Returns all existing entries in DB
	 * 
	 * @param routingContext
	 */
	private void getAllAppointments(RoutingContext routingContext) {
		serviceCallMessage(routingContext);
		// Main operation
		AppointmentServiceController controller = new AppointmentServiceController(vertx, config());
		Future<List<JsonObject>> future = controller.getAllAppointments();
		future.setHandler(result -> {
			List<JsonObject> jsonResult = Collections.emptyList();
			int statuscode = -1;
			if (future.succeeded()) {
				statuscode = 200;
				jsonResult = future.result();
			} else {
				statuscode = 400;
				log.error("Get All Appointments failed. Details: {}", future.cause());
			}

			// Output
			sendResponseToClient(routingContext, statuscode, "application/json; charset=utf-8", jsonResult.toString());
		});
	}

	/**
	 * Returns all existing entries in DB
	 * 
	 * @param routingContext
	 */
	private void deleteAllAppointments(RoutingContext routingContext) {
		serviceCallMessage(routingContext);
		// Main operation
		AppointmentServiceController controller = new AppointmentServiceController(vertx, config());
		Future<JsonObject> future = controller.deleteAllAppointments();
		future.setHandler(result -> {
			JsonObject jsonResult = new JsonObject();
			int statuscode = -1;
			if (future.succeeded()) {
				statuscode = 200;
				jsonResult = future.result();
			} else {
				statuscode = 400;
				log.error("Delete All Appointments failed. Details: {}", future.cause());
			}

			// Output
			sendResponseToClient(routingContext, statuscode, "application/json; charset=utf-8", jsonResult.toString());
		});
	}

	/**
	 * Inserts new entry to DB sent from client.
	 * <p>
	 * Returns true if insertions succeeds, else returns false
	 * 
	 * @param routingContext
	 */
	private void insertAppointment(RoutingContext routingContext) {
		// Input check
		serviceCallMessage(routingContext);
		JsonObject appointmentJson = routingContext.getBodyAsJson();
		log.info("body received from server: {}", appointmentJson);
		Optional<Appointment> appointment = decodeAppointment(appointmentJson);
		if (!appointment.isPresent()) {
			routingContext.response().setStatusCode(400).end();
			return;
		}

		// Main operation
		AppointmentServiceController controller = new AppointmentServiceController(vertx, config());
		Future<JsonObject> future = controller.insertAppointment(appointment.get());
		future.setHandler(result -> {
			JsonObject jsonResult = new JsonObject();
			// Output
			if (result.succeeded()) {
				jsonResult = future.result();
				sendResponseToClient(routingContext, 200, "application/json; charset=utf-8", jsonResult.toString());
			} else {
				sendResponseToClient(routingContext, 400, "application/json; charset=utf-8", jsonResult.toString());
				log.error("Insert Appointment failed. Details: {}", future.cause());
			}
		});
	}

	/**
	 * Removes Appointment with given identifier.
	 * <p>
	 * Returns true to client if successful removal, else returns false
	 * 
	 * @param routingContext
	 */
	private void deleteAppointment(RoutingContext routingContext) {
		// Input check
		serviceCallMessage(routingContext);
		JsonObject json = routingContext.getBodyAsJson();
		if (json == null || json.getString("id") == null) {
			log.info("failed!");
			routingContext.response().setStatusCode(400).end();
			return;
		}

		// Main operation
		AppointmentServiceController controller = new AppointmentServiceController(vertx, config());
		Future<JsonObject> future = controller.deleteOne(json.getString("id"));
		future.setHandler(result -> {
			JsonObject jsonResult = new JsonObject();
			if (result.succeeded()) {
				jsonResult = future.result();
				sendResponseToClient(routingContext, 200, "application/json; charset=utf-8", jsonResult.toString());

			} else {
				sendResponseToClient(routingContext, 400, "application/json; charset=utf-8", jsonResult.toString());
				log.error("Delete Appointment failed. Details: {}", future.cause());
			}
		});
	}

	private void updateAppointment(RoutingContext routingContext) {
		// Input check
		serviceCallMessage(routingContext);
		JsonObject appointmentJson = routingContext.getBodyAsJson();
		Optional<Appointment> appointment = decodeAppointment(appointmentJson);
		if (!appointment.isPresent()) {
			routingContext.response().setStatusCode(400).end();
			return;
		}

		// Main operation
		AppointmentServiceController controller = new AppointmentServiceController(vertx, config());
		Future<JsonObject> future = controller.replaceAppointment(appointment.get());
		future.setHandler(result -> {
			JsonObject jsonResult = new JsonObject();
			// Output
			if (result.succeeded()) {
				jsonResult = future.result();
				sendResponseToClient(routingContext, 200, "application/json; charset=utf-8", jsonResult.toString());
			} else {
				sendResponseToClient(routingContext, 400, "application/json; charset=utf-8", jsonResult.toString());
				log.error("Insert Appointment failed. Details: {}", future.cause());
			}
		});
	}

	/**
	 * Finds Appointment with given identifier.
	 * <p>
	 * Returns true to client if successful removal, else returns false
	 * 
	 * @param routingContext
	 */
	private void findAppointment(RoutingContext routingContext) {
		// Input check
		serviceCallMessage(routingContext);
		JsonObject json = routingContext.getBodyAsJson();
		if (json == null || json.getString("id") == null) {
			log.info("failed!");
			routingContext.response().setStatusCode(400).end();
			return;
		}

		AppointmentServiceController controller = new AppointmentServiceController(vertx, config());
		Future<List<JsonObject>> future = controller.findAppointment(json.getString("id"));
		future.setHandler(result -> {
			JsonObject jsonResult = new JsonObject();
			if (result.succeeded()) {
				if (future.result().size() == 0) {
					sendResponseToClient(routingContext, 200, "application/json; charset=utf-8", jsonResult.toString());
				} else {
					jsonResult = future.result().get(0);
					jsonResult.remove("_id");
					sendResponseToClient(routingContext, 200, "application/json; charset=utf-8", jsonResult.toString());
				}

			} else {
				sendResponseToClient(routingContext, 400, "application/json; charset=utf-8", jsonResult.toString());
				log.error("Find Appointment failed. Details: {}", future.cause());
			}
		});
	}

	/**
	 * Retrieve all appointments that are scheduled between a date range and sorted
	 * by price.
	 * <p>
	 * Returns true to client if successful removal, else returns false
	 * 
	 * @param routingContext
	 */
	private void findAppointmentsWithRangeAndSort(RoutingContext routingContext) {
		// Input check
		serviceCallMessage(routingContext);
		JsonObject json = routingContext.getBodyAsJson();
		if (json == null || json.getLong("start") == null || json.getLong("end") == null) {
			log.info("failed!");
			routingContext.response().setStatusCode(400).end();
			return;
		}

		AppointmentServiceController controller = new AppointmentServiceController(vertx, config());
		Future<List<JsonObject>> future = controller.findAppointmentsWithRangeAndSort(json.getLong("start"),
				json.getLong("end"));
		future.setHandler(result -> {
			List<JsonObject> jsonResult = Collections.emptyList();
			if (result.succeeded()) {
				jsonResult = result.result();
				jsonResult.forEach(jsonEntry -> jsonEntry.remove("_id"));
				sendResponseToClient(routingContext, 200, "application/json; charset=utf-8", jsonResult.toString());
			} else {
				sendResponseToClient(routingContext, 400, "application/json; charset=utf-8", jsonResult.toString());
				log.error("Find Appointments in range and sort failed. Details: {}", future.cause());
			}
		});

	}

	/* ------ COMMON METHODS ------ */

	/**
	 * De-serializes {@link JsonObject} into {@link Appointment} if possible
	 * 
	 * @param appointmentJson
	 * @return
	 */
	private Optional<Appointment> decodeAppointment(JsonObject appointmentJson) {
		if (appointmentJson == null) {
			return Optional.empty();
		}
		Appointment appointment = null;
		try {
			appointment = Json.decodeValue(appointmentJson.toBuffer(), Appointment.class);
		} catch (DecodeException e) {
			log.warn("Json {} is not Appointment object!", appointmentJson);
		}
		return Optional.ofNullable(appointment);
	}

	/**
	 * Generic method for putting header and end to {@link HttpServerResponse}
	 * 
	 * @param routingContext
	 * @param statusCode     REST APIs use the Status-Line part of an HTTP response
	 *                       message to inform clients of their request's
	 *                       overarching result. RFC 2616 defines the Status-Line
	 *                       syntax
	 * 
	 * @param contentType    determines content type (i.e text/html or
	 *                       application/json; charset=utf-8)
	 * 
	 * @param endArg         parameter to be sent as body
	 */
	private void sendResponseToClient(RoutingContext routingContext, int statusCode, String contentType,
			String endArg) {
		routingContext.response().setStatusCode(statusCode);
		routingContext.response().putHeader("Access-Control-Allow-Origin", "*");
		routingContext.response().putHeader("content-type", contentType).end(endArg);
		log.info("Response sended to {} for the query: {}", routingContext.request().remoteAddress().host(),
				routingContext.currentRoute().getPath());
	}

	/**
	 * Indicator of a service call
	 * 
	 * @param routingContext
	 */
	private void serviceCallMessage(@NonNull RoutingContext routingContext) {
		log.info("Route: '{}' is called by host: '{}'", routingContext.currentRoute().getPath(),
				routingContext.request().remoteAddress().host());
	}

	/**
	 * Main method to deploy this verticle and starts service on working host
	 * 
	 * @param noargs
	 */
	public static void main(String[] noargs) {
		Vertx vertx = Vertx.vertx();
		JsonObject serviceConf = new JsonObject().put("http.port", 8080).put("db_name", "DB_APP")
				.put("mongo_collection", "appointments");
		DeploymentOptions options = new DeploymentOptions().setConfig(serviceConf).setInstances(1);
		AppointmentService server = new AppointmentService();
		vertx.deployVerticle(server, options);
	}

}
