package gov.usdot.desktop.apps.main;

import java.net.UnknownHostException;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;
import com.mongodb.MongoException;
import com.mongodb.WriteResult;

/**
 * Clears (deletes) all records in a given MongoDB collection
 * 
 */
public class ClearMongo {

	public static void main(String[] args) {
		final CommandLineParser parser = new BasicParser();
		final Options options = new Options();
		options.addOption("h", "host", true, "The host MongoDB is running on");
		options.addOption("p", "port", true, "The port MongoDB is running on");
		options.addOption("d", "database name", true, "The name of the Mongo database");
		options.addOption("c", "collection name", true, "The name of the Mongo collection to clear");

		String host = null;
		int port = 0;
		String databaseName = null;
		String collectionName = null;

		try {
			final CommandLine commandLine = parser.parse(options, args);

			if (commandLine.hasOption('h')) {
				host = commandLine.getOptionValue('h');
				if (host == null || host.isEmpty()) {
					System.out.println("Invalid parameter: Host parameter must not be empty string");
					usage(options);
					return;
				}
			} else {
				System.out.println("Missing host parameter");
				usage(options);
				return;
			}

			if (commandLine.hasOption('p')) {
				try {
					port = Integer.parseInt(commandLine.getOptionValue('p'));
				} catch (NumberFormatException ex) {
					System.out.println("Invalid parameter: Port parameter must be an integer");
					usage(options);
					return;
				}
			} else {
				System.out.println("Missing port parameter");
				usage(options);
				return;
			}

			if (commandLine.hasOption('d')) {
				databaseName = commandLine.getOptionValue('d');
				if (databaseName == null || databaseName.isEmpty()) {
					System.out.println("Invalid parameter: Database name must not be empty string");
					usage(options);
					return;
				}
			} else {
				System.out.println("Missing database name parameter");
				usage(options);
				return;
			}

			if (commandLine.hasOption('c')) {
				collectionName = commandLine.getOptionValue('c');
				if (collectionName == null || collectionName.isEmpty()) {
					System.out.println("Invalid parameter: Collection name must not be empty string");
					usage(options);
					return;
				}
			} else {
				System.out.println("Missing collection name parameter");
				usage(options);
				return;
			}

		} catch (ParseException ex) {
			System.out.println("Command line arguments parsing failed. Reason: " + ex.getMessage());
			usage(options);
			return;
		}

		Mongo mongoClient = null;
		try {
			mongoClient = new Mongo(host, port);
			DB database = mongoClient.getDB(databaseName);
			DBCollection collection = database.getCollection(collectionName);
			BasicDBObject query = new BasicDBObject();
			WriteResult wr = collection.remove(query);
			System.out.println("Cleared " + wr.getField("n") + " records from " + collectionName);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MongoException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (mongoClient != null) {
				mongoClient.close();
			}
		}
	}

	private static void usage(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("MongoDBClient options", options);
	}
}
