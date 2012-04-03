package yoonsung.odk.spreadsheet.sync;

import java.util.Map;

public class SyncRow {
	private String rowId;
	private String syncTag;
	private boolean deleted;
	private Map<String, String> values;
	
	@java.lang.SuppressWarnings("all")
	public SyncRow(final String rowId, final String syncTag, final boolean deleted, final Map<String, String> values) {
		this.rowId = rowId;
		this.syncTag = syncTag;
		this.deleted = deleted;
		this.values = values;
	}
	
	@java.lang.SuppressWarnings("all")
	public String getRowId() {
		return this.rowId;
	}
	
	@java.lang.SuppressWarnings("all")
	public String getSyncTag() {
		return this.syncTag;
	}
	
	@java.lang.SuppressWarnings("all")
	public boolean isDeleted() {
		return this.deleted;
	}
	
	@java.lang.SuppressWarnings("all")
	public Map<String, String> getValues() {
		return this.values;
	}
	
	@java.lang.SuppressWarnings("all")
	public void setRowId(final String rowId) {
		this.rowId = rowId;
	}
	
	@java.lang.SuppressWarnings("all")
	public void setSyncTag(final String syncTag) {
		this.syncTag = syncTag;
	}
	
	@java.lang.SuppressWarnings("all")
	public void setDeleted(final boolean deleted) {
		this.deleted = deleted;
	}
	
	@java.lang.SuppressWarnings("all")
	public void setValues(final Map<String, String> values) {
		this.values = values;
	}
	
	@java.lang.Override
	@java.lang.SuppressWarnings("all")
	public boolean equals(final java.lang.Object o) {
		if (o == this) return true;
		if (!(o instanceof SyncRow)) return false;
		final SyncRow other = (SyncRow)o;
		if (!other.canEqual((java.lang.Object)this)) return false;
		if (this.getRowId() == null ? other.getRowId() != null : !this.getRowId().equals((java.lang.Object)other.getRowId())) return false;
		if (this.getSyncTag() == null ? other.getSyncTag() != null : !this.getSyncTag().equals((java.lang.Object)other.getSyncTag())) return false;
		if (this.isDeleted() != other.isDeleted()) return false;
		if (this.getValues() == null ? other.getValues() != null : !this.getValues().equals((java.lang.Object)other.getValues())) return false;
		return true;
	}
	
	@java.lang.SuppressWarnings("all")
	public boolean canEqual(final java.lang.Object other) {
		return other instanceof SyncRow;
	}
	
	@java.lang.Override
	@java.lang.SuppressWarnings("all")
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = result * PRIME + (this.getRowId() == null ? 0 : this.getRowId().hashCode());
		result = result * PRIME + (this.getSyncTag() == null ? 0 : this.getSyncTag().hashCode());
		result = result * PRIME + (this.isDeleted() ? 1231 : 1237);
		result = result * PRIME + (this.getValues() == null ? 0 : this.getValues().hashCode());
		return result;
	}
	
	@java.lang.Override
	@java.lang.SuppressWarnings("all")
	public java.lang.String toString() {
		return "SyncRow(rowId=" + this.getRowId() + ", syncTag=" + this.getSyncTag() + ", deleted=" + this.isDeleted() + ", values=" + this.getValues() + ")";
	}
}