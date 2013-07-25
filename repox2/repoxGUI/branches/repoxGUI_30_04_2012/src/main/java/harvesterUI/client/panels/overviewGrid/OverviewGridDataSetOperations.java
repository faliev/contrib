package harvesterUI.client.panels.overviewGrid;

import com.extjs.gxt.ui.client.Registry;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.MenuEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.mvc.Dispatcher;
import com.extjs.gxt.ui.client.widget.Component;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.menu.Menu;
import com.extjs.gxt.ui.client.widget.menu.MenuItem;
import com.extjs.gxt.ui.client.widget.toolbar.ToolBar;
import com.extjs.gxt.ui.client.widget.treegrid.TreeGrid;
import harvesterUI.client.HarvesterUI;
import harvesterUI.client.core.AppEvents;
import harvesterUI.client.panels.dataSetButtons.ManageDataSetMenu;
import harvesterUI.client.panels.harvesting.dialogs.ExportNowDialog;
import harvesterUI.client.panels.harvesting.dialogs.ScheduleExportDialog;
import harvesterUI.client.panels.harvesting.dialogs.ScheduleTaskDialog;
import harvesterUI.client.panels.overviewGrid.contextMenus.DataSetContextMenu;
import harvesterUI.client.servlets.dataManagement.DataSetOperationsServiceAsync;
import harvesterUI.client.util.GridOperations;
import harvesterUI.shared.dataTypes.DataContainer;
import harvesterUI.shared.dataTypes.DataSourceUI;

/**
 * Created to REPOX.
 * User: Edmundo
 * Date: 24-11-2011
 * Time: 15:53
 */
public class OverviewGridDataSetOperations extends GridOperations {

    protected DataSetOperationsServiceAsync service;
    private TreeGrid<DataContainer> tree;

