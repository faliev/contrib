package harvesterUI.client.panels.administration;

import com.extjs.gxt.ui.client.Registry;
import com.extjs.gxt.ui.client.Style;
import com.extjs.gxt.ui.client.core.El;
import com.extjs.gxt.ui.client.core.XDOM;
import com.extjs.gxt.ui.client.data.BaseModel;
import com.extjs.gxt.ui.client.event.*;
import com.extjs.gxt.ui.client.mvc.Dispatcher;
import com.extjs.gxt.ui.client.widget.Component;
import com.extjs.gxt.ui.client.widget.ComponentPlugin;
import com.extjs.gxt.ui.client.widget.VerticalPanel;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.form.FormButtonBinding;
import com.extjs.gxt.ui.client.widget.form.FormPanel;
import com.extjs.gxt.ui.client.widget.form.TextField;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.layout.FormData;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import harvesterUI.client.HarvesterUI;
import harvesterUI.client.core.AppEvents;
import harvesterUI.client.servlets.RepoxServiceAsync;
import harvesterUI.client.util.ServerExceptionDialog;
import harvesterUI.client.util.formPanel.DefaultFormPanel;
import harvesterUI.client.util.formPanel.EditableFormLayout;

/**
 * Created to REPOX.
 * User: Edmundo
 * Date: 07-04-2011
 * Time: 13:55
 */
public class AdminForm extends VerticalPanel {

    private FormData formData;
    public RepoxServiceAsync service;
    protected DefaultFormPanel simple;

    //Fields
    private TextField<String> repositoryFolderField, configFilesFolderField, oaiRequestFolderField, ftpRequestField,
            derbyDbFolderField,baseUrnField, defaultExportFolderField, adminEmailField, smtpServerField,
            smtpPortField, repoxDefualtEmailSenderField,adminPassField, httpRequestField, sampleRecordsField;

    public AdminForm() {
        service = (RepoxServiceAsync) Registry.get(HarvesterUI.REPOX_SERVICE);
        formData = new FormData("-20");
        createForm();
    }

    @Override
    protected void onRender(Element parent, int index) {
        super.onRender(parent, index);
        setLayout(new FitLayout());
    }

