package pt.utl.ist.repox.statistics;

import java.util.Calendar;

public class RecordCount {
	private String dataSourceId;
	private int count;
	private int lastLineCounted;
	private Calendar lastCountDate;
	private Calendar lastCountWithChangesDate;

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public int getLastLineCounted() {
		return lastLineCounted;
	}

	public void setLastLineCounted(int lastLineCounted) {
		this.lastLineCounted = lastLineCounted;
	}

	public String getDataSourceId() {
		return dataSourceId;
	}

	public void setDataSourceId(String dataSourceId) {
		this.dataSourceId = dataSourceId;
	}

	public Calendar getLastCountDate() {
		return lastCountDate;
	}

	public void setLastCountDate(Calendar lastCountDate) {
		this.lastCountDate = lastCountDate;
	}

	public Calendar getLastCountWithChangesDate() {
		return lastCountWithChangesDate;
	}

	public void setLastCountWithChangesDate(Calendar lastCountWithChangesDate) {
		this.lastCountWithChangesDate = lastCountWithChangesDate;
	}

	public RecordCount() {

	}

	public RecordCount(String dataSourceId, int count, int lastLineCounted, Calendar lastCountDate, Calendar lastCountWithChangesDate) {
		super();
		this.dataSourceId = dataSourceId;
		this.count = count;
		this.lastLineCounted = lastLineCounted;
		this.lastCountDate = lastCountDate;
		this.lastCountWithChangesDate = lastCountWithChangesDate;
	}



}
