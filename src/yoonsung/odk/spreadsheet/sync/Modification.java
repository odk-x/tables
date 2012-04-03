package yoonsung.odk.spreadsheet.sync;

import java.util.Map;

public class Modification {
	Map<String, String> syncTags;
	String tableSyncTag;
	
	@java.lang.SuppressWarnings("all")
	public Modification(final Map<String, String> syncTags, final String tableSyncTag) {
		this.syncTags = syncTags;
		this.tableSyncTag = tableSyncTag;
	}
	
	@java.lang.SuppressWarnings("all")
	public Modification() {
	}
	
	@java.lang.SuppressWarnings("all")
	public Map<String, String> getSyncTags() {
		return this.syncTags;
	}
	
	@java.lang.SuppressWarnings("all")
	public String getTableSyncTag() {
		return this.tableSyncTag;
	}
	
	@java.lang.SuppressWarnings("all")
	public void setSyncTags(final Map<String, String> syncTags) {
		this.syncTags = syncTags;
	}
	
	@java.lang.SuppressWarnings("all")
	public void setTableSyncTag(final String tableSyncTag) {
		this.tableSyncTag = tableSyncTag;
	}
	
	@java.lang.Override
	@java.lang.SuppressWarnings("all")
	public boolean equals(final java.lang.Object o) {
		if (o == this) return true;
		if (!(o instanceof Modification)) return false;
		final Modification other = (Modification)o;
		if (!other.canEqual((java.lang.Object)this)) return false;
		if (this.getSyncTags() == null ? other.getSyncTags() != null : !this.getSyncTags().equals((java.lang.Object)other.getSyncTags())) return false;
		if (this.getTableSyncTag() == null ? other.getTableSyncTag() != null : !this.getTableSyncTag().equals((java.lang.Object)other.getTableSyncTag())) return false;
		return true;
	}
	
	@java.lang.SuppressWarnings("all")
	public boolean canEqual(final java.lang.Object other) {
		return other instanceof Modification;
	}
	
	@java.lang.Override
	@java.lang.SuppressWarnings("all")
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = result * PRIME + (this.getSyncTags() == null ? 0 : this.getSyncTags().hashCode());
		result = result * PRIME + (this.getTableSyncTag() == null ? 0 : this.getTableSyncTag().hashCode());
		return result;
	}
	
	@java.lang.Override
	@java.lang.SuppressWarnings("all")
	public java.lang.String toString() {
		return "Modification(syncTags=" + this.getSyncTags() + ", tableSyncTag=" + this.getTableSyncTag() + ")";
	}
}