    private void createForm() {
        simple = new DefaultFormPanel();
        simple.setHeading(HarvesterUI.CONSTANTS.configurationSettings());
        simple.setIcon(HarvesterUI.ICONS.config_properties());

        EditableFormLayout layout = new EditableFormLayout(FormPanel.LabelAlign.RIGHT);
        layout.setLabelWidth(190);
        layout.setLabelSeparator("");
        simple.setLayout(layout);

        ComponentPlugin plugin = new ComponentPlugin() {
            public void init(Component component) {
                component.addListener(Events.Render, new Listener<ComponentEvent>() {
                    public void handleEvent(ComponentEvent be) {
                        El elem = be.getComponent().el().findParent(".x-form-element", 3);
                        // should style in external CSS  rather than directly
                        elem.appendChild(XDOM.create("<div style='color: grey;padding: 1 0 2 0px;'>" + be.getComponent().getData("text") + "</div>"));
                    }
                });
            }
        };

        repositoryFolderField = new TextField<String>();
        repositoryFolderField.setFieldLabel(HarvesterUI.CONSTANTS.repositoryFolder()+ HarvesterUI.REQUIRED_STR);
        repositoryFolderField.setId("admin_repoField");
        repositoryFolderField.addPlugin(plugin);
        repositoryFolderField.setData("text", HarvesterUI.CONSTANTS.repositoryFolderExample());
        repositoryFolderField.setAllowBlank(false);
        simple.add(repositoryFolderField, formData);

        configFilesFolderField = new TextField<String>();
        configFilesFolderField.setFieldLabel(HarvesterUI.CONSTANTS.configurationFilesFolder()+ HarvesterUI.REQUIRED_STR);
        configFilesFolderField.setId("admin_configffield");
        configFilesFolderField.addPlugin(plugin);
        configFilesFolderField.setData("text", HarvesterUI.CONSTANTS.configurationFilesFolderExample());
        configFilesFolderField.setAllowBlank(false);
        simple.add(configFilesFolderField, formData);

        oaiRequestFolderField = new TextField<String>();
        oaiRequestFolderField.setFieldLabel(HarvesterUI.CONSTANTS.oaiPmhRequestsFolder()+ HarvesterUI.REQUIRED_STR);
        oaiRequestFolderField.setId("admin_oaireqfolderfield");
        oaiRequestFolderField.addPlugin(plugin);
        oaiRequestFolderField.setData("text", HarvesterUI.CONSTANTS.oaiPmhRequestsFolderExample());
        oaiRequestFolderField.setAllowBlank(false);
        simple.add(oaiRequestFolderField, formData);

        ftpRequestField = new TextField<String>();
        ftpRequestField.setFieldLabel(HarvesterUI.CONSTANTS.ftpRequestsFolder()+ HarvesterUI.REQUIRED_STR);
        ftpRequestField.setId("admin_ftpreqfolderfield");
        ftpRequestField.addPlugin(plugin);
        ftpRequestField.setData("text", HarvesterUI.CONSTANTS.ftpRequestsFolderExample());
        ftpRequestField.setAllowBlank(false);
        simple.add(ftpRequestField, formData);

        httpRequestField = new TextField<String>();
        httpRequestField.setFieldLabel(HarvesterUI.CONSTANTS.httpRequestsFolder()+ HarvesterUI.REQUIRED_STR);
        httpRequestField.setId("admin_httpreqfolderfield");
        httpRequestField.addPlugin(plugin);
        httpRequestField.setData("text", HarvesterUI.CONSTANTS.httpRequestsFolderExample());
        httpRequestField.setAllowBlank(false);
        simple.add(httpRequestField, formData);

        derbyDbFolderField = new TextField<String>();
        derbyDbFolderField.setFieldLabel(HarvesterUI.CONSTANTS.derbyDatabaseFolder()+ HarvesterUI.REQUIRED_STR);
        derbyDbFolderField.setId("admin_derbyDBField");
        derbyDbFolderField.addPlugin(plugin);
        derbyDbFolderField.setData("text", HarvesterUI.CONSTANTS.derbyDatabaseFolderExample());
        derbyDbFolderField.setAllowBlank(false);
        simple.add(derbyDbFolderField, formData);

        sampleRecordsField = new TextField<String>();
        sampleRecordsField.setFieldLabel(HarvesterUI.CONSTANTS.sampleRecords()+ HarvesterUI.REQUIRED_STR);
        sampleRecordsField.setId("admin_samplerecordsfield");
        sampleRecordsField.addPlugin(plugin);
        sampleRecordsField.setData("text", HarvesterUI.CONSTANTS.sampleRecordsExample());
        sampleRecordsField.setAllowBlank(false);
        simple.add(sampleRecordsField, formData);

        baseUrnField = new TextField<String>();
        baseUrnField.setFieldLabel(HarvesterUI.CONSTANTS.baseUrn()+ HarvesterUI.REQUIRED_STR);
        baseUrnField.setId("admin_baseurnField");
        baseUrnField.addPlugin(plugin);
        baseUrnField.setData("text", HarvesterUI.CONSTANTS.baseUrnExample());
        baseUrnField.setAllowBlank(false);
        simple.add(baseUrnField, formData);

        defaultExportFolderField = new TextField<String>();
        defaultExportFolderField.setFieldLabel(HarvesterUI.CONSTANTS.defaultExportFolder()+ HarvesterUI.REQUIRED_STR);
        defaultExportFolderField.addPlugin(plugin);
        defaultExportFolderField.setId("admin_defaultExportField");
        defaultExportFolderField.setData("text", HarvesterUI.CONSTANTS.defaultExportFolderExample());
//        defaultExportFolderField.
        defaultExportFolderField.setAllowBlank(false);
        simple.add(defaultExportFolderField, formData);

        // Only for Europeana version
        if(!HarvesterUI.getProjectType().equals("EUROPEANA")) {
            defaultExportFolderField.hide();
        }

        adminEmailField = new TextField<String>();
        adminEmailField.setFieldLabel(HarvesterUI.CONSTANTS.administratorEmail()+ HarvesterUI.REQUIRED_STR);
        adminEmailField.setId("admin_email");
        adminEmailField.setAllowBlank(false);
        simple.add(adminEmailField, formData);

        smtpServerField = new TextField<String>();
        smtpServerField.setFieldLabel(HarvesterUI.CONSTANTS.smtpServer()+ HarvesterUI.REQUIRED_STR);
        smtpServerField.setId("admin_smtpserverfield");
        smtpServerField.setAllowBlank(false);
        simple.add(smtpServerField, formData);

        smtpPortField = new TextField<String>();
        smtpPortField.setFieldLabel(HarvesterUI.CONSTANTS.smtpPort()+ HarvesterUI.REQUIRED_STR);
        smtpPortField.setId("admin_smtpportfield");
        simple.add(smtpPortField, formData);

        repoxDefualtEmailSenderField = new TextField<String>();
        repoxDefualtEmailSenderField.setFieldLabel(HarvesterUI.CONSTANTS.repoxDefaultEmailSender()+ HarvesterUI.REQUIRED_STR);
        repoxDefualtEmailSenderField.setAllowBlank(false);
        repoxDefualtEmailSenderField.setId("admin_repoxdefaultemailsenderField");
        simple.add(repoxDefualtEmailSenderField, formData);

        adminPassField = new TextField<String>();
        adminPassField.setFieldLabel(HarvesterUI.CONSTANTS.administrationEmailPassword());
        adminPassField.setId("admin_adminpassField");
        adminPassField.setPassword(true);
        simple.add(adminPassField, formData);

        Button b = new Button(HarvesterUI.CONSTANTS.save(),HarvesterUI.ICONS.save_icon(),new SelectionListener<ButtonEvent>() {
            @Override
            public void componentSelected(ButtonEvent be) {
                BaseModel adminInfo = new BaseModel();
                adminInfo.set("repositoryFolder",repositoryFolderField.getValue());
                adminInfo.set("configFilesFolder",configFilesFolderField.getValue());
                adminInfo.set("oaiRequestFolder",oaiRequestFolderField.getValue());
                adminInfo.set("derbyDbFolder",derbyDbFolderField.getValue());
                adminInfo.set("baseUrn",baseUrnField.getValue());
                adminInfo.set("defaultExportFolder",defaultExportFolderField.getValue());
                adminInfo.set("adminEmail",adminEmailField.getValue());
                adminInfo.set("smtpServer",smtpServerField.getValue());
                adminInfo.set("smtpPort",smtpPortField.getValue());
                adminInfo.set("repoxDefaultEmailSender",repoxDefualtEmailSenderField.getValue());
                adminInfo.set("adminPass",adminPassField.getValue());
                adminInfo.set("ftpRequestFolder",ftpRequestField.getValue());
                adminInfo.set("httpRequestFolder",httpRequestField.getValue());
                adminInfo.set("sampleRecords",sampleRecordsField.getValue());

                AsyncCallback callback = new AsyncCallback() {
                    public void onFailure(Throwable caught) {
                        new ServerExceptionDialog("Failed to get response from server",caught.getMessage()).show();
                    }
                    public void onSuccess(Object result) {
                        simple.submit();
                        History.newItem("OVERVIEW_GRID");
                        Window.Location.reload();
//                        Dispatcher.get().dispatch(AppEvents.ViewOverviewGrid);
//                        HarvesterUI.UTIL_MANAGER.getSaveBox("Administration",
//                                "Admin Configuration Saved Successfully");
                    }
                };
                service.saveAdminFormInfo(adminInfo, callback);
            }
        });
        simple.addButton(b);
        simple.addButton(new Button(HarvesterUI.CONSTANTS.cancel(),HarvesterUI.ICONS.cancel_icon(),new SelectionListener<ButtonEvent>() {
            @Override
            public void componentSelected(ButtonEvent be) {
                Dispatcher.get().dispatch(AppEvents.LoadMainData);
            }
        }));

        simple.setButtonAlign(Style.HorizontalAlignment.CENTER);

        FormButtonBinding binding = new FormButtonBinding(simple);
        binding.addButton(b);

        add(simple);
    }

