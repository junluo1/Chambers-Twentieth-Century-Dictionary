import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NewWorldDictionaryReader {



	/**
	 * Read the definition into the defStr buffer up to defSize characters.
	 * Subsequent characters are ignored.
	 *
	 * @return true of the definition was read, false if there was an error
	 * @throws IOException if error reading definition
	 */
	static String readDefinition(BufferedReader br, StringBuffer def) throws IOException {
		for (String s = br.readLine(); s != null; s = br.readLine()) {
			if (s.trim().length() == 0) {
				break;
			}
			if (def.length() > 0) {
				def.append(" ");
			}
			def.append(s);
		}
		return def.toString();
	}

	/**
	 * Returns next definition from reader.
	 * 
	 * @param br the reader
	 * @return the next definition
	 * @throws IOException if I/O errors
	 */
	static String readDefinition(BufferedReader br) throws IOException {
	    // return first definition
	    return readDefinition(br, new StringBuffer());
	}

	/**
	 * Find and return first definition from input stream. A definition is a
	 * sequence of lines terminated by an empty line. The first word in the
	 * definition followed by a comma is the definition
	 *
	 * @param firstWord the first word whose definition is added to the map
	 * @return the definition string
	 * @throws IOException if I/O errors
	 */
	static String readDefinition(BufferedReader br, String firstWord) throws IOException {
	    String matchFirstWordComma = firstWord + ", ";
	    String matchFirstWordPeriod = firstWord + ". ";

	    // return definition for firstWord
	    String line;
	    while ((line = br.readLine()) != null) {
	        if (   line.startsWith(matchFirstWordComma) 
	        		|| line.startsWith(matchFirstWordPeriod)) {
	        		return readDefinition(br, new StringBuffer(line));
	        }
	    }
	    return null;
	}
	
	/**
	 * Find word delimited by '.' or ','.
	 * @param s the string starting with word
	 * @return the index of the '.' or ',' delimiting char
	 */
	static int findWord(String s) {
		int i = s.indexOf(". ");
		int j = s.indexOf(", ");
		if (i > 0 && j > 0) return Math.min(i, j);
		return (i < 0) ? j : i;
	}


	/**
	 * Reads definitions from New World Dictionary on Project Gutenberg
	 * @param args first and last word if two arguments specified.
	 */
	public static void main(String[] args) {
		/**
		 * Q1: connect to mongoDB
		 */
		Logger mongoLogger = Logger.getLogger("org.mongodb.driver");
		mongoLogger.setLevel(Level.SEVERE);

		// Creating a Mongo client
		MongoClient mongoClient = new MongoClient( "localhost" , 27017);
		System.out.println("Connected to the server successfully");

		// Accessing the database
		MongoDatabase database = mongoClient.getDatabase("chambersDB");
		System.out.printf("Database %s opened\n", database.getName());
		database.drop();

		// Retrieving a collection
		MongoCollection<Document> collection = database.getCollection("chambers_20th_c_dictionary");
		System.out.printf("Collection %s selected\n", collection.getNamespace());


		String urlStr = "http://www.gutenberg.org/cache/epub/38700/pg38700.txt";
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(new URL(urlStr).openStream()));
		} catch (MalformedURLException e) {
			System.err.println(e.toString());
			System.exit(1);
		} catch (IOException e) {
			System.err.println(e.toString());
			System.exit(1);
		}
		
		// process from first word to last word
		String firstWord = "SAB";
		String lastWord = "SYZYGY";
		if (args.length == 2) {
			firstWord = args[0];
			lastWord = args[1];
		}
		
		int defCount = 0;
		try {
			String def = readDefinition(br, firstWord);
			while (def != null) {
				String pronounce = null;
				String note = null;
				String derivation = null;
				String[] defs = null;
				String word = null;
				// look for word
				int endWord = findWord(def);
				if (endWord > 0) {
					// found word
					word = def.substring(0, endWord).trim();
					char punct = def.charAt(endWord);
					def = def.substring(endWord+1);

					if (punct == '.') {
						// treat rest of def as a note
						note = def;
					} else {
						// look for pronunciation
						endWord = findWord(def);
						if (endWord > 0) {
							// found pronunciation
							pronounce = def.substring(0, endWord).trim();
							punct = def.charAt(endWord);
							def = def.substring(endWord+1);

							if (punct == '.') {
								// treat rest of def as a note
								note = def;
							} else {
								// find derivation at end of def
								if (def.endsWith("]")) {
									// find start of origin at '[]
									int i = def.length()-1;
									do {
										// skip over [=A]...
										i = def.lastIndexOf('[', i-1);
									} while (i > 0 && def.charAt(i+1) == '=');
									if (i > 0) {
										// found derivation
										derivation = def.substring(i+1, def.length()-1).trim();
										def = def.substring(0,i);
									}
								}
								
								// split out into separate definitions at '--' following '.' 
								defs = def.replaceAll("\\.--", "..--").split("\\.--");
								for (int i = 0; i < defs.length; i++) {
									// ensure all fields are trimmed
									defs[i] = defs[i].trim();
								}
							}
						}
					}
					
					defCount++;
					
				}

				/**
				 * Q2
				 * create a new Document for the word
				 * if not null, add: pronounciation, note, derivation, and definitions
				 * insert the document into the collection
				 */
				Document currentDoc = new Document().append("word",word);
				if(pronounce!=null)currentDoc.append("pronouciation",pronounce);
				if(note!=null)currentDoc.append("note",note);
				if(derivation!=null)currentDoc.append("derivation",derivation);
				if(defs!=null)currentDoc.append("definitions", Arrays.asList(defs));
				collection.insertOne(currentDoc);

				// done if last word processed
				if (lastWord.equals(word)) {
					break;
				}
				// get next definition
				def = readDefinition(br);
			}
			System.out.printf(
				"Processed %d definitions from '%s' to '%s'.\n", defCount, firstWord, lastWord);
		} catch (IOException e) {
			System.err.println(e.toString());
			System.exit(1);
		}

		/**
		 * Q3
		 * create a BasicDBObject representing the index
		 * put the word, definitions, and derivation fields as "text" indexes to the BasicDBObject
		 * use the BasicDBObject to create an index for the collection
		 */
		BasicDBObject index = new BasicDBObject()
				.append("word","text")
				.append(" definitions","text")
				.append("derivation", "text");
		collection.createIndex(index);
	}
}