    public OverviewGridDataSetOperations(TreeGrid<DataContainer> mainTree) {
        service = (DataSetOperationsServiceAsync) Registry.get(HarvesterUI.DATA_SET_OPERATIONS_SERVICE);
        tree = mainTree;

        /*
         * Listeners
         */
        final SelectionListener<ButtonEvent> emptyDataSetListener = new SelectionListener<ButtonEvent> () {
            public void componentSelected(ButtonEvent ce) {
                Dispatcher.forwardEvent(AppEvents.EmptyDataSet,
                        DataSetContextMenu.getOnlyDataSourceUIs(tree.getSelectionModel().getSelectedItems()));
            }
        };

        /*
         * Buttons
         */

        // Harvest Menu
        Button harvest = new Button();
        harvest.setText(HarvesterUI.CONSTANTS.harvest());
        harvest.setIcon(HarvesterUI.ICONS.harvest_menu_icon());
        Menu harvestMenu = new Menu();
        harvest.setMenu(harvestMenu);

        MenuItem empty = new MenuItem();
        empty.setText(HarvesterUI.CONSTANTS.emptyDataSet());
        empty.setIcon(HarvesterUI.ICONS.broom_icon());
        empty.addSelectionListener(new SelectionListener<MenuEvent>() {
            @Override
            public void componentSelected(MenuEvent me) {
                HarvesterUI.UTIL_MANAGER.createConfirmMessageBox(HarvesterUI.CONSTANTS.confirm(), HarvesterUI.CONSTANTS.emptyDataSetMessage(), emptyDataSetListener);
            }
        });

        Button ingestNow = new Button();
        ingestNow.setText(HarvesterUI.CONSTANTS.ingestNow());
        ingestNow.setIcon(HarvesterUI.ICONS.ingest_now_icon());
        ingestNow.addSelectionListener(new SelectionListener<ButtonEvent>() {
            @Override
            public void componentSelected(ButtonEvent me) {
                Dispatcher.forwardEvent(AppEvents.IngestDataSet,
                        DataSetContextMenu.getOnlyDataSourceUIs(tree.getSelectionModel().getSelectedItems()));
            }
        });

        MenuItem ingestSample = new MenuItem();
        ingestSample.setText(HarvesterUI.CONSTANTS.ingestSample());
        ingestSample.setIcon(HarvesterUI.ICONS.ingest_sample_icon());
        ingestSample.addSelectionListener(new SelectionListener<MenuEvent>() {
            @Override
            public void componentSelected(MenuEvent me) {
                Dispatcher.forwardEvent(AppEvents.IngestDataSetSample,
                        DataSetContextMenu.getOnlyDataSourceUIs(tree.getSelectionModel().getSelectedItems()));
            }
        });

        MenuItem scheduleIngest = new MenuItem();
        scheduleIngest.setText(HarvesterUI.CONSTANTS.scheduleIngest());
        scheduleIngest.setIcon(HarvesterUI.ICONS.calendar());
        scheduleIngest.addSelectionListener(new SelectionListener<MenuEvent>() {
            @Override
            public void componentSelected(MenuEvent ce) {
                DataSourceUI selectedDS =
                        DataSetContextMenu.getOnlyDataSourceUIs(tree.getSelectionModel().getSelectedItems()).get(0);
                ScheduleTaskDialog scheduleTaskDialog = new ScheduleTaskDialog(selectedDS.getDataSourceSet());
            }
        });

        MenuItem scheduleExport = new MenuItem();
        scheduleExport.setText(HarvesterUI.CONSTANTS.scheduleExport());
        scheduleExport.setIcon(HarvesterUI.ICONS.schedule_export_icon());
        scheduleExport.addSelectionListener(new SelectionListener<MenuEvent>() {
            @Override
            public void componentSelected(MenuEvent ce) {
                DataSourceUI selectedDS =
                        DataSetContextMenu.getOnlyDataSourceUIs(tree.getSelectionModel().getSelectedItems()).get(0);
                ScheduleExportDialog scheduleExportDialog = new ScheduleExportDialog(selectedDS.getDataSourceSet());
                scheduleExportDialog.setModal(true);
                scheduleExportDialog.resetValues();
                scheduleExportDialog.show();
                scheduleExportDialog.center();
            }
        });

        MenuItem exportNow = new MenuItem();
        exportNow.setText(HarvesterUI.CONSTANTS.exportNow());
        exportNow.setIcon(HarvesterUI.ICONS.export_now_icon());
        exportNow.addSelectionListener(new SelectionListener<MenuEvent>() {
            @Override
            public void componentSelected(MenuEvent ce) {
                DataSourceUI selectedDS =
                        DataSetContextMenu.getOnlyDataSourceUIs(tree.getSelectionModel().getSelectedItems()).get(0);
                ExportNowDialog exportNowDialog = new ExportNowDialog(selectedDS);
                exportNowDialog.setModal(true);
                exportNowDialog.show();
                exportNowDialog.center();
            }
        });

        harvestMenu.add(empty);
        harvestMenu.add(ingestSample);
        harvestMenu.add(scheduleIngest);
        harvestMenu.add(scheduleExport);
        harvestMenu.add(exportNow);

        // Management part

        Button viewInfo = new Button();
        viewInfo.setId("viewInfoButton");
        viewInfo.setText(HarvesterUI.CONSTANTS.viewInfo());
        viewInfo.setIcon(HarvesterUI.ICONS.view_info_icon());
        viewInfo.addSelectionListener(new SelectionListener<ButtonEvent>() {
            public void componentSelected(ButtonEvent ce) {
                DataSourceUI selected = DataSetContextMenu.getOnlyDataSourceUIs(tree.getSelectionModel().getSelectedItems()).get(0);
                Dispatcher.get().dispatch(AppEvents.ViewDataSetInfo,selected);
            }
        });

        componentList.add(viewInfo);
        new ManageDataSetMenu(tree,componentList);
        componentList.add(ingestNow);
        componentList.add(harvest);

        createSeparator();
    }

    public void hideButtons(ToolBar toolBar) {
        for(Component component : componentList) {
            if(toolBar.getItems().contains(component))
                component.removeFromParent();
        }
    }

    public void showButtons(ToolBar toolBar) {
        if(toolBar.getItem(0).getId().equals("firstToolBarButton"))
            for(int i = 0; i < componentList.size() ; i++)
                toolBar.insert(componentList.get(i),i+3);
        else{
            for(int i = 0; i < componentList.size() ; i++)
                toolBar.insert(componentList.get(i),i+2);
        }
    }
}
