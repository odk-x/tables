package yoonsung.odk.spreadsheet.sync;

import java.util.ArrayList;
import java.util.List;

public class IncomingModification {
	List<SyncRow> rows;
	String tableSyncTag;
	
	public IncomingModification() {
		this.rows = new ArrayList<SyncRow>();
		this.tableSyncTag = null;
	}
	
	@java.lang.SuppressWarnings("all")
	public IncomingModification(final List<SyncRow> rows, final String tableSyncTag) {
		this.rows = rows;
		this.tableSyncTag = tableSyncTag;
	}
	
	@java.lang.SuppressWarnings("all")
	public List<SyncRow> getRows() {
		return this.rows;
	}
	
	@java.lang.SuppressWarnings("all")
	public String getTableSyncTag() {
		return this.tableSyncTag;
	}
	
	@java.lang.SuppressWarnings("all")
	public void setRows(final List<SyncRow> rows) {
		this.rows = rows;
	}
	
	@java.lang.SuppressWarnings("all")
	public void setTableSyncTag(final String tableSyncTag) {
		this.tableSyncTag = tableSyncTag;
	}
	
	@java.lang.Override
	@java.lang.SuppressWarnings("all")
	public boolean equals(final java.lang.Object o) {
		if (o == this) return true;
		if (!(o instanceof IncomingModification)) return false;
		final IncomingModification other = (IncomingModification)o;
		if (!other.canEqual((java.lang.Object)this)) return false;
		if (this.getRows() == null ? other.getRows() != null : !this.getRows().equals((java.lang.Object)other.getRows())) return false;
		if (this.getTableSyncTag() == null ? other.getTableSyncTag() != null : !this.getTableSyncTag().equals((java.lang.Object)other.getTableSyncTag())) return false;
		return true;
	}
	
	@java.lang.SuppressWarnings("all")
	public boolean canEqual(final java.lang.Object other) {
		return other instanceof IncomingModification;
	}
	
	@java.lang.Override
	@java.lang.SuppressWarnings("all")
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = result * PRIME + (this.getRows() == null ? 0 : this.getRows().hashCode());
		result = result * PRIME + (this.getTableSyncTag() == null ? 0 : this.getTableSyncTag().hashCode());
		return result;
	}
	
	@java.lang.Override
	@java.lang.SuppressWarnings("all")
	public java.lang.String toString() {
		return "IncomingModification(rows=" + this.getRows() + ", tableSyncTag=" + this.getTableSyncTag() + ")";
	}
}