    public void editAdminForm() {
        AsyncCallback<BaseModel> callback = new AsyncCallback<BaseModel>() {
            public void onFailure(Throwable caught) {
                new ServerExceptionDialog("Failed to get response from server",caught.getMessage()).show();
            }
            public void onSuccess(BaseModel dataModel) {
                repositoryFolderField.setValue((String)dataModel.get("repositoryFolder"));
                configFilesFolderField.setValue((String)dataModel.get("configFilesFolder"));
                oaiRequestFolderField.setValue((String)dataModel.get("oaiRequestFolder"));
                derbyDbFolderField.setValue((String)dataModel.get("derbyDbFolder"));
                baseUrnField.setValue((String)dataModel.get("baseUrn"));
                defaultExportFolderField.setValue((String)dataModel.get("defaultExportFolder"));
                adminEmailField.setValue((String)dataModel.get("adminEmail"));
                smtpServerField.setValue((String)dataModel.get("smtpServer"));
                smtpPortField.setValue((String)dataModel.get("smtpPort"));
                repoxDefualtEmailSenderField.setValue((String)dataModel.get("repoxDefualtEmailSender"));
                adminPassField.setValue((String)dataModel.get("adminPass"));
                httpRequestField.setValue((String)dataModel.get("httpRequestFolder"));
                ftpRequestField.setValue((String)dataModel.get("ftpRequestFolder"));
                sampleRecordsField.setValue((String)dataModel.get("sampleRecords"));
            }
        };
        service.loadAdminFormInfo(callback);
    }

    public String getRepositoryFolderPath() { return repositoryFolderField.getValue();}
    public String getDefaultExportFolder() { return defaultExportFolderField.getValue();}
}
