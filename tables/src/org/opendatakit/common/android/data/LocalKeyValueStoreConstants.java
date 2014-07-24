package org.opendatakit.common.android.data;

/**
 * Values used by the key value store that are only relevant on the device.
 * @author sudar.sam@gmail.com
 *
 */
public class LocalKeyValueStoreConstants {


  /**
   * Constants needed to use the key value store with list views.
   * @author sudar.sam@gmail.com
   *
   */
  public static class ListViews {
    /**
     * The general partition in which table-wide ListDisplayActivity information
     * is stored. An example might be the current list view for a table.
     */
    public static final String PARTITION = "ListDisplayActivity";
    /**
     * The partition under which actual individual view information is stored. For
     * instance if a user added a list view named "Doctor", the partition would be
     * KVS_PARTITION_VIEWS, and all the keys relating to this view would fall
     * within this partition and a particular aspect. (Perhaps the name "Doctor"?)
     */
    public static final String PARTITION_VIEWS = PARTITION + ".views";
    /**
     * This key holds the filename associated with the view.
     */
    public static final String KEY_FILENAME = "filename";
    /**
     * This key holds the name of the list view. In the default aspect the idea is
     * that this will then give the value of the aspect for which the default list
     * view is set.
     * <p>
     * E.g. partition=KVS_PARTITION, aspect=KVS_ASPECT_DEFAULT,
     * key="KEY_LIST_VIEW_NAME", value="My Custom List View" would mean that
     * "My Custom List View" was an aspect under the KVS_PARTITION_VIEWS partition
     * that had the information regarding a custom list view.
     */
    public static final String KEY_LIST_VIEW_NAME = "nameOfListView";
  }
}
