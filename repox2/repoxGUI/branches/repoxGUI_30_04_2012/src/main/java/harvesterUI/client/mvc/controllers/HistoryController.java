package harvesterUI.client.mvc.controllers;

import com.extjs.gxt.ui.client.event.EventType;
import com.extjs.gxt.ui.client.mvc.AppEvent;
import com.extjs.gxt.ui.client.mvc.Controller;
import com.google.gwt.user.client.History;
import harvesterUI.client.HarvesterUI;
import harvesterUI.client.core.AppEvents;
import harvesterUI.shared.dataTypes.AggregatorUI;
import harvesterUI.shared.dataTypes.DataProviderUI;
import harvesterUI.shared.dataTypes.DataSourceUI;

/**
 * Created to REPOX.
 * User: Edmundo
 * Date: 14-04-2011
 * Time: 20:14
 */
public class HistoryController extends Controller {

    public HistoryController() {
        registerEventTypes(AppEvents.ViewSchemaMapper);
        registerEventTypes(AppEvents.ViewStatistics);
        registerEventTypes(AppEvents.LoadMainData);
//        registerEventTypes(AppEvents.ViewDPImportForm);
        registerEventTypes(AppEvents.ViewOAITest);
        registerEventTypes(AppEvents.ViewScheduledTasksCalendar);
        registerEventTypes(AppEvents.ViewScheduledTasksList);
        registerEventTypes(AppEvents.ViewRunningTasksList);
        registerEventTypes(AppEvents.ViewAdminForm);
        registerEventTypes(AppEvents.ViewUserManagementForm);
        registerEventTypes(AppEvents.ViewRestRecordOperations);
//        registerEventTypes(AppEvents.ViewAccountEditForm);
//        registerEventTypes(AppEvents.ViewAggregatorForm);
//        registerEventTypes(AppEvents.ViewDataProviderForm);
        registerEventTypes(AppEvents.ViewDataSetInfo);
        registerEventTypes(AppEvents.ViewServiceManager);
        registerEventTypes(AppEvents.ViewRssFeedPanel);
    }

    public void handleEvent(AppEvent event) {
        EventType type = event.getType();
        if (type == AppEvents.LoadMainData) {
            History.newItem("OVERVIEW_GRID",false);
        }else if (type == AppEvents.ViewStatistics) {
            History.newItem("STATISTICS",false);
//        }else if (type == AppEvents.ViewDPImportForm) {
//            History.newItem("IMPORT_DATA_PROVIDER",false);
        }else if (type == AppEvents.ViewSchemaMapper) {
            History.newItem("SCHEMA_MAPPER",false);
        }else if (type == AppEvents.ViewOAITest) {
            History.newItem("OAI_TESTS",false);
        }else if (type == AppEvents.ViewScheduledTasksCalendar) {
            History.newItem("CALENDAR",false);
        }else if (type == AppEvents.ViewScheduledTasksList) {
            History.newItem("SCHEDULED_TASKS",false);
        }else if (type == AppEvents.ViewRunningTasksList) {
            History.newItem("RUNNING_TASKS",false);
        }else if (type == AppEvents.ViewAdminForm) {
            History.newItem("ADMIN_CONFIG",false);
        }else if (type == AppEvents.ViewUserManagementForm) {
            History.newItem("USER_MANAGEMENT",false);
        }else if (type == AppEvents.ViewRestRecordOperations) {
            History.newItem("REST_OPERATIONS",false);
        }else if (type == AppEvents.ViewAccountEditForm) {
            History.newItem("EDIT_ACCOUNT",false);
        }else if (type == AppEvents.ViewServiceManager) {
            History.newItem("EXTERNAL_SERVICES_MANAGER",false);
        }else if (type == AppEvents.ViewRssFeedPanel) {
            History.newItem("RSS_PANEL",false);
        }

        // functions with parameters
        else if (type == AppEvents.ViewAggregatorForm) {
            if(event.getData() instanceof AggregatorUI) {
                AggregatorUI aggregatorUI = (AggregatorUI) event.getData();
                History.newItem("EDIT_AGG?id=" + aggregatorUI.getName(),false);
            } else
                History.newItem("CREATE_AGGREGATOR",false);
        }else if (type == AppEvents.ViewDataProviderForm) {
            if(HarvesterUI.getProjectType().equals("EUROPEANA")) {
                if(event.getData() instanceof DataProviderUI) {
                    DataProviderUI dataProviderUI = (DataProviderUI) event.getData();
                    History.newItem("EDIT_DP?id=" + dataProviderUI.getName(),false);
                } else {
                    AggregatorUI aggregatorUI = (AggregatorUI) event.getData();
                    History.newItem("CREATE_DATA_PROVIDER?aggregatorName=" + aggregatorUI.getName(),false);
                }
            } else {
                if(event.getData() instanceof DataProviderUI) {
                    DataProviderUI dataProviderUI = (DataProviderUI) event.getData();
                    History.newItem("EDIT_DP?id=" + dataProviderUI.getName(),false);
                } else
                    History.newItem("CREATE_DATA_PROVIDER",false);
            }
        }else if (type == AppEvents.ViewDataSetInfo) {
            if(event.getData() instanceof DataSourceUI) {
                DataSourceUI dataSourceUI = (DataSourceUI) event.getData();
                History.newItem("VIEW_DS?id=" + dataSourceUI.getDataSourceSet(),false);
            }
        }
    }

    public void initialize() {
//        accountEditView = new AccountEditView(this);
    }

    private void onInit(AppEvent event) {
//        forwardToView(accountEditView, event);
    }
}
