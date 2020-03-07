import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.json.JsonWriterSettings;
import org.junit.*;

import java.util.logging.Level;
import java.util.logging.Logger;

import static com.mongodb.client.model.Filters.*;

public class testClass {

    @Test
    public void test1(){
        Logger mongoLogger = Logger.getLogger("org.mongodb.driver");
        mongoLogger.setLevel(Level.SEVERE);
        // Creating a Mongo client
        MongoClient mongoClient = new MongoClient( "localhost" , 27017);
        System.out.println("Connected to the server successfully");

        // Accessing the database
        MongoDatabase database = mongoClient.getDatabase("chambersDB");
        System.out.printf("Database %s opened\n", database.getName());
        // Retrieving a collection
        MongoCollection<Document> collection = database.getCollection("chambers_20th_c_dictionary");
        System.out.printf("Collection %s selected\n", collection.getNamespace());

        BasicDBObject query = new BasicDBObject("word","SALAD");

        // sort results by descending number of likes
        FindIterable<Document> iterDoc = collection.find(query);

        // JasonWriter renders Bson document as formatted Json document
        JsonWriterSettings.Builder settingsBuilder = JsonWriterSettings.builder().indent(true);
        JsonWriterSettings settings = settingsBuilder.build();
        //System.out.println("Documents:"+iterDoc.toString());
        for (Document doc : iterDoc) {
            System.out.println(doc.toJson(settings));
        }

        BasicDBObject query1 = new BasicDBObject("definitions","SABURR'AL");


        // sort results by descending number of likes
        FindIterable<Document> iterDoc1 = collection.find(query1);

        // JasonWriter renders Bson document as formatted Json document
        JsonWriterSettings.Builder settingsBuilder1 = JsonWriterSettings.builder().indent(true);
        JsonWriterSettings settings1 = settingsBuilder1.build();
        //System.out.println("Documents:"+iterDoc.toString());
        for (Document doc : iterDoc1) {
            System.out.println(doc.toJson(settings1));
        }
        
        mongoClient.close();
    }

}
