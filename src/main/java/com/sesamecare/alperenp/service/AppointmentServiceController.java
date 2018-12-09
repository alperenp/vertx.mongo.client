package com.sesamecare.alperenp.service;

import java.util.Collections;
import java.util.List;

import com.sesamecare.alperenp.data.Appointment;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.MongoClient;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller class for service logic implementation.
 * <p>
 * Operates service logic
 * 
 * @author alperenp
 *
 */
@Slf4j
public class AppointmentServiceController {

	private Vertx vertx;

	private JsonObject config;

	/**
	 * Mongo collection name to be used insert/delete/update appointments
	 * <p>
	 * Default value is "appointments". See constructor
	 */
	private String COLLECTIONNAME;

	/**
	 * Controller constructor
	 * 
	 * @param vertx
	 * @param config
	 */
	public AppointmentServiceController(@NonNull Vertx vertx, @NonNull JsonObject config) {
		this.vertx = vertx;
		this.config = config;
		this.COLLECTIONNAME = config.getString("mongo_collection", "appointments");
	}

	/**
	 * 1- Deletes given {@link Appointment} (with respect to id) from mongo
	 * 
	 * @param appointmentID
	 * @return
	 */
	public Future<JsonObject> deleteOne(String appointmentID) {
		Future<JsonObject> future = Future.future();
		MongoClient mongoClient = MongoClient.createShared(vertx, config);
		mongoClient.findOneAndDelete(COLLECTIONNAME, createQueryWithID(appointmentID), asyncResult -> {
			asyncOperation(asyncResult, future);
		});
		return future;
	}

	/**
	 * 2- Inserts given {@link Appointment} to mongo
	 * 
	 * @param appointment
	 * @return
	 */
	public Future<JsonObject> insertAppointment(Appointment appointment) {
		Future<JsonObject> future = Future.future();
		MongoClient mongoClient = MongoClient.createShared(vertx, config);
		JsonObject document = new JsonObject(Json.encode(appointment));
		mongoClient.insert(COLLECTIONNAME, document, asyncResult -> {
			asyncInsertOperation(asyncResult, future);
		});
		return future;
	}

	/**
	 * 3- Replaces given appointment with the one which already exists in mongo
	 * <p>
	 * Replace is done according to Appointment.id
	 * 
	 * @param appointment
	 * @return
	 */
	public Future<JsonObject> replaceAppointment(Appointment appointment) {
		Future<JsonObject> future = Future.future();
		MongoClient mongoClient = MongoClient.createShared(vertx, config);
		JsonObject newAppointment = new JsonObject(Json.encode(appointment));
		mongoClient.findOneAndReplace(COLLECTIONNAME, createQueryWithID(appointment.getId()), newAppointment,
				asyncResult -> {
					asyncOperation(asyncResult, future);
				});
		return future;
	}

	/**
	 * 4- Finds given {@link Appointment} (with respect to id) from mongo
	 * 
	 * @param appointment
	 * @return
	 */
	public Future<List<JsonObject>> findAppointment(String appointmentID) {
		JsonObject query = createQueryWithID(appointmentID);
		FindOptions options = new FindOptions();
		return search(query, options);
	}

	/**
	 * 5- Finds given {@link Appointment} all appointments that are scheduled
	 * between a date range and sorted by price (ascending).
	 * 
	 * @param start
	 * @param end
	 * @return
	 */
	public Future<List<JsonObject>> findAppointmentsWithRangeAndSort(long start, long end) {
		JsonObject query = createQueryWithTimeRange(start, end);
		FindOptions options = ascendingPrice();
		return search(query, options);
	}

	/**
	 * 6- Returns all {@link Appointment}s those exist in mongo
	 * 
	 * @param start
	 * @param end
	 * @return
	 */
	public Future<List<JsonObject>> getAllAppointments() {
		JsonObject query = new JsonObject();
		FindOptions options = new FindOptions();
		return search(query, options);
	}

	/**
	 * 6- Deletes all {@link Appointment}s those exist in mongo
	 * 
	 * @return
	 */
	public Future<JsonObject> deleteAllAppointments() {
		JsonObject query = new JsonObject();
		Future<JsonObject> future = Future.future();
		MongoClient mongoClient = MongoClient.createShared(vertx, config);
		mongoClient.removeDocuments(COLLECTIONNAME, query, result -> {
			if (result.succeeded()) {
				JsonObject clientResponse = new JsonObject().put("result", true);
				future.complete(clientResponse);
			} else {
				JsonObject clientResponse = new JsonObject().put("result", false);
				future.complete(clientResponse);
			}
		});
		return future;
	}

