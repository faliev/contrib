package pt.utl.ist.repox.z3950;

import eu.europeana.repox2sip.models.DataSet;
import org.apache.log4j.Logger;
import org.dom4j.DocumentException;
import pt.utl.ist.repox.dataProvider.DataProvider;
import pt.utl.ist.repox.dataProvider.DataSource;
import pt.utl.ist.repox.dataProvider.dataSource.RecordIdPolicy;
import pt.utl.ist.repox.metadataTransformation.MetadataFormat;
import pt.utl.ist.repox.metadataTransformation.MetadataTransformation;
import pt.utl.ist.repox.recordPackage.RecordRepox;
import pt.utl.ist.repox.statistics.RecordCountManager;
import pt.utl.ist.repox.task.Task;
import pt.utl.ist.repox.task.Task.Status;
import pt.utl.ist.repox.util.RepoxContextUtil;
import pt.utl.ist.repox.util.StringUtil;
import pt.utl.ist.repox.util.TimeUtil;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DataSourceZ3950 extends DataSource {
	private static final Logger log = Logger.getLogger(DataSourceZ3950.class);
	
	private HarvestMethod harvestMethod;
	
	public HarvestMethod getHarvestMethod() {
		return harvestMethod;
	}

	public void setHarvestMethod(HarvestMethod harvestMethod) {
		this.harvestMethod = harvestMethod;
	}

	public DataSourceZ3950() {
		this.metadataFormat = MetadataFormat.MarcXchange.toString();
	}
	
	public DataSourceZ3950(DataProvider dataProvider, String id, String description, HarvestMethod harvestMethod,
			RecordIdPolicy recordIdPolicy, Map<String, MetadataTransformation> metadataTransformations) {
		super(dataProvider, id, description, MetadataFormat.MarcXchange.toString(), recordIdPolicy, metadataTransformations);
		this.harvestMethod = harvestMethod;
	}

	@Override
	public Status ingestRecords(File logFile, boolean fullIngest) throws IOException {
		Status ingestStatus = Task.Status.OK;
		
		if(harvestMethod.isFullIngestExclusive() || fullIngest) {
			boolean successfulDeletion = emptyRecords();
			
			if(!successfulDeletion) {
				StringUtil.simpleLog("Importing aborted - unable to delete the current Records", this.getClass(), logFile);
				return Task.Status.FAILED;
			}
		}
		
		try {
			StringUtil.simpleLog("Importing from Z39.50 Source: " + harvestMethod.getTarget().getAddress()
					+ " using " + harvestMethod.getClass().getSimpleName(), this.getClass(), logFile);
			
			List<RecordRepox> batchRecords = new ArrayList<RecordRepox>();
			
			harvestMethod.init();
			
			TimeUtil.getTimeSinceLastTimerArray(1);
			Iterator<RecordRepox> recordIterator = harvestMethod.getIterator(this, logFile, fullIngest);
			
			while(recordIterator.hasNext()) {
				if(stopExecution) {
					StringUtil.simpleLog("Received stop signal: exiting import.", this.getClass(), logFile);
					ingestStatus = Task.Status.CANCELED;
					
					break;
				}
				
				batchRecords.add(recordIterator.next());
				
				if(batchRecords.size() >= RECORDS_BATCH_SIZE) {
					importBatchRecords(batchRecords, logFile);
                    //pooulate the ingestedRecords list to insert later the records into the DB
                    ingestedRecords.addAll(batchRecords);
					batchRecords = new ArrayList<RecordRepox>();
				}
			}
			
			// Import remaining records
			importBatchRecords(batchRecords, logFile);
            //pooulate the ingestedRecords list to insert later the records into the DB
            ingestedRecords.addAll(batchRecords);
			batchRecords = new ArrayList<RecordRepox>();
		}
		catch(Exception e) {
			log.error("Error ingesting records", e);
			StringUtil.simpleLog("Error ingesting records " + e.getMessage(), this.getClass(), logFile);
			
			ingestStatus = Task.Status.FAILED;
		}
		finally {
			harvestMethod.cleanup();
		}
		
		return ingestStatus;
	}
	

	private void importBatchRecords(List<RecordRepox> batchRecords, File logFile) throws IOException, DocumentException, SQLException {
		long memBefore = Runtime.getRuntime().totalMemory() / (1024 * 1024);
		TimeUtil.getTimeSinceLastTimerArray(9);
		
		RecordCountManager recordCountManager = RepoxContextUtil.getRepoxManager().getRecordCountManager();
		if(recordCountManager.getRecordCount(id) != null) {
			log.debug("[BEFORE] Count: " + recordCountManager.getRecordCount(id).getCount());
		}

		RepoxContextUtil.getRepoxManager().getAccessPointsManager().processRecords(this, batchRecords);

		if(recordCountManager.getRecordCount(id) != null) {
			log.debug("[AFTER]  count: " + recordCountManager.getRecordCount(id).getCount());
		}

		double importTime = TimeUtil.getTimeSinceLastTimerArray(9) / 1000.0;
		long memAfter = Runtime.getRuntime().totalMemory() / (1024 * 1024);
		log.info(batchRecords.size() + " records imported in " + importTime + "s." +
				" Memory before/after (MB) : " + memBefore + "/"+ memAfter);
		StringUtil.simpleLog(batchRecords.size() + " records imported", this.getClass(), logFile);
	}

	@Override
	public boolean isWorking()  {
		throw new RuntimeException("Unimplemented Operation");
	}

    public DataSet createDataSetSip(){
        DataSet dataSet = new DataSet();

        dataSet.setName(this.getId());
        /*
        todo
        dataSet.setHomePage();
        dataSet.setLanguage();
        dataSet.setnameCode();
        dataSet.setProvider();
        dataSet.setQName();
        dataSet.setRequests();
        dataSet.setType();
        */

        return dataSet;
    }

}
