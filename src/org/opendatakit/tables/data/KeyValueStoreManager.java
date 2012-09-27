package org.opendatakit.tables.data;

/**
 * This class manages the key value store. This entails maintaining two 
 * versions of the key value store--a default and an active. The default
 * is the copy that is reflected on the server. The active is the version that
 * is currently being used and modified by the phone. Information will be
 * set between them by "save as default" and a "revert to default" commands.
 * <p>
 * Implementation-wise, the manager maintains pointers to two singleton 
 * objects. One is the active key value store, and one is the default.
 * @author sudar.sam@gmail.com
 *
 */
public class KeyValueStoreManager {

}