	/**
	 * Generic search/find method for mongo query with {@link FindOptions}
	 * 
	 * @param query
	 * @param options
	 * @return
	 */
	private Future<List<JsonObject>> search(JsonObject query, FindOptions options) {
		Future<List<JsonObject>> future = Future.future();
		MongoClient mongoClient = MongoClient.createShared(vertx, config);
		mongoClient.findWithOptions(COLLECTIONNAME, query, options, asyncResult -> {
			asyncFindOperation(asyncResult, future);
		});
		return future;
	}

	/**
	 * After find/search, this method logs and prepares response to be sent to
	 * client
	 * 
	 * @param asyncResult
	 * @param future
	 */
	private void asyncFindOperation(AsyncResult<List<JsonObject>> asyncResult, Future<List<JsonObject>> future) {
		if (asyncResult.failed()) {
			// async operation failed
			future.fail("MongoClient failed to operate find operation!");
		} else {
			List<JsonObject> result = asyncResult.result();

			if (result == null) {
				// No entry in db
				log.info("No entry exist given query");
				future.complete(Collections.emptyList());
			} else {
				// find succeeded
				log.info("Find {} element(s) for given search", result.size());
				future.complete(result);
			}
		}
	}

	/**
	 * After insertion, this method logs and prepares response to be sent to client
	 * 
	 * @param asyncResult
	 * @param future
	 */
	private void asyncInsertOperation(AsyncResult<String> asyncResult, Future<JsonObject> future) {
		if (asyncResult.failed()) {
			// async operation failed
			future.fail("MongoClient failed to operate given operation!");
		} else {
			String result = asyncResult.result();

			// Used in response json object in order to respond to client whether their
			// request successfully completed or not
			String RESULT = "result";
			if (result == null) {
				// insertion failed
				log.info("Operation insert failed!");
				JsonObject response = new JsonObject().put(RESULT, false);
				future.complete(response);
			} else {
				// insertion succeeded
				log.info("Operation insert completed successfully for appointment");
				JsonObject response = new JsonObject().put(RESULT, true);
				future.complete(response);
			}
		}
	}

	/**
	 * After update/delete, this method logs and prepares response to sent to client
	 * 
	 * @param asyncResult
	 * @param future
	 */
	private void asyncOperation(AsyncResult<JsonObject> asyncResult, Future<JsonObject> future) {
		if (asyncResult.failed()) {
			// async operation failed
			future.fail("MongoClient failed to operate given operation!");
		} else {
			JsonObject result = asyncResult.result();

			// Used in response json object in order to respond to client whether their
			// request successfully completed or not
			String RESULT = "result";
			if (result == null) {
				// delete failed
				log.info("Operation delete/update failed!");
				JsonObject response = new JsonObject().put(RESULT, false);
				future.complete(response);
			} else {
				// delete succeeded
				log.info("Operation delete/update completed successfully for appointment");
				JsonObject response = new JsonObject().put(RESULT, true);
				future.complete(response);
			}
		}
	}

	/**
	 * Create query with respect to id
	 * 
	 * @param id
	 * @return
	 */
	private JsonObject createQueryWithID(String id) {
		return new JsonObject().put("id", id);
	}

	/**
	 * Creates mongo query with respect to given range of appointmentDate.
	 * <p>
	 * This query also takes into account whether appointsments are booked
	 * 
	 * @param start
	 * @param end
	 * @return
	 */
	private JsonObject createQueryWithTimeRange(long start, long end) {
		JsonObject query = new JsonObject();
		JsonObject innerQuery = new JsonObject();
		innerQuery.put("$gte", start);
		innerQuery.put("$lte", end);
		query.put("appointmentDate", innerQuery);
		query.put("status", Appointment.Status.BOOKED.toString());
		return query;
	}

	/**
	 * Creates {@link FindOptions} for mongo query in order to sort result with
	 * respect to price
	 * 
	 * @return
	 */
	private FindOptions ascendingPrice() {
		FindOptions options = new FindOptions();
		// +1 from lower to higher, -1 reverse
		options.setSort(new JsonObject().put("price", 1));
		return options;
	}
}
