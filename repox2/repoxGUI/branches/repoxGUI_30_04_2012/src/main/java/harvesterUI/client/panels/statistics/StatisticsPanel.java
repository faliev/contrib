package harvesterUI.client.panels.statistics;

import com.extjs.gxt.ui.client.Registry;
import com.extjs.gxt.ui.client.Style;
import com.extjs.gxt.ui.client.data.BaseTreeModel;
import com.extjs.gxt.ui.client.data.ModelData;
import com.extjs.gxt.ui.client.data.ModelIconProvider;
import com.extjs.gxt.ui.client.store.TreeStore;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.grid.AggregationRowConfig;
import com.extjs.gxt.ui.client.widget.grid.ColumnConfig;
import com.extjs.gxt.ui.client.widget.grid.ColumnModel;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.treegrid.TreeGrid;
import com.extjs.gxt.ui.client.widget.treegrid.TreeGridCellRenderer;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import harvesterUI.client.HarvesterUI;
import harvesterUI.client.servlets.RepoxServiceAsync;
import harvesterUI.client.util.ServerExceptionDialog;
import harvesterUI.shared.RepoxStatisticsUI;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created to REPOX.
 * User: Edmundo
 * Date: 26-03-2011
 * Time: 17:39
 */
public class StatisticsPanel extends ContentPanel{

    private TreeStore<ModelData> store;
    private TreeGrid<ModelData> grid;
    private AggregationRowConfig<ModelData> gener;
    protected RepoxStatisticsUI statistics;

    public StatisticsPanel() {
        setHeading(HarvesterUI.CONSTANTS.statistics());
        setIcon(HarvesterUI.ICONS.statistics_icon());
        setScrollMode(Style.Scroll.AUTO);
        setLayout(new FitLayout());

        store = new TreeStore<ModelData>();

        createGridList();
    }

    public void getStatistics() {
        AsyncCallback<RepoxStatisticsUI> callback = new AsyncCallback<RepoxStatisticsUI>() {
            public void onFailure(Throwable caught) {
                new ServerExceptionDialog("Failed to get response from server",caught.getMessage()).show();
            }
            public void onSuccess(RepoxStatisticsUI result) {
                statistics = result;
                parseStatistics();
            }
        };
        RepoxServiceAsync service = (RepoxServiceAsync) Registry.get(HarvesterUI.REPOX_SERVICE);
        service.getStatisticsInfo(callback);
    }

    private void parseStatistics() {
        BaseTreeModel treeModel = new BaseTreeModel();
        

        if(HarvesterUI.getProjectType().equals("EUROPEANA")) {
            BaseTreeModel agg = createNewNode(HarvesterUI.CONSTANTS.aggregators(), formatNumber(statistics.getAggregators()));
            treeModel.add(agg);
        }

        BaseTreeModel dps = createNewNode(HarvesterUI.CONSTANTS.dataProviders(), formatNumber(statistics.getDataProviders()));
        treeModel.add(dps);
        // Total DS
        int totalDS = statistics.getDataSourcesIdExtracted() + statistics.getDataSourcesIdGenerated();
        treeModel.add(createNewNode(HarvesterUI.CONSTANTS.dataSets(), formatNumber(totalDS)));

        BaseTreeModel type = createNewNode(HarvesterUI.CONSTANTS.type(), "");
        type.add(createNewNode("OAI-PMH", formatNumber(statistics.getDataSourcesOai())));
        type.add(createNewNode("Folder", formatNumber(statistics.getDataSourcesDirectoryImporter())));
        type.add(createNewNode("Z39.50", formatNumber(statistics.getDataSourcesZ3950())));
        treeModel.add(type);

        BaseTreeModel metadataFormat = createNewNode(HarvesterUI.CONSTANTS.metadataFormat(), "");
        Iterator it = statistics.getDataSourcesMetadataFormats().entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry mapEntry=(Map.Entry)it.next();
            String format = (String)mapEntry.getKey();
            int dsNum = (Integer)mapEntry.getValue();
            metadataFormat.add(createNewNode(format, formatNumber(dsNum)));
        }
        treeModel.add(metadataFormat);

