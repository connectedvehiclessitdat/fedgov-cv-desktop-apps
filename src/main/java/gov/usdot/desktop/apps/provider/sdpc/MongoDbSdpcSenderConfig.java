package gov.usdot.desktop.apps.provider.sdpc;

import gov.usdot.asn1.generated.j2735.semi.GroupID;
import gov.usdot.cv.common.asn1.GroupIDHelper;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import net.sf.json.JSONObject;

import org.apache.commons.io.FileUtils;

import com.mongodb.BasicDBObject;
import com.mongodb.util.JSON;

public class MongoDbSdpcSenderConfig {
	
	public final Source source;
	public final Destination destination;
	public final Trust trust;
	public final Other other;
	
	public MongoDbSdpcSenderConfig(JSONObject config) throws IOException {
		source = new Source(config);
		destination = new Destination(config);
		trust = new Trust(config);
		other = new Other(config);
	}
	
	public class Source {
		
		static private final String SECTION_NAME = "source";
		static private final String DEFAULT_HOST = "localhost";
		private static final int DEFAULT_PORT = 27017;
		private static final String DEFAULT_DBNAME = "cvdb";
		private static final String DEFAULT_COL_NAME = "vehSitDataMessage";
		private static final String DEFAULT_QUERY = "{ \"$query\": { }, \"$orderby\": { \"deCreatedAt\" : -1 } }";
		private static final int DEFAULT_SKIP = 0;
		private static final int DEFAULT_LIMIT = -1;
		
		public final String host;
		public final int port;
		public final String database;
		public final String collection;
		public BasicDBObject query;
		public final int skip;
		public final int limit;
		
		private Source(JSONObject config) throws IOException {
			JSONObject source = config.has(SECTION_NAME) ? config.getJSONObject(SECTION_NAME) : new JSONObject();

			host = source.optString("host", DEFAULT_HOST);
			port = source.optInt("port", DEFAULT_PORT);
			database = source.optString("database", DEFAULT_DBNAME);
			collection = source.optString("collection", DEFAULT_COL_NAME);
			skip = source.optInt("skip", DEFAULT_SKIP);
			limit = source.optInt("limit", DEFAULT_LIMIT);
			
			final String queryStr;
			if ( source.has("queryStr") ) {									// queryStr	 -- MongoDB query as string value
				queryStr = source.getString("queryStr");
			} else if ( source.has("query") ) {								// query	 -- in-line MongoDB query
				JSONObject queryJson = source.getJSONObject("query");
				queryStr = queryJson.toString();
			} else if ( source.has("queryFile") ) {							// queryFile -- file name to read the query from
				final String fileName = source.getString("queryFile");
				queryStr = FileUtils.readFileToString(new File(fileName));
			} else {
				queryStr = DEFAULT_QUERY;
			}
			query = (BasicDBObject) JSON.parse(queryStr);
		}
	}
	
	public class Destination {
		static private final String SECTION_NAME = "destination";
		static private final String DEFAULT_HOST = "localhost";
		private static final int DEFAULT_PORT = 46751;
		private static final int DEFAULT_REPLY_PORT = DEFAULT_PORT + 1;
		
		public final boolean send;
		public final InetAddress host;
		public final int port;
		public final int replyPort;
		
		private Destination(JSONObject config) throws UnknownHostException {
			JSONObject destination;
			if ( config.has(SECTION_NAME) ) {
				send = true;
				destination = config.getJSONObject(SECTION_NAME);
			} else {
				send = false;
				destination = new JSONObject();
			}
			final String hostname = destination.optString("host", DEFAULT_HOST);
			host = InetAddress.getByName(hostname);
			port = destination.optInt("port", DEFAULT_PORT);
			replyPort = destination.optInt("replyPort", DEFAULT_REPLY_PORT);
		}
	}
	
	public class Trust {
		static private final String SECTION_NAME = "trust";
		static private final boolean DEFAULT_IGNORE = true;
		private static final int DEFAULT_ATTEMPTS = 3;
		private static final int DEFAULT_TIMEOUT = 3000;
		
		public final boolean establish;
		public final boolean ignore;
		public final int attempts;
		public final int timeout;
		
		private Trust(JSONObject config) {
			JSONObject trust;
			if ( config.has(SECTION_NAME)  ) {
				establish = true;
				trust = config.getJSONObject(SECTION_NAME);;
			} else {
				establish = false;
				trust = new JSONObject();
			}
			ignore = trust.optBoolean("ignore", DEFAULT_IGNORE);
			attempts = trust.optInt("attempts", DEFAULT_ATTEMPTS);
			timeout = trust.optInt("timeout", DEFAULT_TIMEOUT);
		}
	}
	
	public class Other {
		static private final String SECTION_NAME = "other";
		static private final boolean DEFAULT_VERBOSE = false;
		private static final int DEFAULT_DELAY = 500;
		static private final String PROCESSOR_NAME = "processor";
		static private final int DEFAULT_GROUP_ID = 0;
		
		public final boolean verbose;
		public final int delay;
		public final String processor;
		public final JSONObject processor_config;
		public final GroupID groupID;
		
		private Other(JSONObject config) {
			JSONObject other = config.has(SECTION_NAME) ? config.getJSONObject(SECTION_NAME) : new JSONObject();
			verbose = other.optBoolean("verbose", DEFAULT_VERBOSE);
			delay = other.optInt("delay", DEFAULT_DELAY);
			groupID = GroupIDHelper.toGroupID(other.optInt("groupId", DEFAULT_GROUP_ID));
			if ( other.has(PROCESSOR_NAME) ) {
				JSONObject processorInfo = other.getJSONObject(PROCESSOR_NAME);
				processor = processorInfo.optString("className");
				processor_config = processorInfo.optJSONObject("config");
			} else {
				processor = null;
				processor_config = null;
			}
		}

	}
}
