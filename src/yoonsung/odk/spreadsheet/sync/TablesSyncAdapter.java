package yoonsung.odk.spreadsheet.sync;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.client.ClientProtocolException;
import org.opendatakit.aggregate.odktables.client.api.SynchronizeAPI;
import org.opendatakit.aggregate.odktables.client.exception.AggregateInternalErrorException;
import org.opendatakit.aggregate.odktables.client.exception.ColumnDoesNotExistException;
import org.opendatakit.aggregate.odktables.client.exception.OutOfSynchException;
import org.opendatakit.aggregate.odktables.client.exception.PermissionDeniedException;
import org.opendatakit.aggregate.odktables.client.exception.RowOutOfSynchException;
import org.opendatakit.aggregate.odktables.client.exception.TableAlreadyExistsException;
import org.opendatakit.aggregate.odktables.client.exception.TableDoesNotExistException;
import org.opendatakit.aggregate.odktables.client.exception.UserDoesNotExistException;

import yoonsung.odk.spreadsheet.data.DbHelper;
import yoonsung.odk.spreadsheet.data.Preferences;
import yoonsung.odk.spreadsheet.sync.aggregate.AggregateSyncProcessor;
import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;

public class TablesSyncAdapter extends AbstractThreadedSyncAdapter
{
    private static final String TAG = "TablesSyncAdapter";

    private final Context context;

    public TablesSyncAdapter(Context context, boolean autoInitialize)
    {
        super(context, autoInitialize);
        this.context = context;
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
            ContentProviderClient provider, SyncResult syncResult)
    {
		try {
			
	    	Preferences prefs = new Preferences(this.context);
	    	String aggregateURIString = prefs.getAggregateUri();
	    	String username = prefs.getAggregateUsername();
	    	if (aggregateURIString != null && username != null)
	    	{
		    	URI aggregateURI = new URI(aggregateURIString);
				SynchronizeAPI api = new SynchronizeAPI(aggregateURI, username);
		    	DbHelper helper = DbHelper.getDbHelper(context);
		    	AggregateSyncProcessor processor = new AggregateSyncProcessor(api, helper);
		    	processor.synchronize(1);
	    	}
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		} catch (UserDoesNotExistException e) {
			e.printStackTrace();
		} catch (AggregateInternalErrorException e) {
			e.printStackTrace();
		} catch (IOException e) {
			syncResult.stats.numIoExceptions++;
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TableAlreadyExistsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (OutOfSynchException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TableDoesNotExistException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (PermissionDeniedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ColumnDoesNotExistException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RowOutOfSynchException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

}