        BaseTreeModel idPolicy = createNewNode(HarvesterUI.CONSTANTS.idPolicy(), "");
        idPolicy.add(createNewNode(HarvesterUI.CONSTANTS.idExtracted(), formatNumber(statistics.getDataSourcesIdExtracted())));
        idPolicy.add(createNewNode(HarvesterUI.CONSTANTS.idGenerated(), formatNumber(statistics.getDataSourcesIdGenerated())));
        treeModel.add(idPolicy);

        treeModel.add(createNewNode(HarvesterUI.CONSTANTS.records(), String.valueOf(statistics.getRecordsTotal())));

        BaseTreeModel averages = createNewNode(HarvesterUI.CONSTANTS.averages(), "");
        averages.add(createNewNode(HarvesterUI.CONSTANTS.averageRecordsDP(), formatNumber(statistics.getRecordsAvgDataProvider())));
        averages.add(createNewNode(HarvesterUI.CONSTANTS.averageRecordsDS(), formatNumber(statistics.getRecordsAvgDataSource())));
        treeModel.add(averages);

        BaseTreeModel countries = createNewNode(HarvesterUI.CONSTANTS.countries(), "");
        it = statistics.getCountriesRecords().entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry mapEntry=(Map.Entry)it.next();
            String countryC = (String)mapEntry.getKey();
            int records = (Integer)mapEntry.getValue();
            String imageLink = "<img src='resources/images/countries/" +
                    countryC + ".png' alt='???'/> &nbsp ";
            countries.add(createNewNode(imageLink + countryC, formatNumber(records)));
        }
        treeModel.add(countries);

        gener.setHtml("value", String.valueOf(statistics.getGenerationDate()));

        store.removeAll();
        store.add(treeModel.getChildren(), true);
        grid.expandAll();
    }

    private BaseTreeModel createNewNode(String name,String value) {
        BaseTreeModel result = new BaseTreeModel();
        result.set("name", name);
        result.set("value", value);
        return result;
    }

    private void createGridList() {
        List<ColumnConfig> columns = new ArrayList<ColumnConfig>();

        ColumnConfig name = new ColumnConfig("name", HarvesterUI.CONSTANTS.name(), 100);
        name.setRenderer(new TreeGridCellRenderer<ModelData>());
        columns.add(name);

        ColumnConfig result = new ColumnConfig("value", HarvesterUI.CONSTANTS.data(), 100);
        columns.add(result);

        ColumnModel cm = new ColumnModel(columns);

        gener = new AggregationRowConfig<ModelData>();
        gener.setHtml("name", HarvesterUI.CONSTANTS.generationDate()+": ");
        cm.addAggregationRow(gener);

        grid = new TreeGrid<ModelData>(store, cm);
        grid.getView().setEmptyText(HarvesterUI.CONSTANTS.statisticsPanelMask());
        grid.setBorders(false);
        grid.setAutoExpand(true);
        grid.setTrackMouseOver(false);
        grid.setLayoutData(new FitLayout());
        grid.setStripeRows(true);
        grid.setColumnLines(true);
        grid.getView().setForceFit(true);
        grid.disableTextSelection(false);

        grid.setIconProvider(new ModelIconProvider<ModelData>() {

            public AbstractImagePrototype getIcon(ModelData model) {
                if(model instanceof BaseTreeModel) {
                    BaseTreeModel treeModel = (BaseTreeModel) model;
                    if(treeModel.get("name").equals("Aggregators"))
                        return HarvesterUI.ICONS.aggregator_icon();
                    else if(treeModel.get("name").equals("Data Providers"))
                        return HarvesterUI.ICONS.data_provider_icon();
                }
                return null;
            }
        });

        add(grid);
    }
    
    private String formatNumber(int number){
        NumberFormat fmt = NumberFormat.getDecimalFormat();
        String result = fmt.format(number);
        return result.replace(",",".");
    }
}
