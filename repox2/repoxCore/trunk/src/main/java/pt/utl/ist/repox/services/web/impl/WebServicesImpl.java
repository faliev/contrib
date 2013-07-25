package pt.utl.ist.repox.services.web.impl;

import org.apache.log4j.Logger;
import org.dom4j.*;
import org.dom4j.io.SAXReader;
import pt.utl.ist.repox.Urn;
import pt.utl.ist.repox.dataProvider.*;
import pt.utl.ist.repox.dataProvider.dataSource.IdExtracted;
import pt.utl.ist.repox.dataProvider.dataSource.IdGenerated;
import pt.utl.ist.repox.externalServices.ExternalRestService;
import pt.utl.ist.repox.ftp.DataSourceFtp;
import pt.utl.ist.repox.http.DataSourceHttp;
import pt.utl.ist.repox.marc.DataSourceDirectoryImporter;
import pt.utl.ist.repox.marc.Iso2709FileExtract;
import pt.utl.ist.repox.metadataSchemas.MetadataSchema;
import pt.utl.ist.repox.metadataSchemas.MetadataSchemaVersion;
import pt.utl.ist.repox.metadataTransformation.MetadataFormat;
import pt.utl.ist.repox.metadataTransformation.MetadataTransformation;
import pt.utl.ist.repox.metadataTransformation.TransformationsFileManager;
import pt.utl.ist.repox.oai.DataSourceOai;
import pt.utl.ist.repox.recordPackage.RecordRepox;
import pt.utl.ist.repox.services.web.WebServices;
import pt.utl.ist.repox.services.web.rest.RestUtils;
import pt.utl.ist.repox.statistics.RepoxStatistics;
import pt.utl.ist.repox.statistics.StatisticsManager;
import pt.utl.ist.repox.task.IngestDataSource;
import pt.utl.ist.repox.task.ScheduledTask;
import pt.utl.ist.repox.task.Task;
import pt.utl.ist.repox.util.ConfigSingleton;
import pt.utl.ist.repox.util.TimeUtil;
import pt.utl.ist.repox.z3950.DataSourceZ3950;
import pt.utl.ist.repox.z3950.IdListHarvester;
import pt.utl.ist.repox.z3950.IdSequenceHarvester;
import pt.utl.ist.repox.z3950.TimestampHarvester;
import pt.utl.ist.util.DateUtil;
import pt.utl.ist.util.FileUtil;
import pt.utl.ist.util.exceptions.*;

import java.io.*;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.*;

public class WebServicesImpl implements WebServices {
    private static final Logger log = Logger.getLogger(WebServicesImpl.class);
    private String requestURI;

    public String getRequestURI() {
        return requestURI;
    }

    public void setRequestURI(String requestURI) {
        this.requestURI = requestURI;
    }

    public WebServicesImpl() {
        super();
    }


    @Override
    public void writeDataProviders(OutputStream out) throws DocumentException, IOException {
        List<DataProvider> dataProviders = ConfigSingleton.getRepoxContextUtil().getRepoxManager().getDataManager().getDataProviders();

        Element dataProvidersElement = DocumentHelper.createElement("dataProviders");

        for (DataProvider dataProvider : dataProviders) {
            Element currentDataProviderElement = dataProvider.createElement(false);
            dataProvidersElement.add(currentDataProviderElement);
        }

        RestUtils.writeRestResponse(out, dataProvidersElement);
    }

    @Override
    public void createDataProvider(OutputStream out, String name, String country, String description) throws DocumentException {
        try {
            DataProvider dataProvider = ConfigSingleton.getRepoxContextUtil().getRepoxManager().getDataManager().createDataProvider(name,
                    country, description);
            RestUtils.writeRestResponse(out, dataProvider.createElement(false));
        } catch (IOException e) {
            createErrorMessage(out, MessageType.OTHER, "Error creating Data Provider.");
        } catch (AlreadyExistsException e) {
            createErrorMessage(out, MessageType.ALREADY_EXISTS, "Data Provider already exists.");
        }
    }

    @Override
    public void updateDataProvider(OutputStream out, String id, String name, String country, String description) throws DocumentException {
        try {
            DataProvider dataProvider = ConfigSingleton.getRepoxContextUtil().getRepoxManager().getDataManager().updateDataProvider(id,
                    name, country, description);
            RestUtils.writeRestResponse(out, dataProvider.createElement(false));
        } catch (ObjectNotFoundException e) {
            createErrorMessage(out, MessageType.NOT_FOUND, "Error updating Data Provider: id \"" + id + "\" was not found.");
        } catch (IOException e) {
            createErrorMessage(out, MessageType.OTHER, "Error updating Data Provider with id \"" + id + "\".");
        }
    }


    @Override
    public void deleteDataProvider(OutputStream out, String dataProviderId) throws DocumentException {
        try {
            ConfigSingleton.getRepoxContextUtil().getRepoxManager().getDataManager().deleteDataProvider(dataProviderId);
            Element currentDataProviderElement = DocumentHelper.createElement("success");
            currentDataProviderElement.setText("Data Provider with id \"" + dataProviderId + "\" was successfully deleted.");
            RestUtils.writeRestResponse(out, currentDataProviderElement);
        } catch (IOException e) {
            createErrorMessage(out, MessageType.OTHER, "Error deleting Data Provider with id \"" + dataProviderId + "\".");
        } catch (ObjectNotFoundException e) {
            createErrorMessage(out, MessageType.NOT_FOUND, "Error deleting Data Provider. Data provider with id \"" + dataProviderId + "\" was not found.");
        }
    }

    @Override
    public void getDataProvider(OutputStream out, String dataProviderId) throws DocumentException {
        try {
            DataProvider dataProvider = ConfigSingleton.getRepoxContextUtil().getRepoxManager().getDataManager().getDataProvider(dataProviderId);

            if(dataProvider != null){
                Element dataProviderElement = dataProvider.createElement(false);
                RestUtils.writeRestResponse(out, dataProviderElement);
            }
            else{
                createErrorMessage(out, MessageType.NOT_FOUND, "Error retrieving Data Provider. Data Provider with id \"" + dataProviderId + "\" was not found.");
            }
        }
        catch (IOException e) {
            createErrorMessage(out, MessageType.OTHER, "Error retrieving Data Provider with id \"" + dataProviderId + "\".");
        }
    }


    @Override
    public void writeDataSources(OutputStream out) throws DocumentException, IOException {
        HashMap<String, DataSourceContainer> dataSourceContainers = ConfigSingleton.getRepoxContextUtil().getRepoxManager().getDataManager().loadDataSourceContainers();

        Element dataSourcesElement = DocumentHelper.createElement("dataSources");

        for (DataSourceContainer dataSourceContainer : dataSourceContainers.values()) {
            Element currentDatasourceElement = dataSourceContainer.getDataSource().createElement();
            dataSourcesElement.add(currentDatasourceElement);
        }

        RestUtils.writeRestResponse(out, dataSourcesElement);
    }

    @Override
    public void writeDataSources(OutputStream out, String dataProviderId) throws DocumentException, IOException {
        DataProvider dataProvider = ConfigSingleton.getRepoxContextUtil().getRepoxManager().getDataManager().getDataProvider(dataProviderId);

        if(dataProvider != null){
            Element dataSourcesElement = DocumentHelper.createElement("dataSources");

            for (DataSourceContainer dataSourceContainer : dataProvider.getDataSourceContainers().values()) {
                Element currentDatasourceElement = dataSourceContainer.getDataSource().createElement();
                dataSourcesElement.add(currentDatasourceElement);
            }

            RestUtils.writeRestResponse(out, dataSourcesElement);
        }
        else{
            createErrorMessage(out, MessageType.NOT_FOUND, "Error retrieving Data Sources. Data provider with id \"" + dataProviderId + "\" was not found.");
        }
    }


    @Override
    public void createDataSourceOai(OutputStream out, String dataProviderId, String id, String description,
                                    String schema, String namespace, String metadataFormat, String oaiSourceURL,
                                    String oaiSet, String marcFormat) throws DocumentException {
        saveNewMetadataSchema(metadataFormat, schema, namespace, out);

        try {
            DataSource dataSource = ((DataManagerDefault)ConfigSingleton.getRepoxContextUtil().getRepoxManager().getDataManager()).createDataSourceOai(dataProviderId,
                    id, description, schema, namespace, metadataFormat, oaiSourceURL, oaiSet, new HashMap<String, MetadataTransformation>(),
                    new ArrayList<ExternalRestService>(),marcFormat);
            DataSourceContainer dataSourceContainer = ConfigSingleton.getRepoxContextUtil().getRepoxManager().getDataManager().getDataSourceContainer(dataSource.getId());
            RestUtils.writeRestResponse(out, dataSourceContainer.createElement());
        } catch (SQLException e) {
            createErrorMessage(out, MessageType.ERROR_DATABASE, "Error creating Data Base.");
        } catch (IOException e) {
            createErrorMessage(out, MessageType.OTHER, "Error creating Data Source.");
        } catch (ObjectNotFoundException e) {
            createErrorMessage(out, MessageType.NOT_FOUND, "Error creating a Data Source OAI. Data provider with id \"" + dataProviderId + "\" was not found.");
        } catch (AlreadyExistsException e) {
            createErrorMessage(out, MessageType.ALREADY_EXISTS, "Error creating a Data Source OAI. Data source with id \"" + id + "\" already exists.");
        } catch (InvalidArgumentsException e) {
            createErrorMessage(out, MessageType.INVALID_ARGUMENTS, "Error creating a Data Source OAI. Invalid argument " + e.getMessage() + ".");
        }
    }


    public void createDataSourceZ3950Timestamp(OutputStream out, String dataProviderId, String id, String description,
                                               String schema, String namespace, String address, String port, String database,
                                               String user, String password, String recordSyntax, String charset, String earliestTimestampString,
                                               String recordIdPolicyClass, String idXpath, String namespacePrefix, String namespaceUri) throws DocumentException, IOException, ParseException {
        saveNewMetadataSchema(MetadataFormat.MarcXchange.toString(), schema, namespace, out);
        try {
            Map<String, String> namespaces = new TreeMap<String, String>();
            namespaces.put(namespacePrefix, namespaceUri);

            DataSource dataSource = ((DataManagerDefault)ConfigSingleton.getRepoxContextUtil().getRepoxManager().getDataManager()).createDataSourceZ3950Timestamp(dataProviderId,
                    id, description, schema, namespace, address, port, database, user, password,
                    recordSyntax, charset, earliestTimestampString, recordIdPolicyClass, idXpath, namespaces,
                    new HashMap<String, MetadataTransformation>(),new ArrayList<ExternalRestService>());
            DataSourceContainer dataSourceContainer = ConfigSingleton.getRepoxContextUtil().getRepoxManager().getDataManager().getDataSourceContainer(dataSource.getId());
            RestUtils.writeRestResponse(out, dataSourceContainer.createElement());
        } catch (AlreadyExistsException e) {
            createErrorMessage(out, MessageType.ALREADY_EXISTS, "Error creating a Data Source Z39.50 Timestamp. Data source with id \"" + id + "\" already exists.");
        } catch (ObjectNotFoundException e) {
            createErrorMessage(out, MessageType.NOT_FOUND, "Error creating a Data Source Z39.50 Timestamp. Data provider with id \"" + dataProviderId + "\" was not found.");
        } catch (InvalidArgumentsException e) {
            createErrorMessage(out, MessageType.INVALID_ARGUMENTS, "Error creating a Data Source Z39.50 Timestamp. Invalid argument " + e.getMessage() + ".");
        } catch (SQLException e) {
            createErrorMessage(out, MessageType.ERROR_DATABASE, "Error creating Data Source Z39.50 Timestamp.");
        }
    }

    public void createDataSourceZ3950IdList(OutputStream out, String dataProviderId, String id, String description,
                                            String schema, String namespace, String address, String port, String database,
                                            String user, String password, String recordSyntax, String charset, InputStream xslFile,
                                            String recordIdPolicyClass, String idXpath, String namespacePrefix, String namespaceUri) throws DocumentException, IOException, ParseException {
        saveNewMetadataSchema(MetadataFormat.MarcXchange.toString(), schema, namespace, out);
        try {
            Map<String, String> namespaces = new TreeMap<String, String>();
            namespaces.put(namespacePrefix, namespaceUri);

            File temporaryFile = IdListHarvester.getIdListFilePermanent();
            byte[] buffer = new byte[8 * 1024];
            try {
                OutputStream output = new FileOutputStream(temporaryFile);
                try {
                    int bytesRead;
                    while ((bytesRead = xslFile.read(buffer)) != -1) {
                        output.write(buffer, 0, bytesRead);
                    }
                } finally {
                    output.close();
                }
            } finally {
                xslFile.close();
            }

            DataSource dataSource = ((DataManagerDefault) ConfigSingleton.getRepoxContextUtil().getRepoxManager().getDataManager()).createDataSourceZ3950IdList(dataProviderId,
                    id, description, schema, namespace, address, port, database, user, password,
                    recordSyntax, charset, temporaryFile.getAbsolutePath(), recordIdPolicyClass, idXpath, namespaces,
                    new HashMap<String, MetadataTransformation>(),new ArrayList<ExternalRestService>());
            DataSourceContainer dataSourceContainer = ConfigSingleton.getRepoxContextUtil().getRepoxManager().getDataManager().getDataSourceContainer(dataSource.getId());
            RestUtils.writeRestResponse(out, dataSourceContainer.createElement());
        } catch (ObjectNotFoundException e) {
            createErrorMessage(out, MessageType.NOT_FOUND, "Error creating a Data Source Z39.50 Id List. Data provider with id \"" + dataProviderId + "\" was not found.");
        } catch (AlreadyExistsException e) {
            createErrorMessage(out, MessageType.ALREADY_EXISTS, "Error creating a Data Source Z39.50 Id List. Data source with id \"" + id + "\" already exists.");
        } catch (InvalidArgumentsException e) {
            createErrorMessage(out, MessageType.INVALID_ARGUMENTS, "Error creating a Data Source Z39.50 Id List. Invalid argument " + e.getMessage() + ".");
        } catch (SQLException e) {
            createErrorMessage(out, MessageType.ERROR_DATABASE, "Error creating Data Source Z39.50 Id List.");
        }
    }

    public void createDataSourceZ3950IdSequence(OutputStream out, String dataProviderId, String id, String description,
                                                String schema, String namespace, String address, String port,
                                                String database, String user, String password, String recordSyntax,
                                                String charset, String maximumIdString, String recordIdPolicyClass,
                                                String idXpath, String namespacePrefix, String namespaceUri) throws DocumentException, IOException, ParseException {
        saveNewMetadataSchema(MetadataFormat.MarcXchange.toString(), schema, namespace, out);
        try {
            Map<String, String> namespaces = new TreeMap<String, String>();
            namespaces.put(namespacePrefix, namespaceUri);

            DataSource dataSource = ((DataManagerDefault)ConfigSingleton.getRepoxContextUtil().getRepoxManager().getDataManager()).createDataSourceZ3950IdSequence(dataProviderId,
                    id, description, schema, namespace, address, port, database, user, password,
                    recordSyntax, charset, maximumIdString, recordIdPolicyClass, idXpath, namespaces,
                    new HashMap<String, MetadataTransformation>(),new ArrayList<ExternalRestService>());
            DataSourceContainer dataSourceContainer = ConfigSingleton.getRepoxContextUtil().getRepoxManager().getDataManager().getDataSourceContainer(dataSource.getId());
            RestUtils.writeRestResponse(out, dataSourceContainer.createElement());
        } catch (AlreadyExistsException e) {
            createErrorMessage(out, MessageType.ALREADY_EXISTS, "Error creating a Data Source Z39.50 Id Sequence. Data source with id \"" + id + "\" already exists.");
        } catch (ObjectNotFoundException e) {
            createErrorMessage(out, MessageType.NOT_FOUND, "Error creating a Data Source Z39.50 Id Sequence. Data provider with id \"" + dataProviderId + "\" was not found.");
        } catch (InvalidArgumentsException e) {
            createErrorMessage(out, MessageType.INVALID_ARGUMENTS, "Error creating a Data Source Z39.50 Id Sequence. Invalid argument " + e.getMessage() + ".");
        } catch (SQLException e) {
            createErrorMessage(out, MessageType.ERROR_DATABASE, "Error creating Data Source Z39.50 Id Sequence.");
        }
    }


    @Override
    public void createDataSourceFtp(OutputStream out, String dataProviderId, String id, String description, String schema, String namespace,
                                    String metadataFormat, String isoFormat, String charset,
                                    String recordIdPolicyClass, String idXpath, String namespacePrefix, String namespaceUri,
                                    String recordXPath, String server, String user, String password, String ftpPath, String marcFormat) throws DocumentException {
        saveNewMetadataSchema(metadataFormat, schema, namespace, out);
        try {
            Map<String, String> namespaces = new TreeMap<String, String>();
            namespaces.put(namespacePrefix, namespaceUri);

            DataSource dataSource = ((DataManagerDefault)ConfigSingleton.getRepoxContextUtil().getRepoxManager().getDataManager()).createDataSourceFtp(dataProviderId,
                    id, description, schema, namespace, metadataFormat, isoFormat, charset,
                    recordIdPolicyClass, idXpath, namespaces, recordXPath, server, user, password,
                    ftpPath, new HashMap<String, MetadataTransformation>(),new ArrayList<ExternalRestService>(),marcFormat);
            DataSourceContainer dataSourceContainer = ConfigSingleton.getRepoxContextUtil().getRepoxManager().getDataManager().getDataSourceContainer(dataSource.getId());
            RestUtils.writeRestResponse(out, dataSourceContainer.createElement());
        } catch (IOException e) {
            createErrorMessage(out, MessageType.OTHER, "Error creating Data Source FTP.");
        } catch (AlreadyExistsException e) {
            createErrorMessage(out, MessageType.ALREADY_EXISTS, "Error creating a Data Source FTP. Data source with id \"" + id + "\" already exists.");
        } catch (ObjectNotFoundException e) {
            createErrorMessage(out, MessageType.NOT_FOUND, "Error creating a Data Source FTP. Data provider with id \"" + dataProviderId + "\" was not found.");
        } catch (InvalidArgumentsException e) {
            createErrorMessage(out, MessageType.INVALID_ARGUMENTS, "Error creating Data Source. Invalid argument " + e.getMessage() + ".");
        } catch (SQLException e) {
            createErrorMessage(out, MessageType.ERROR_DATABASE, "Error creating Data Source FTP.");
        }

    }

    public void createDataSourceHttp(OutputStream out, String dataProviderId, String id, String description, String schema, String namespace,
                                     String metadataFormat, String isoFormat, String charset,
                                     String recordIdPolicyClass, String idXpath, String namespacePrefix, String namespaceUri,
                                     String recordXPath, String url, String marcFormat) throws DocumentException {
        saveNewMetadataSchema(metadataFormat, schema, namespace, out);
        try {
            Map<String, String> namespaces = new TreeMap<String, String>();
            namespaces.put(namespacePrefix, namespaceUri);

            DataSource dataSource = ((DataManagerDefault)ConfigSingleton.getRepoxContextUtil().getRepoxManager().getDataManager()).createDataSourceHttp(dataProviderId,
                    id, description, schema, namespace, metadataFormat, isoFormat, charset,
                    recordIdPolicyClass, idXpath, namespaces, recordXPath, url,
                    new HashMap<String, MetadataTransformation>(),new ArrayList<ExternalRestService>(),marcFormat);
            DataSourceContainer dataSourceContainer = ConfigSingleton.getRepoxContextUtil().getRepoxManager().getDataManager().getDataSourceContainer(dataSource.getId());
            RestUtils.writeRestResponse(out, dataSourceContainer.createElement());
        } catch (IOException e) {
            createErrorMessage(out, MessageType.OTHER, "Error creating Data Source HTTP.");
        } catch (AlreadyExistsException e) {
            createErrorMessage(out, MessageType.ALREADY_EXISTS, "Error creating a Data Source HTTP. Data source with id \"" + id + "\" already exists.");
        } catch (ObjectNotFoundException e) {
            createErrorMessage(out, MessageType.NOT_FOUND, "Error creating a Data Source HTTP. Data provider with id \"" + dataProviderId + "\" was not found.");
        } catch (InvalidArgumentsException e) {
            createErrorMessage(out, MessageType.INVALID_ARGUMENTS, "Error creating Data Source HTTP. Invalid argument " + e.getMessage() + ".");
        } catch (SQLException e) {
            createErrorMessage(out, MessageType.ERROR_DATABASE, "Error creating Data Source HTTP.");
        }
    }


    public void createDataSourceFolder(OutputStream out, String dataProviderId, String id, String description, String schema, String namespace,
                                       String metadataFormat, String isoFormat, String charset,
                                       String recordIdPolicyClass, String idXpath, String namespacePrefix, String namespaceUri,
                                       String recordXPath, String sourcesDirPath, String marcFormat) throws DocumentException {
        saveNewMetadataSchema(metadataFormat, schema, namespace, out);
        try {
            Map<String, String> namespaces = new TreeMap<String, String>();
            namespaces.put(namespacePrefix, namespaceUri);

            DataSource dataSource = ((DataManagerDefault)ConfigSingleton.getRepoxContextUtil().getRepoxManager().getDataManager()).createDataSourceFolder(dataProviderId,
                    id, description, schema, namespace, metadataFormat, isoFormat, charset,
                    recordIdPolicyClass, idXpath, namespaces, recordXPath, sourcesDirPath,
                    new HashMap<String, MetadataTransformation>(),new ArrayList<ExternalRestService>(),marcFormat);
            DataSourceContainer dataSourceContainer = ConfigSingleton.getRepoxContextUtil().getRepoxManager().getDataManager().getDataSourceContainer(dataSource.getId());
            RestUtils.writeRestResponse(out, dataSourceContainer.createElement());
        } catch (IOException e) {
            createErrorMessage(out, MessageType.OTHER, "Error creating Data Source.");
        } catch (ObjectNotFoundException e) {
            createErrorMessage(out, MessageType.NOT_FOUND, "Error creating a Data Source Folder. Data provider with id \"" + dataProviderId + "\" was not found.");
        } catch (AlreadyExistsException e) {
            createErrorMessage(out, MessageType.ALREADY_EXISTS, "Error creating a Data Source Folder. Data source with id \"" + id + "\" already exists.");
        } catch (InvalidArgumentsException e) {
            createErrorMessage(out, MessageType.INVALID_ARGUMENTS, "Error creating Data Source Folder. Invalid " + e.getMessage() + ".");
        } catch (SQLException e) {
            createErrorMessage(out, MessageType.ERROR_DATABASE, "Error creating Data Source.");
        }
    }

    public void updateDataSourceOai(OutputStream out, String id, String description, String schema, String namespace,
                                    String metadataFormat, String oaiSourceURL, String oaiSet, String marcFormat) throws DocumentException {
        if(metadataFormat != null && schema != null && namespace != null)
            saveNewMetadataSchema(metadataFormat, schema, namespace, out);
        try {
            Map<String, MetadataTransformation> transformations = new HashMap<String, MetadataTransformation>();
            List<ExternalRestService> externalServices = new ArrayList<ExternalRestService>();

            DataSourceContainerDefault dataSourceContainerOld = (DataSourceContainerDefault)(ConfigSingleton.getRepoxContextUtil().getRepoxManager().getDataManager().getDataSourceContainer(id));
            if(dataSourceContainerOld != null){
                DataSourceOai dataSourceOld = (DataSourceOai)dataSourceContainerOld.getDataSource();
                if(description == null)
                    description = dataSourceOld.getDescription();
                if(schema == null)
                    schema = dataSourceOld.getSchema();
                if(namespace == null)
                    namespace = dataSourceOld.getNamespace();
                if(metadataFormat == null)
                    metadataFormat = dataSourceOld.getMetadataFormat();
                if(oaiSourceURL == null)
                    oaiSourceURL = dataSourceOld.getOaiSourceURL();
                if(oaiSet == null)
                    oaiSet = dataSourceOld.getOaiSet();
                if(marcFormat == null)
                    marcFormat = dataSourceOld.getMarcFormat();
                if(dataSourceOld.getMetadataTransformations().size() > 0)
                    transformations = dataSourceOld.getMetadataTransformations();
                if(dataSourceOld.getExternalRestServices().size() > 0)
                    externalServices = dataSourceOld.getExternalRestServices();
            }
            DataSource dataSource = ((DataManagerDefault)ConfigSingleton.getRepoxContextUtil().getRepoxManager().getDataManager()).updateDataSourceOai(id, id,
                    description, schema, namespace, metadataFormat, oaiSourceURL, oaiSet,
                    transformations, externalServices, marcFormat, false);
            DataSourceContainer dataSourceContainer = ConfigSingleton.getRepoxContextUtil().getRepoxManager().getDataManager().getDataSourceContainer(dataSource.getId());
            RestUtils.writeRestResponse(out, dataSourceContainer.createElement());
        } catch (IOException e) {
            createErrorMessage(out, MessageType.OTHER, "Error updating an OAI Data Source. Data Source with id \"" + id + "\" was not an OAI-PMH data source.");
        } catch (ObjectNotFoundException e) {
            createErrorMessage(out, MessageType.NOT_FOUND, "Error updating an OAI Data Source. Data Provider was not found or the Data Source with id \"" + id + "\" was not found.");
        } catch (InvalidArgumentsException e) {
            createErrorMessage(out, MessageType.INVALID_ARGUMENTS, "Error updating an OAI Data Source. Invalid argument " + e.getMessage() + ".");
        } catch (IncompatibleInstanceException e) {
            createErrorMessage(out, MessageType.INCOMPATIBLE_TYPE, "Error updating an OAI Data Source. Data Source with id \"" + id + "\" was not an OAI-PMH data source.");
        }
    }

    public void updateDataSourceZ3950Timestamp(OutputStream out, String id, String description, String schema, String namespace,
                                               String address, String port, String database, String user, String password,
                                               String recordSyntax, String charset, String earliestTimestampString,
                                               String recordIdPolicyClass, String idXpath, String namespacePrefix,
                                               String namespaceUri) throws DocumentException, IOException, ParseException {
        if(schema != null && namespace != null)
            saveNewMetadataSchema(MetadataFormat.MarcXchange.toString(), schema, namespace, out);
        try {
            Map<String, String> namespaces = new TreeMap<String, String>();
            Map<String, MetadataTransformation> transformations = new HashMap<String, MetadataTransformation>();
            List<ExternalRestService> externalServices = new ArrayList<ExternalRestService>();

            DataSourceContainerDefault dataSourceContainerOld = (DataSourceContainerDefault)(ConfigSingleton.getRepoxContextUtil().getRepoxManager().getDataManager().getDataSourceContainer(id));if(dataSourceContainerOld != null){
                DataSourceZ3950 dataSourceOld = (DataSourceZ3950)dataSourceContainerOld.getDataSource();
                if(description == null)
                    description = dataSourceOld.getDescription();
                if(schema == null)
                    schema = dataSourceOld.getSchema();
                if(namespace == null)
                    namespace = dataSourceOld.getNamespace();
                if(address == null)
                    address = ((TimestampHarvester)dataSourceOld.getHarvestMethod()).getTarget().getAddress();
                if(port == null)
                    port = String.valueOf(((TimestampHarvester)dataSourceOld.getHarvestMethod()).getTarget().getPort());
                if(database == null)
                    database = ((TimestampHarvester)dataSourceOld.getHarvestMethod()).getTarget().getDatabase();
                if(user == null)
                    user = ((TimestampHarvester)dataSourceOld.getHarvestMethod()).getTarget().getUser();
                if(password == null)
                    password = ((TimestampHarvester)dataSourceOld.getHarvestMethod()).getTarget().getPassword();
                if(recordSyntax == null)
                    recordSyntax = ((TimestampHarvester)dataSourceOld.getHarvestMethod()).getTarget().getRecordSyntax();

                if(earliestTimestampString == null)
                    earliestTimestampString = DateUtil.date2String(((TimestampHarvester)dataSourceOld.getHarvestMethod()).getEarliestTimestamp(), "yyyyMMdd");

                if(charset == null && dataSourceOld.getHarvestMethod().getTarget().getCharacterEncoding() != null)
                    charset = dataSourceOld.getHarvestMethod().getTarget().getCharacterEncoding().toString();

                // todo... make a lot of tests
                if(recordIdPolicyClass == null){
                    if(dataSourceOld.getRecordIdPolicy() instanceof IdExtracted)
                        recordIdPolicyClass = IdExtracted.class.getSimpleName();
                    else if(dataSourceOld.getRecordIdPolicy() instanceof IdGenerated)
                        recordIdPolicyClass = IdGenerated.class.getSimpleName();
                }

                if(recordIdPolicyClass.equals(IdExtracted.class.getSimpleName())){
                    if(idXpath == null && dataSourceOld.getRecordIdPolicy() instanceof IdExtracted) {
                        IdExtracted idExtracted = (IdExtracted) dataSourceOld.getRecordIdPolicy();
                        idXpath = idExtracted.getIdentifierXpath();
                    }
                    if(idXpath == null)
                        throw new InvalidArgumentsException("idXpath is missing");

                    if(namespacePrefix == null && namespaceUri == null &&
                            dataSourceOld.getRecordIdPolicy() instanceof IdExtracted) {
                        IdExtracted idExtracted = (IdExtracted) dataSourceOld.getRecordIdPolicy();
                        namespaces = idExtracted.getNamespaces();
                    }
                    else if(namespacePrefix == null &&
                            dataSourceOld.getRecordIdPolicy() instanceof IdExtracted) {
                        // update to new namespaceUri
                        IdExtracted idExtracted = (IdExtracted) dataSourceOld.getRecordIdPolicy();

                        if(idExtracted.getNamespaces() != null && idExtracted.getNamespaces().size() > 0) {
                            for(String prefix : idExtracted.getNamespaces().keySet()) {
                                namespacePrefix = prefix;
                                break;
                            }
                            namespaces.put(namespacePrefix, namespaceUri);
                        }
                        else
                            throw new InvalidArgumentsException("namespacePrefix is missing");
                    }
                    else if(namespaceUri == null &&
                            dataSourceOld.getRecordIdPolicy() instanceof IdExtracted) {
                        // update to new namespacePrefix
                        IdExtracted idExtracted = (IdExtracted) dataSourceOld.getRecordIdPolicy();

                        if(idExtracted.getNamespaces() != null && idExtracted.getNamespaces().size() > 0) {
                            for(String uri : idExtracted.getNamespaces().values()) {
                                namespaceUri = uri;
                                break;
                            }
                            namespaces.put(namespacePrefix, namespaceUri);
                        }
                        else
                            throw new InvalidArgumentsException("namespaceUri is missing");
                    }
                    else {
                        // use the new values
                        if(namespacePrefix != null && namespaceUri != null) {
                            namespaces.put(namespacePrefix, namespaceUri);
                        }
                        else if(namespacePrefix != null && namespaceUri == null) {
                            throw new InvalidArgumentsException("namespaceUri is missing");
                        }
                        else if(namespacePrefix == null && namespaceUri != null) {
                            throw new InvalidArgumentsException("namespacePrefix is missing");
                        }
                    }
                }
                else{
                    // IdGenerated - empty fields
                    idXpath = null;
                    namespaces = new TreeMap<String, String>();
                }
                if(dataSourceOld.getMetadataTransformations().size() > 0)
                    transformations = dataSourceOld.getMetadataTransformations();
                if(dataSourceOld.getExternalRestServices().size() > 0)
                    externalServices = dataSourceOld.getExternalRestServices();
            }

            DataSource dataSource = ((DataManagerDefault) ConfigSingleton.getRepoxContextUtil().getRepoxManager().getDataManager()).updateDataSourceZ3950Timestamp(id, id,
                    description, schema, namespace, address, port, database, user, password,
                    recordSyntax, charset, earliestTimestampString, recordIdPolicyClass, idXpath, namespaces,
                    transformations, externalServices, false);
            DataSourceContainer dataSourceContainer = ConfigSingleton.getRepoxContextUtil().getRepoxManager().getDataManager().getDataSourceContainer(dataSource.getId());
            RestUtils.writeRestResponse(out, dataSourceContainer.createElement());
        } catch (ObjectNotFoundException e) {
            createErrorMessage(out, MessageType.NOT_FOUND, "Error updating a Z39.50 Data Source with Time Stamp. Data Source with id \"" + id + "\" was not found.");
        } catch (InvalidArgumentsException e) {
            createErrorMessage(out, MessageType.INVALID_ARGUMENTS, "Error updating a Z39.50 Data Source with Time Stamp. Invalid argument " + e.getMessage() + ".");
        } catch (IncompatibleInstanceException e) {
            createErrorMessage(out, MessageType.INCOMPATIBLE_TYPE, "Error updating a Z39.50 Data Source with Time Stamp. Data Source with id \"" + id + "\" was not a Z39.50 Data Source with Time Stamp.");
        }
    }

    public void updateDataSourceZ3950IdList(OutputStream out, String id, String description, String schema,
                                            String namespace, String address, String port, String database,
                                            String user, String password, String recordSyntax, String charset,
                                            InputStream xslFile, String recordIdPolicyClass, String idXpath,
                                            String namespacePrefix, String namespaceUri) throws DocumentException, IOException, ParseException {
        String filePath = null;
        if(xslFile != null){
            File temporaryFile = IdListHarvester.getIdListFilePermanent();
            filePath = temporaryFile.getAbsolutePath();
            byte[] buffer = new byte[8 * 1024];
            try {
                OutputStream output = new FileOutputStream(temporaryFile);
                try {
                    int bytesRead;
                    while ((bytesRead = xslFile.read(buffer)) != -1) {
                        output.write(buffer, 0, bytesRead);
                    }
                } finally {
                    output.close();
                }
            } finally {
                xslFile.close();
            }
        }

        if(schema != null && namespace != null)
            saveNewMetadataSchema(MetadataFormat.MarcXchange.toString(), schema, namespace, out);
        try {
            Map<String, String> namespaces = new TreeMap<String, String>();
            Map<String, MetadataTransformation> transformations = new HashMap<String, MetadataTransformation>();
            List<ExternalRestService> externalServices = new ArrayList<ExternalRestService>();


            DataSourceContainerDefault dataSourceContainerOld = (DataSourceContainerDefault)(ConfigSingleton.getRepoxContextUtil().getRepoxManager().getDataManager().getDataSourceContainer(id));if(dataSourceContainerOld != null){
                DataSourceZ3950 dataSourceOld = (DataSourceZ3950)dataSourceContainerOld.getDataSource();
                if(description == null)
                    description = dataSourceOld.getDescription();
                if(schema == null)
                    schema = dataSourceOld.getSchema();
                if(namespace == null)
                    namespace = dataSourceOld.getNamespace();
                if(address == null)
                    address = ((IdListHarvester)dataSourceOld.getHarvestMethod()).getTarget().getAddress();
                if(port == null)
                    port = String.valueOf(((IdListHarvester) dataSourceOld.getHarvestMethod()).getTarget().getPort());
                if(database == null)
                    database = ((IdListHarvester)dataSourceOld.getHarvestMethod()).getTarget().getDatabase();
                if(user == null)
                    user = ((IdListHarvester)dataSourceOld.getHarvestMethod()).getTarget().getUser();
                if(password == null)
                    password = ((IdListHarvester)dataSourceOld.getHarvestMethod()).getTarget().getPassword();
                if(recordSyntax == null)
                    recordSyntax = ((IdListHarvester)dataSourceOld.getHarvestMethod()).getTarget().getRecordSyntax();
                if(charset == null && ((IdListHarvester)dataSourceOld.getHarvestMethod()).getTarget().getCharacterEncoding() != null)
                    charset = ((IdListHarvester)dataSourceOld.getHarvestMethod()).getTarget().getCharacterEncoding().toString();
                if(filePath == null)
                    filePath = (((IdListHarvester)dataSourceOld.getHarvestMethod()).getIdListFile().getAbsolutePath());

                // todo... make a lot of tests
                if(recordIdPolicyClass == null){
                    if(dataSourceOld.getRecordIdPolicy() instanceof IdExtracted)
                        recordIdPolicyClass = IdExtracted.class.getSimpleName();
                    else if(dataSourceOld.getRecordIdPolicy() instanceof IdGenerated)
                        recordIdPolicyClass = IdGenerated.class.getSimpleName();
                }

                if(recordIdPolicyClass.equals(IdExtracted.class.getSimpleName())){
                    if(idXpath == null && dataSourceOld.getRecordIdPolicy() instanceof IdExtracted) {
                        IdExtracted idExtracted = (IdExtracted) dataSourceOld.getRecordIdPolicy();
                        idXpath = idExtracted.getIdentifierXpath();
                    }
                    if(idXpath == null)
                        throw new InvalidArgumentsException("idXpath is missing");

                    if(namespacePrefix == null && namespaceUri == null &&
                            dataSourceOld.getRecordIdPolicy() instanceof IdExtracted) {
                        IdExtracted idExtracted = (IdExtracted) dataSourceOld.getRecordIdPolicy();
                        namespaces = idExtracted.getNamespaces();
                    }
                    else if(namespacePrefix == null &&
                            dataSourceOld.getRecordIdPolicy() instanceof IdExtracted) {
                        // update to new namespaceUri
                        IdExtracted idExtracted = (IdExtracted) dataSourceOld.getRecordIdPolicy();

                        if(idExtracted.getNamespaces() != null && idExtracted.getNamespaces().size() > 0) {
                            for(String prefix : idExtracted.getNamespaces().keySet()) {
                                namespacePrefix = prefix;
                                break;
                            }
                            namespaces.put(namespacePrefix, namespaceUri);
                        }
                        else
                            throw new InvalidArgumentsException("namespacePrefix is missing");
                    }
                    else if(namespaceUri == null &&
                            dataSourceOld.getRecordIdPolicy() instanceof IdExtracted) {
                        // update to new namespacePrefix
                        IdExtracted idExtracted = (IdExtracted) dataSourceOld.getRecordIdPolicy();

                        if(idExtracted.getNamespaces() != null && idExtracted.getNamespaces().size() > 0) {
                            for(String uri : idExtracted.getNamespaces().values()) {
                                namespaceUri = uri;
                                break;
                            }
                            namespaces.put(namespacePrefix, namespaceUri);
                        }
                        else
                            throw new InvalidArgumentsException("namespaceUri is missing");
                    }
                    else {
                        // use the new values
                        if(namespacePrefix != null && namespaceUri != null) {
                            namespaces.put(namespacePrefix, namespaceUri);
                        }
                        else if(namespacePrefix != null && namespaceUri == null) {
                            throw new InvalidArgumentsException("namespaceUri is missing");
                        }
                        else if(namespacePrefix == null && namespaceUri != null) {
                            throw new InvalidArgumentsException("namespacePrefix is missing");
                        }
                    }
                }
                else{
                    // IdGenerated - empty fields
                    idXpath = null;
                    namespaces = new TreeMap<String, String>();
                }
                if(dataSourceOld.getMetadataTransformations().size() > 0)
                    transformations = dataSourceOld.getMetadataTransformations();
                if(dataSourceOld.getExternalRestServices().size() > 0)
                    externalServices = dataSourceOld.getExternalRestServices();
            }

            DataSource dataSource = ((DataManagerDefault)ConfigSingleton.getRepoxContextUtil().getRepoxManager().getDataManager()).updateDataSourceZ3950IdList(id, id,
                    description, schema, namespace, address, port, database, user, password,
                    recordSyntax, charset, filePath, recordIdPolicyClass, idXpath, namespaces,
                    transformations, externalServices, false);
            DataSourceContainer dataSourceContainer = ConfigSingleton.getRepoxContextUtil().getRepoxManager().getDataManager().getDataSourceContainer(dataSource.getId());
            RestUtils.writeRestResponse(out, dataSourceContainer.createElement());
        } catch (ObjectNotFoundException e) {
            createErrorMessage(out, MessageType.NOT_FOUND, "Error updating a Z39.50 Data Source with ID List. Data Source with id \"" + id + "\" was not found.");
        } catch (IncompatibleInstanceException e) {
            createErrorMessage(out, MessageType.INCOMPATIBLE_TYPE, "Error updating a Z39.50 Data Source with ID List. Data Source with id \"" + id + "\" was not a Z39.50 Data Source with ID List.");
        } catch (InvalidArgumentsException e) {
            createErrorMessage(out, MessageType.INVALID_ARGUMENTS, "Error updating a Z39.50 Data Source with ID List. Invalid argument " + e.getMessage() + ".");
        }
    }

    public void updateDataSourceZ3950IdSequence(OutputStream out, String id, String description, String schema,
                                                String namespace, String address, String port, String database,
                                                String user, String password, String recordSyntax, String charset,
                                                String maximumIdString, String recordIdPolicyClass, String idXpath,
                                                String namespacePrefix, String namespaceUri) throws DocumentException, IOException, ParseException {
        if(schema != null && namespace != null)
            saveNewMetadataSchema(MetadataFormat.MarcXchange.toString(), schema, namespace, out);
        try {
            Map<String, String> namespaces = new TreeMap<String, String>();
            Map<String, MetadataTransformation> transformations = new HashMap<String, MetadataTransformation>();
            List<ExternalRestService> externalServices = new ArrayList<ExternalRestService>();

            DataSourceContainerDefault dataSourceContainerOld = (DataSourceContainerDefault)(ConfigSingleton.getRepoxContextUtil().getRepoxManager().getDataManager().getDataSourceContainer(id));if(dataSourceContainerOld != null){
                DataSourceZ3950 dataSourceOld = (DataSourceZ3950)dataSourceContainerOld.getDataSource();
                if(description == null)
                    description = dataSourceOld.getDescription();
                if(schema == null)
                    schema = dataSourceOld.getSchema();
                if(namespace == null)
                    namespace = dataSourceOld.getNamespace();
                if(address == null)
                    address = ((IdSequenceHarvester)dataSourceOld.getHarvestMethod()).getTarget().getAddress();
                if(port == null)
                    port = String.valueOf(((IdSequenceHarvester) dataSourceOld.getHarvestMethod()).getTarget().getPort());
                if(database == null)
                    database = ((IdSequenceHarvester)dataSourceOld.getHarvestMethod()).getTarget().getDatabase();
                if(user == null)
                    user = ((IdSequenceHarvester)dataSourceOld.getHarvestMethod()).getTarget().getUser();
                if(password == null)
                    password = ((IdSequenceHarvester)dataSourceOld.getHarvestMethod()).getTarget().getPassword();
                if(recordSyntax == null)
                    recordSyntax = ((IdSequenceHarvester)dataSourceOld.getHarvestMethod()).getTarget().getRecordSyntax();
                if(charset == null && ((IdSequenceHarvester)dataSourceOld.getHarvestMethod()).getTarget().getCharacterEncoding() != null)
                    charset = ((IdSequenceHarvester)dataSourceOld.getHarvestMethod()).getTarget().getCharacterEncoding().toString();
                if(maximumIdString == null)
                    maximumIdString = String.valueOf(((IdSequenceHarvester) dataSourceOld.getHarvestMethod()).getMaximumId());

                // todo... make a lot of tests
                if(recordIdPolicyClass == null){
                    if(dataSourceOld.getRecordIdPolicy() instanceof IdExtracted)
                        recordIdPolicyClass = IdExtracted.class.getSimpleName();
                    else if(dataSourceOld.getRecordIdPolicy() instanceof IdGenerated)
                        recordIdPolicyClass = IdGenerated.class.getSimpleName();
                }

                if(recordIdPolicyClass.equals(IdExtracted.class.getSimpleName())){
                    if(idXpath == null && dataSourceOld.getRecordIdPolicy() instanceof IdExtracted) {
                        IdExtracted idExtracted = (IdExtracted) dataSourceOld.getRecordIdPolicy();
                        idXpath = idExtracted.getIdentifierXpath();
                    }
                    if(idXpath == null)
                        throw new InvalidArgumentsException("idXpath is missing");

                    if(namespacePrefix == null && namespaceUri == null &&
                            dataSourceOld.getRecordIdPolicy() instanceof IdExtracted) {
                        IdExtracted idExtracted = (IdExtracted) dataSourceOld.getRecordIdPolicy();
                        namespaces = idExtracted.getNamespaces();
                    }
                    else if(namespacePrefix == null &&
                            dataSourceOld.getRecordIdPolicy() instanceof IdExtracted) {
                        // update to new namespaceUri
                        IdExtracted idExtracted = (IdExtracted) dataSourceOld.getRecordIdPolicy();

                        if(idExtracted.getNamespaces() != null && idExtracted.getNamespaces().size() > 0) {
                            for(String prefix : idExtracted.getNamespaces().keySet()) {
                                namespacePrefix = prefix;
                                break;
                            }
                            namespaces.put(namespacePrefix, namespaceUri);
                        }
                        else
                            throw new InvalidArgumentsException("namespacePrefix is missing");
                    }
                    else if(namespaceUri == null &&
                            dataSourceOld.getRecordIdPolicy() instanceof IdExtracted) {
                        // update to new namespacePrefix
                        IdExtracted idExtracted = (IdExtracted) dataSourceOld.getRecordIdPolicy();

                        if(idExtracted.getNamespaces() != null && idExtracted.getNamespaces().size() > 0) {
                            for(String uri : idExtracted.getNamespaces().values()) {
                                namespaceUri = uri;
                                break;
                            }
                            namespaces.put(namespacePrefix, namespaceUri);
                        }
                        else
                            throw new InvalidArgumentsException("namespaceUri is missing");
                    }
                    else {
                        // use the new values
                        if(namespacePrefix != null && namespaceUri != null) {
                            namespaces.put(namespacePrefix, namespaceUri);
                        }
                        else if(namespacePrefix != null && namespaceUri == null) {
                            throw new InvalidArgumentsException("namespaceUri is missing");
                        }
                        else if(namespacePrefix == null && namespaceUri != null) {
                            throw new InvalidArgumentsException("namespacePrefix is missing");
                        }
                    }
                }
                else{
                    // IdGenerated - empty fields
                    idXpath = null;
                    namespaces = new TreeMap<String, String>();
                }
                if(dataSourceOld.getMetadataTransformations().size() > 0)
                    transformations = dataSourceOld.getMetadataTransformations();
                if(dataSourceOld.getExternalRestServices().size() > 0)
                    externalServices = dataSourceOld.getExternalRestServices();
            }

            DataSource dataSource = ((DataManagerDefault)ConfigSingleton.getRepoxContextUtil().getRepoxManager().getDataManager()).updateDataSourceZ3950IdSequence(id, id,
                    description, schema, namespace, address, port, database, user, password,
                    recordSyntax, charset, maximumIdString, recordIdPolicyClass, idXpath, namespaces,
                    transformations, externalServices, false);
            DataSourceContainer dataSourceContainer = ConfigSingleton.getRepoxContextUtil().getRepoxManager().getDataManager().getDataSourceContainer(dataSource.getId());
            RestUtils.writeRestResponse(out, dataSourceContainer.createElement());
        } catch (ObjectNotFoundException e) {
            createErrorMessage(out, MessageType.NOT_FOUND, "Error updating a Z39.50 Data Source with ID Sequence. Data Provider was not found.");
        } catch (InvalidArgumentsException e) {
            createErrorMessage(out, MessageType.INVALID_ARGUMENTS, "Error updating a Z39.50 Data Source with ID List. Invalid argument " + e.getMessage() + ".");
        } catch (IncompatibleInstanceException e) {
            createErrorMessage(out, MessageType.INCOMPATIBLE_TYPE, "Error updating a Z39.50 Data Source with ID Sequence. Data Source with id \"" + id + "\" was not a Z39.50 Data Source with ID Sequence.");
        }
    }

    public void updateDataSourceFtp(OutputStream out, String id, String description, String schema, String namespace,
                                    String metadataFormat, String isoFormat, String charset, String recordIdPolicyClass,
                                    String idXpath, String namespacePrefix, String namespaceUri, String recordXPath,
                                    String server, String user, String password, String ftpPath, String marcFormat) throws DocumentException {
        if(metadataFormat != null && schema != null && namespace != null)
            saveNewMetadataSchema(metadataFormat, schema, namespace, out);
        try {
            Map<String, String> namespaces = new TreeMap<String, String>();
            Map<String, MetadataTransformation> transformations = new HashMap<String, MetadataTransformation>();
            List<ExternalRestService> externalServices = new ArrayList<ExternalRestService>();

            DataSourceContainerDefault dataSourceContainerOld = (DataSourceContainerDefault)(ConfigSingleton.getRepoxContextUtil().getRepoxManager().getDataManager().getDataSourceContainer(id));if(dataSourceContainerOld != null){
                DataSourceDirectoryImporter dataSourceOld = (DataSourceDirectoryImporter)dataSourceContainerOld.getDataSource();
                if(description == null)
                    description = dataSourceOld.getDescription();
                if(schema == null)
                    schema = dataSourceOld.getSchema();
                if(namespace == null)
                    namespace = dataSourceOld.getNamespace();
                if(metadataFormat == null)
                    metadataFormat = dataSourceOld.getMetadataFormat();

                if(isoFormat == null && metadataFormat.equals(MetadataFormat.ISO2709.toString())){
                    if(dataSourceOld.getExtractStrategy() instanceof Iso2709FileExtract) {
                        Iso2709FileExtract extractStrategy = (Iso2709FileExtract) dataSourceOld.getExtractStrategy();
                        isoFormat = extractStrategy.getIsoImplementationClass().toString();
                    }
                    else
                        throw new InvalidArgumentsException("isoFormat is missing");
                }
                if(charset == null && dataSourceOld.getCharacterEncoding() != null)
                    charset = dataSourceOld.getCharacterEncoding().toString();

                if(recordIdPolicyClass == null){
                    if(dataSourceOld.getRecordIdPolicy() instanceof IdExtracted)
                        recordIdPolicyClass = IdExtracted.class.getSimpleName();
                    else if(dataSourceOld.getRecordIdPolicy() instanceof IdGenerated)
                        recordIdPolicyClass = IdGenerated.class.getSimpleName();
                }

                if(recordIdPolicyClass.equals(IdExtracted.class.getSimpleName())){
                    if(idXpath == null && dataSourceOld.getRecordIdPolicy() instanceof IdExtracted) {
                        IdExtracted idExtracted = (IdExtracted) dataSourceOld.getRecordIdPolicy();
                        idXpath = idExtracted.getIdentifierXpath();
                    }
                    if(idXpath == null)
                        throw new InvalidArgumentsException("idXpath is missing");

                    if(namespacePrefix == null && namespaceUri == null &&
                            dataSourceOld.getRecordIdPolicy() instanceof IdExtracted) {
                        IdExtracted idExtracted = (IdExtracted) dataSourceOld.getRecordIdPolicy();
                        namespaces = idExtracted.getNamespaces();
                    }
                    else if(namespacePrefix == null &&
                            dataSourceOld.getRecordIdPolicy() instanceof IdExtracted) {
                        // update to new namespaceUri
                        IdExtracted idExtracted = (IdExtracted) dataSourceOld.getRecordIdPolicy();

                        if(idExtracted.getNamespaces() != null && idExtracted.getNamespaces().size() > 0) {
                            for(String prefix : idExtracted.getNamespaces().keySet()) {
                                namespacePrefix = prefix;
                                break;
                            }
                            namespaces.put(namespacePrefix, namespaceUri);
                        }
                        else
                            throw new InvalidArgumentsException("namespacePrefix is missing");
                    }
                    else if(namespaceUri == null &&
                            dataSourceOld.getRecordIdPolicy() instanceof IdExtracted) {
                        // update to new namespacePrefix
                        IdExtracted idExtracted = (IdExtracted) dataSourceOld.getRecordIdPolicy();

                        if(idExtracted.getNamespaces() != null && idExtracted.getNamespaces().size() > 0) {
                            for(String uri : idExtracted.getNamespaces().values()) {
                                namespaceUri = uri;
                                break;
                            }
                            namespaces.put(namespacePrefix, namespaceUri);
                        }
                        else
                            throw new InvalidArgumentsException("namespaceUri is missing");
                    }
                    else {
                        // use the new values
                        if(namespacePrefix != null && namespaceUri != null) {
                            namespaces.put(namespacePrefix, namespaceUri);
                        }
                        else if(namespacePrefix != null && namespaceUri == null) {
                            throw new InvalidArgumentsException("namespaceUri is missing");
                        }
                        else if(namespacePrefix == null && namespaceUri != null) {
                            throw new InvalidArgumentsException("namespacePrefix is missing");
                        }
                    }
                }
                else{
                    // IdGenerated - empty fields
                    idXpath = null;
                    namespaces = new TreeMap<String, String>();
                }

                if(recordXPath == null)
                    recordXPath = dataSourceOld.getRecordXPath();
                if(server == null)
                    server = ((DataSourceFtp)dataSourceOld.getRetrieveStrategy()).getServer();
                if(user == null)
                    user = ((DataSourceFtp)dataSourceOld.getRetrieveStrategy()).getUser();
                if(password == null)
                    password = ((DataSourceFtp)dataSourceOld.getRetrieveStrategy()).getPassword();
                if(ftpPath == null)
                    ftpPath = ((DataSourceFtp)dataSourceOld.getRetrieveStrategy()).getFtpPath();
                if(marcFormat == null)
                    marcFormat = dataSourceOld.getMarcFormat();
                if(dataSourceOld.getMetadataTransformations().size() > 0)
                    transformations = dataSourceOld.getMetadataTransformations();
                if(dataSourceOld.getExternalRestServices().size() > 0)
                    externalServices = dataSourceOld.getExternalRestServices();
            }

            if(user == null)
                user = "";

            if(password == null)
                password = "";

            DataSource dataSource = ((DataManagerDefault)ConfigSingleton.getRepoxContextUtil().getRepoxManager().getDataManager()).updateDataSourceFtp(id, id,
                    description, schema, namespace, metadataFormat, isoFormat, charset,
                    recordIdPolicyClass, idXpath, namespaces, recordXPath, server, user, password,
                    ftpPath, transformations, externalServices, marcFormat, false);
            DataSourceContainer dataSourceContainer = ConfigSingleton.getRepoxContextUtil().getRepoxManager().getDataManager().getDataSourceContainer(dataSource.getId());
            RestUtils.writeRestResponse(out, dataSourceContainer.createElement());
        } catch (IOException e) {
            createErrorMessage(out, MessageType.OTHER, "Error updating Data Source FTP.");
        } catch (ObjectNotFoundException e) {
            createErrorMessage(out, MessageType.NOT_FOUND, "Error updating Data Source FTP. Data Provider was not found.");
        } catch (InvalidArgumentsException e) {
            createErrorMessage(out, MessageType.INVALID_ARGUMENTS, "Error updating Data Source FTP. Invalid argument " + e.getMessage() + ".");
        } catch (IncompatibleInstanceException e) {
            createErrorMessage(out, MessageType.INCOMPATIBLE_TYPE, "Error updating Data Source FTP. Data Source with id \"" + id + "\" was not a FTP data source.");
        }

    }

    public void updateDataSourceHttp(OutputStream out, String id, String description, String schema, String namespace,
                                     String metadataFormat, String isoFormat, String charset, String recordIdPolicyClass,
                                     String idXpath, String namespacePrefix, String namespaceUri, String recordXPath,
                                     String url, String marcFormat) throws DocumentException {
        if(metadataFormat != null && schema != null && namespace != null)
            saveNewMetadataSchema(metadataFormat, schema, namespace, out);
        try {
            Map<String, String> namespaces = new TreeMap<String, String>();
            Map<String, MetadataTransformation> transformations = new HashMap<String, MetadataTransformation>();
            List<ExternalRestService> externalServices = new ArrayList<ExternalRestService>();

            DataSourceContainerDefault dataSourceContainerOld = (DataSourceContainerDefault)(ConfigSingleton.getRepoxContextUtil().getRepoxManager().getDataManager().getDataSourceContainer(id));if(dataSourceContainerOld != null){
                DataSourceDirectoryImporter dataSourceOld = (DataSourceDirectoryImporter)dataSourceContainerOld.getDataSource();
                if(description == null)
                    description = dataSourceOld.getDescription();
                if(schema == null)
                    schema = dataSourceOld.getSchema();
                if(namespace == null)
                    namespace = dataSourceOld.getNamespace();
                if(metadataFormat == null)
                    metadataFormat = dataSourceOld.getMetadataFormat();

                if(isoFormat == null && metadataFormat.equals(MetadataFormat.ISO2709.toString())){
                    if(dataSourceOld.getExtractStrategy() instanceof Iso2709FileExtract) {
                        Iso2709FileExtract extractStrategy = (Iso2709FileExtract) dataSourceOld.getExtractStrategy();
                        isoFormat = extractStrategy.getIsoImplementationClass().toString();
                    }
                    else
                        throw new InvalidArgumentsException("isoFormat is missing");
                }
                if(charset == null && dataSourceOld.getCharacterEncoding() != null)
                    charset = dataSourceOld.getCharacterEncoding().toString();

                if(recordIdPolicyClass == null){
                    if(dataSourceOld.getRecordIdPolicy() instanceof IdExtracted)
                        recordIdPolicyClass = IdExtracted.class.getSimpleName();
                    else if(dataSourceOld.getRecordIdPolicy() instanceof IdGenerated)
                        recordIdPolicyClass = IdGenerated.class.getSimpleName();
                }

                if(recordIdPolicyClass.equals(IdExtracted.class.getSimpleName())){
                    if(idXpath == null && dataSourceOld.getRecordIdPolicy() instanceof IdExtracted) {
                        IdExtracted idExtracted = (IdExtracted) dataSourceOld.getRecordIdPolicy();
                        idXpath = idExtracted.getIdentifierXpath();
                    }
                    if(idXpath == null)
                        throw new InvalidArgumentsException("idXpath is missing");

                    if(namespacePrefix == null && namespaceUri == null &&
                            dataSourceOld.getRecordIdPolicy() instanceof IdExtracted) {
                        IdExtracted idExtracted = (IdExtracted) dataSourceOld.getRecordIdPolicy();
                        namespaces = idExtracted.getNamespaces();
                    }
                    else if(namespacePrefix == null &&
                            dataSourceOld.getRecordIdPolicy() instanceof IdExtracted) {
                        // update to new namespaceUri
                        IdExtracted idExtracted = (IdExtracted) dataSourceOld.getRecordIdPolicy();

                        if(idExtracted.getNamespaces() != null && idExtracted.getNamespaces().size() > 0) {
                            for(String prefix : idExtracted.getNamespaces().keySet()) {
                                namespacePrefix = prefix;
                                break;
                            }
                            namespaces.put(namespacePrefix, namespaceUri);
                        }
                        else
                            throw new InvalidArgumentsException("namespacePrefix is missing");
                    }
                    else if(namespaceUri == null &&
                            dataSourceOld.getRecordIdPolicy() instanceof IdExtracted) {
                        // update to new namespacePrefix
                        IdExtracted idExtracted = (IdExtracted) dataSourceOld.getRecordIdPolicy();

                        if(idExtracted.getNamespaces() != null && idExtracted.getNamespaces().size() > 0) {
                            for(String uri : idExtracted.getNamespaces().values()) {
                                namespaceUri = uri;
                                break;
                            }
                            namespaces.put(namespacePrefix, namespaceUri);
                        }
                        else
                            throw new InvalidArgumentsException("namespaceUri is missing");
                    }
                    else {
                        // use the new values
                        if(namespacePrefix != null && namespaceUri != null) {
                            namespaces.put(namespacePrefix, namespaceUri);
                        }
                        else if(namespacePrefix != null && namespaceUri == null) {
                            throw new InvalidArgumentsException("namespaceUri is missing");
                        }
                        else if(namespacePrefix == null && namespaceUri != null) {
                            throw new InvalidArgumentsException("namespacePrefix is missing");
                        }
                    }
                }
                else{
                    // IdGenerated - empty fields
                    idXpath = null;
                    namespaces = new TreeMap<String, String>();
                }
                if(recordXPath == null)
                    recordXPath = dataSourceOld.getRecordXPath();
                if(url == null)
                    url = ((DataSourceHttp)dataSourceOld.getRetrieveStrategy()).getUrl();;
                if(marcFormat == null)
                    marcFormat = dataSourceOld.getMarcFormat();
                if(dataSourceOld.getMetadataTransformations().size() > 0)
                    transformations = dataSourceOld.getMetadataTransformations();
                if(dataSourceOld.getExternalRestServices().size() > 0)
                    externalServices = dataSourceOld.getExternalRestServices();
            }

            DataSource dataSource = ((DataManagerDefault)ConfigSingleton.getRepoxContextUtil().getRepoxManager().getDataManager()).updateDataSourceHttp(id, id,
                    description, schema, namespace, metadataFormat, isoFormat, charset,
                    recordIdPolicyClass, idXpath, namespaces, recordXPath, url,
                    transformations, externalServices, marcFormat, false);
            DataSourceContainer dataSourceContainer = ConfigSingleton.getRepoxContextUtil().getRepoxManager().getDataManager().getDataSourceContainer(dataSource.getId());
            RestUtils.writeRestResponse(out, dataSourceContainer.createElement());
        } catch (IOException e) {
            createErrorMessage(out, MessageType.OTHER, "Error updating Data Source HTTP.");
        } catch (ObjectNotFoundException e) {
            createErrorMessage(out, MessageType.NOT_FOUND, "Error updating a Data Source HTTP. Data Provider was not found.");
        } catch (InvalidArgumentsException e) {
            createErrorMessage(out, MessageType.INVALID_ARGUMENTS, "Error updating Data Source HTTP. Invalid argument " + e.getMessage() + ".");
        } catch (IncompatibleInstanceException e) {
            createErrorMessage(out, MessageType.INCOMPATIBLE_TYPE, "Error updating Data Source HTTP. Data Source with id \"" + id + "\" was not a HTTP data source.");
        }
    }

    public void updateDataSourceFolder(OutputStream out, String id, String description, String schema, String namespace,
                                       String metadataFormat, String isoFormat, String charset, String recordIdPolicyClass,
                                       String idXpath, String namespacePrefix, String namespaceUri, String recordXPath,
                                       String sourcesDirPath, String marcFormat) throws IOException, DocumentException {
        if(metadataFormat != null && schema != null && namespace != null)
            saveNewMetadataSchema(metadataFormat, schema, namespace, out);
        try {
            Map<String, String> namespaces = new TreeMap<String, String>();
            Map<String, MetadataTransformation> transformations = new HashMap<String, MetadataTransformation>();
            List<ExternalRestService> externalServices = new ArrayList<ExternalRestService>();

            DataSourceContainerDefault dataSourceContainerOld = (DataSourceContainerDefault)(ConfigSingleton.getRepoxContextUtil().getRepoxManager().getDataManager().getDataSourceContainer(id));if(dataSourceContainerOld != null){
                DataSourceDirectoryImporter dataSourceOld = (DataSourceDirectoryImporter)dataSourceContainerOld.getDataSource();
                if(description == null)
                    description = dataSourceOld.getDescription();
                if(schema == null)
                    schema = dataSourceOld.getSchema();
                if(namespace == null)
                    namespace = dataSourceOld.getNamespace();
                if(metadataFormat == null)
                    metadataFormat = dataSourceOld.getMetadataFormat();

                if(isoFormat == null && metadataFormat.equals(MetadataFormat.ISO2709.toString())){
                    if(dataSourceOld.getExtractStrategy() instanceof Iso2709FileExtract) {
                        Iso2709FileExtract extractStrategy = (Iso2709FileExtract) dataSourceOld.getExtractStrategy();
                        isoFormat = extractStrategy.getIsoImplementationClass().toString();
                    }
                    else
                        throw new InvalidArgumentsException("isoFormat is missing");
                }
                //
                if(charset == null && dataSourceOld.getCharacterEncoding() != null)
                    charset = dataSourceOld.getCharacterEncoding().toString();
                //

                if(recordIdPolicyClass == null){
                    if(dataSourceOld.getRecordIdPolicy() instanceof IdExtracted)
                        recordIdPolicyClass = IdExtracted.class.getSimpleName();
                    else if(dataSourceOld.getRecordIdPolicy() instanceof IdGenerated)
                        recordIdPolicyClass = IdGenerated.class.getSimpleName();
                }

                if(recordIdPolicyClass.equals(IdExtracted.class.getSimpleName())){
                    if(idXpath == null && dataSourceOld.getRecordIdPolicy() instanceof IdExtracted) {
                        IdExtracted idExtracted = (IdExtracted) dataSourceOld.getRecordIdPolicy();
                        idXpath = idExtracted.getIdentifierXpath();
                    }
                    if(idXpath == null)
                        throw new InvalidArgumentsException("idXpath is missing");

                    if(namespacePrefix == null && namespaceUri == null &&
                            dataSourceOld.getRecordIdPolicy() instanceof IdExtracted) {
                        IdExtracted idExtracted = (IdExtracted) dataSourceOld.getRecordIdPolicy();
                        namespaces = idExtracted.getNamespaces();
                    }
                    else if(namespacePrefix == null &&
                            dataSourceOld.getRecordIdPolicy() instanceof IdExtracted) {
                        // update to new namespaceUri
                        IdExtracted idExtracted = (IdExtracted) dataSourceOld.getRecordIdPolicy();

                        if(idExtracted.getNamespaces() != null && idExtracted.getNamespaces().size() > 0) {
                            for(String prefix : idExtracted.getNamespaces().keySet()) {
                                namespacePrefix = prefix;
                                break;
                            }
                            namespaces.put(namespacePrefix, namespaceUri);
                        }
                        else
                            throw new InvalidArgumentsException("namespacePrefix is missing");
                    }
                    else if(namespaceUri == null &&
                            dataSourceOld.getRecordIdPolicy() instanceof IdExtracted) {
                        // update to new namespacePrefix
                        IdExtracted idExtracted = (IdExtracted) dataSourceOld.getRecordIdPolicy();

                        if(idExtracted.getNamespaces() != null && idExtracted.getNamespaces().size() > 0) {
                            for(String uri : idExtracted.getNamespaces().values()) {
                                namespaceUri = uri;
                                break;
                            }
                            namespaces.put(namespacePrefix, namespaceUri);
                        }
                        else
                            throw new InvalidArgumentsException("namespaceUri is missing");
                    }
                    else {
                        // use the new values
                        if(namespacePrefix != null && namespaceUri != null) {
                            namespaces.put(namespacePrefix, namespaceUri);
                        }
                        else if(namespacePrefix != null && namespaceUri == null) {
                            throw new InvalidArgumentsException("namespaceUri is missing");
                        }
                        else if(namespacePrefix == null && namespaceUri != null) {
                            throw new InvalidArgumentsException("namespacePrefix is missing");
                        }
                    }
                }
                else{
                    // IdGenerated - empty fields
                    idXpath = null;
                    namespaces = new TreeMap<String, String>();
                }

                if(recordXPath == null)
                    recordXPath = dataSourceOld.getRecordXPath();
                if(sourcesDirPath == null)
                    sourcesDirPath = dataSourceOld.getSourcesDirPath();
                if(marcFormat == null)
                    marcFormat = dataSourceOld.getMarcFormat();

                if(dataSourceOld.getMetadataTransformations().size() > 0)
                    transformations = dataSourceOld.getMetadataTransformations();

                if(dataSourceOld.getExternalRestServices().size() > 0)
                    externalServices = dataSourceOld.getExternalRestServices();
            }

            DataSource dataSource = ((DataManagerDefault)ConfigSingleton.getRepoxContextUtil().getRepoxManager().getDataManager()).updateDataSourceFolder(id, id,
                    description, schema, namespace, metadataFormat, isoFormat, charset,
                    recordIdPolicyClass, idXpath, namespaces, recordXPath, sourcesDirPath,
                    transformations, externalServices, marcFormat, false);
            DataSourceContainer dataSourceContainer = ConfigSingleton.getRepoxContextUtil().getRepoxManager().getDataManager().getDataSourceContainer(dataSource.getId());
            RestUtils.writeRestResponse(out, dataSourceContainer.createElement());
        } catch (ObjectNotFoundException e) {
            createErrorMessage(out, MessageType.NOT_FOUND, "Error updating a Data Source Folder. Data Source with id \"" + id + "\" was not found.");
        } catch (InvalidArgumentsException e) {
            createErrorMessage(out, MessageType.INVALID_ARGUMENTS, "Error creating Data Source. Invalid argument " + e.getMessage() + ".");
        } catch (IncompatibleInstanceException e) {
            createErrorMessage(out, MessageType.INCOMPATIBLE_TYPE, "Error updating Data Source Folder. Data Source with id \"" + id + "\" was not a Folder data source.");
        }
    }

    @Override
    public void deleteDataSource(OutputStream out, String id) throws DocumentException {
        try {
            ConfigSingleton.getRepoxContextUtil().getRepoxManager().getDataManager().deleteDataSourceContainer(id);
            Element currentDataProviderElement = DocumentHelper.createElement("success");
            currentDataProviderElement.setText("Data Source with id \"" + id + "\" was successfully deleted.");
            RestUtils.writeRestResponse(out, currentDataProviderElement);
        } catch (IOException e) {
            createErrorMessage(out, MessageType.OTHER, "Error deleting Data Source with id \"" + id + "\".");
        } catch (ObjectNotFoundException e) {
            createErrorMessage(out, MessageType.NOT_FOUND, "Error deleting Data Source with id \"" + id + "\".");
        }
    }

    @Override
    public void getDataSource(OutputStream out, String dataSourceId) throws DocumentException {
        try {
            DataSourceContainer dataSourceContainer = ConfigSingleton.getRepoxContextUtil().getRepoxManager().getDataManager().getDataSourceContainer(dataSourceId);

            if(dataSourceContainer != null){
                Element dataSourcesElement = dataSourceContainer.getDataSource().createElement();
                RestUtils.writeRestResponse(out, dataSourcesElement);
            }
            else{
                createErrorMessage(out, MessageType.NOT_FOUND, "Error retrieving Data Source. Data Source with id \"" + dataSourceId + "\" was not found.");
            }
        }
        catch (IOException e) {
            createErrorMessage(out, MessageType.OTHER, "Error retrieving Data Source with id \"" + dataSourceId + "\".");
        }
    }

    @Override
    public void countRecordsDataSource(OutputStream out, String dataSourceId) throws DocumentException, IOException, SQLException {
        DataSourceContainer dataSourceContainer = ConfigSingleton.getRepoxContextUtil().getRepoxManager().getDataManager().getDataSourceContainer(dataSourceId);
        if(dataSourceContainer != null){
            int recordCount = ConfigSingleton.getRepoxContextUtil().getRepoxManager().getRecordCountManager().getRecordCount(dataSourceId).getCount();

            Element successElement = DocumentHelper.createElement("recordCount");
            successElement.setText(String.valueOf(recordCount));
            RestUtils.writeRestResponse(out, successElement);
        }
        else{
            createErrorMessage(out, MessageType.NOT_FOUND, "Error counting records. Data source with ID \"" + dataSourceId + "\" was not found.");
        }
    }

    @Override
    public void lastIngestionDateDataSource(OutputStream out, String dataSourceId) throws DocumentException, IOException, SQLException {
        DataSourceContainer dataSourceContainer = ConfigSingleton.getRepoxContextUtil().getRepoxManager().getDataManager().getDataSourceContainer(dataSourceId);
        if(dataSourceContainer != null){
            String lastIngestionDate = dataSourceContainer.getDataSource().getSynchronizationDateString();

            Element successElement = DocumentHelper.createElement("lastIngestionDate");
            successElement.setText(String.valueOf(lastIngestionDate));
            RestUtils.writeRestResponse(out, successElement);
        }
        else{
            createErrorMessage(out, MessageType.NOT_FOUND, "Error retrieving last ingestion date. Data source with ID \"" + dataSourceId + "\" was not found.");
        }
    }

    @Override
    public void startIngestDataSource(OutputStream out, String dataSourceId, boolean fullIngest) throws DocumentException, IOException {
        try {
            ConfigSingleton.getRepoxContextUtil().getRepoxManager().getDataManager().startIngestDataSource(dataSourceId, fullIngest);
            Element successElement = DocumentHelper.createElement("success");
            successElement.setText("Harvest of Data Source with ID \"" + dataSourceId + "\" will start in a few seconds.");
            RestUtils.writeRestResponse(out, successElement);
        } catch (NoSuchMethodException e) {
            createErrorMessage(out, MessageType.OTHER, "Error starting Data Source ingestion.");
        } catch (ClassNotFoundException e) {
            createErrorMessage(out, MessageType.OTHER, "Error starting Data Source ingestion.");
        } catch (ParseException e) {
            createErrorMessage(out, MessageType.OTHER, "Error starting Data Source ingestion.");
        } catch (ObjectNotFoundException e) {
            createErrorMessage(out, MessageType.NOT_FOUND, "Error starting the Data Source ingestion. ID \"" + dataSourceId + "\" was not found.");
        } catch (AlreadyExistsException e) {
            createErrorMessage(out, MessageType.ALREADY_EXISTS, "Error starting the Data Source ingestion. ID \"" + dataSourceId + "\" is already harvesting.");
        }
    }

    @Override
    public void stopIngestDataSource(OutputStream out, String dataSourceId) throws DocumentException, IOException, NoSuchMethodException {
        try {
            ConfigSingleton.getRepoxContextUtil().getRepoxManager().getDataManager().stopIngestDataSource(dataSourceId, Task.Status.CANCELED);
            Element successElement = DocumentHelper.createElement("success");
            successElement.setText("Task for Data Source with ID \"" + dataSourceId + "\" was stopped successfully.");
            RestUtils.writeRestResponse(out, successElement);
        } catch (ObjectNotFoundException e) {
            if(e.getMessage().equals(dataSourceId)) {
                createErrorMessage(out, MessageType.OTHER, "Error stopping the Data Source task. No task is running for Data Source with ID \"" + dataSourceId + "\".");
            }
            else{
                createErrorMessage(out, MessageType.NOT_FOUND, "Error stopping the Data Source task. ID \"" + dataSourceId + "\" was not found.");
            }
        } catch (ClassNotFoundException e) {
            createErrorMessage(out, MessageType.OTHER, "Error stopping the Data Source task.");
        } catch (ParseException e) {
            createErrorMessage(out, MessageType.OTHER, "Error stopping the Data Source task.");
        }
    }


    @Override
    public void scheduleIngestDataSource(OutputStream out, String dataSourceId, String firstRunDate, String firstRunHour,
                                         String frequency, String xmonths, String fullIngest) throws DocumentException, IOException {
        DataSourceContainer dataSourceContainer = ConfigSingleton.getRepoxContextUtil().getRepoxManager().getDataManager().getDataSourceContainer(dataSourceId);
        if(dataSourceContainer != null){
            DataSource dataSource = dataSourceContainer.getDataSource();
            String newTaskId = dataSource.getNewTaskId();
            ScheduledTask scheduledTask = new ScheduledTask();
            scheduledTask.setId(newTaskId);
            scheduledTask.setDate(firstRunDate);
            scheduledTask.setHour(Integer.valueOf(firstRunHour.split(":")[0]));
            scheduledTask.setMinute(Integer.valueOf(firstRunHour.split(":")[1]));

            if(frequency.equalsIgnoreCase("once")){
                scheduledTask.setFrequency(ScheduledTask.Frequency.ONCE);
            }
            else if(frequency.equalsIgnoreCase("daily")){
                scheduledTask.setFrequency(ScheduledTask.Frequency.DAILY);
            }
            else if(frequency.equalsIgnoreCase("weekly")){
                scheduledTask.setFrequency(ScheduledTask.Frequency.WEEKLY);
            }
            else if(frequency.equalsIgnoreCase("xmonthly")){
                scheduledTask.setFrequency(ScheduledTask.Frequency.XMONTHLY);
                scheduledTask.setXmonths(Integer.valueOf(xmonths));
            }

            scheduledTask.setTaskClass(IngestDataSource.class);
            // Parameter 0 -> taskId; Parameter 1 -> dataSourceId; Parameter 2 -> isFullIngest?
            String[] parameters = new String[]{newTaskId, dataSource.getId(), (Boolean.valueOf(fullIngest)).toString()};
            scheduledTask.setParameters(parameters);

            if(ConfigSingleton.getRepoxContextUtil().getRepoxManager().getTaskManager().taskAlreadyExists(dataSource.getId(), DateUtil.date2String(scheduledTask.getFirstRun().getTime(), TimeUtil.LONG_DATE_FORMAT_NO_SECS), scheduledTask.getFrequency(), fullIngest)){
                createErrorMessage(out, MessageType.ALREADY_EXISTS, "Error scheduling the Data Source ingestion. A task for this specific hour and data source ID \"" + dataSourceId + "\" is already scheduled.");
            }
            else{
                ConfigSingleton.getRepoxContextUtil().getRepoxManager().getTaskManager().saveTask(scheduledTask);

                Element successElement = DocumentHelper.createElement("success");
                successElement.setText("Ingest successfully scheduled for Data Source with ID \"" + dataSourceId + "\" .");
                RestUtils.writeRestResponse(out, successElement);
            }
        }
        else{
            createErrorMessage(out, MessageType.NOT_FOUND, "Error scheduling the Data Source ingestion. ID \"" + dataSourceId + "\" was not found.");
        }
    }

    @Override
    public void scheduleListDataSource(OutputStream out, String dataSourceId) throws DocumentException, IOException {
        DataSourceContainer dataSourceContainer = ConfigSingleton.getRepoxContextUtil().getRepoxManager().getDataManager().getDataSourceContainer(dataSourceId);
        if(dataSourceContainer != null){
            Element scheduleTasksElement = DocumentHelper.createElement("scheduleTasks");

            for (ScheduledTask scheduledTask : ConfigSingleton.getRepoxContextUtil().getRepoxManager().getTaskManager().getScheduledTasks()) {
                if(scheduledTask.getParameters()[1].equals(dataSourceId)){
                    Element scheduledTaskElement = scheduleTasksElement.addElement("task");
                    scheduledTaskElement.addAttribute("id", scheduledTask.getId());

                    Element timeElement = scheduledTaskElement.addElement("time");
                    timeElement.setText(DateUtil.date2String(scheduledTask.getFirstRun().getTime(), TimeUtil.LONG_DATE_FORMAT_NO_SECS));

                    Element frequencyElement = scheduledTaskElement.addElement("frequency");
                    frequencyElement.addAttribute("type", scheduledTask.getFrequency().toString());
                    if(scheduledTask.getFrequency().equals(ScheduledTask.Frequency.XMONTHLY)) {
                        frequencyElement.addAttribute("xmonthsPeriod", scheduledTask.getXmonths().toString());
                    }

                    scheduledTaskElement.addElement("fullIngest").addText(scheduledTask.getParameters()[2]);
                }
            }
            RestUtils.writeRestResponse(out, scheduleTasksElement);
        }
        else{
            createErrorMessage(out, MessageType.NOT_FOUND, "Error scheduling the Data Source ingestion. ID \"" + dataSourceId + "\" was not found.");
        }
    }

    @Override
    public void harvestStatusDataSource(OutputStream out, String dataSourceId) throws DocumentException, IOException {
        DataSourceContainer dataSourceContainer = ConfigSingleton.getRepoxContextUtil().getRepoxManager().getDataManager().getDataSourceContainer(dataSourceId);
        if(dataSourceContainer != null){
            DataSource dataSource = dataSourceContainer.getDataSource();

            Element harvestingStatus = DocumentHelper.createElement("harvestingStatus");

            if(dataSource.getStatus() != null){
                if(dataSource.getStatusString().equalsIgnoreCase(DataSource.StatusDS.RUNNING.name())){
                    try {
                        ConfigSingleton.getRepoxContextUtil().getRepoxManager().getRecordCountManager().getRecordCount(dataSource.getId(), true);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }

                    Element statusMessage = DocumentHelper.createElement("status");
                    statusMessage.setText(dataSource.getStatusString());
                    harvestingStatus.add(statusMessage);

                    long timeLeft = dataSource.getTimeLeft();
                    if(timeLeft != -1){
                        Element timeLeftMessage = DocumentHelper.createElement("timeLeft");
                        timeLeftMessage.setText(String.valueOf(timeLeft));
                        harvestingStatus.add(timeLeftMessage);
                    }

                    float percentage = dataSource.getPercentage();
                    if(percentage >= 0){
                        Element percentageMessage = DocumentHelper.createElement("percentage");
                        percentageMessage.setText(String.valueOf(percentage));
                        harvestingStatus.add(percentageMessage);
                    }

                    float totalRecords = dataSource.getTotalRecords2Harvest();
                    if(totalRecords > 0){
                        Element recordsMessage = DocumentHelper.createElement("records");
                        try {
                            recordsMessage.setText(String.valueOf(dataSource.getIntNumberRecords() + "/" + dataSource.getNumberOfRecords2HarvestStr()));
                            harvestingStatus.add(recordsMessage);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                    RestUtils.writeRestResponse(out, harvestingStatus);
                }
                else{
                    Element statusMessage = DocumentHelper.createElement("status");
                    statusMessage.setText(dataSource.getStatusString());
                    harvestingStatus.add(statusMessage);

                    if(dataSource.getStatusString().equalsIgnoreCase(DataSource.StatusDS.OK.name())){
                        Element recordsMessage = DocumentHelper.createElement("records");
                        try {
                            recordsMessage.setText(String.valueOf(dataSource.getIntNumberRecords()) + "/" + String.valueOf(dataSource.getIntNumberRecords()));
                            harvestingStatus.add(recordsMessage);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            else{
                Element statusMessage = DocumentHelper.createElement("status");
                statusMessage.setText("undefined");
                harvestingStatus.add(statusMessage);
            }
            RestUtils.writeRestResponse(out, harvestingStatus);
        }
        else{
            createErrorMessage(out, MessageType.NOT_FOUND, "Error scheduling the Data Source ingestion. ID \"" + dataSourceId + "\" was not found.");
        }
    }

    @Override
    public void logDataSource(OutputStream out, String dataSourceId) throws DocumentException, IOException {
        DataSourceContainer dataSourceContainer = ConfigSingleton.getRepoxContextUtil().getRepoxManager().getDataManager().getDataSourceContainer(dataSourceId);
        if(dataSourceContainer != null){
            DataSource dataSource = dataSourceContainer.getDataSource();
            if(dataSource.getLogFilenames().size() > 0){
                Element logElement;
                File logFile = new File(dataSource.getLogsDir(), dataSource.getLogFilenames().get(0));
                try{
                    SAXReader reader = new SAXReader();
                    Document document = reader.read(logFile);
                    logElement = document.getRootElement();
                }catch (DocumentException e){
                    ArrayList<String> logFileContent = FileUtil.readFile(new File(dataSource.getLogsDir(), dataSource.getLogFilenames().get(0)));

                    logElement = DocumentHelper.createElement("log");

                    for (String line : logFileContent) {
                        logElement.addElement("line").addText(line);
                    }
                }

                RestUtils.writeRestResponse(out, logElement);
            }
            else{
                createErrorMessage(out, MessageType.OTHER, "Error showing log file for Data Source. There is no logs for Data Source with ID \"" + dataSourceId + "\".");
            }
        }
        else{
            createErrorMessage(out, MessageType.NOT_FOUND, "Error scheduling the Data Source ingestion. ID \"" + dataSourceId + "\" was not found.");
        }
    }

    @Override
    public void harvestingDataSources(OutputStream out) throws DocumentException, IOException {
        Element runningTasksElement = DocumentHelper.createElement("runningTasks");
        for (Task task : ConfigSingleton.getRepoxContextUtil().getRepoxManager().getTaskManager().getRunningTasks()){
            runningTasksElement.addElement("dataSource").addText(task.getParameters()[1]);
        }
        RestUtils.writeRestResponse(out, runningTasksElement);
    }

    public void startExportDataSource(OutputStream out, String dataSourceId, String recordsPerFile) throws DocumentException, IOException {
        startExportDataSource(out, dataSourceId, recordsPerFile, null);
    }

    @Override
    public void startExportDataSource(OutputStream out, String dataSourceId, String recordsPerFile, String metadataExportFormat) throws DocumentException, IOException {
        try {
            ConfigSingleton.getRepoxContextUtil().getRepoxManager().getDataManager().startExportDataSource(dataSourceId, recordsPerFile, metadataExportFormat);
            Element successElement = DocumentHelper.createElement("success");
            successElement.setText("Exportation of Data Source with ID \"" + dataSourceId + "\" will start in a few seconds.");
            RestUtils.writeRestResponse(out, successElement);
        } catch (AlreadyExistsException e) {
            createErrorMessage(out, MessageType.ALREADY_EXISTS, "Error starting the Data Source exportation. ID \"" + dataSourceId + "\" is already exporting.");
        } catch (ClassNotFoundException e) {
            createErrorMessage(out, MessageType.OTHER, "Error starting Data Source exportation.");
        } catch (NoSuchMethodException e) {
            createErrorMessage(out, MessageType.OTHER, "Error starting Data Source exportation.");
        } catch (ParseException e) {
            createErrorMessage(out, MessageType.OTHER, "Error starting Data Source exportation.");
        } catch (ObjectNotFoundException e) {
            createErrorMessage(out, MessageType.NOT_FOUND, "Error starting the Data Source exportation. ID \"" + dataSourceId + "\" was not found.");
        }
    }


    @Override
    public void getRecord(OutputStream out, Urn recordUrn) throws IOException, DocumentException, SQLException {
        byte[] recordMetadata = ConfigSingleton.getRepoxContextUtil().getRepoxManager().getAccessPointsManager().getRecord(recordUrn).getMetadata();
        SAXReader reader = new SAXReader();
        Document recordDocument = reader.read(new ByteArrayInputStream(recordMetadata));

        Element recordResultElement = DocumentHelper.createElement("recordResult");
        recordResultElement.addAttribute("urn", recordUrn.toString());
        Node detachedRecordNode = recordDocument.getRootElement().detach();
        recordResultElement.add(detachedRecordNode);
        RestUtils.writeRestResponse(out, recordResultElement);
    }

    @Override
    public void saveRecord(OutputStream out, String recordId, String dataSourceId, String recordString) throws IOException, DocumentException {
        DataSourceContainer dataSourceContainer = ConfigSingleton.getRepoxContextUtil().getRepoxManager().getDataManager().getDataSourceContainer(dataSourceId);
        if(dataSourceContainer != null){
            try {
                Element recordRoot = (Element) DocumentHelper.parseText(recordString).getRootElement().detach();
                DataSource dataSource = dataSourceContainer.getDataSource();
                RecordRepox recordRepox = dataSource.getRecordIdPolicy().createRecordRepox(recordRoot, recordId, false, false);
                ConfigSingleton.getRepoxContextUtil().getRepoxManager().getAccessPointsManager().processRecord(dataSource, recordRepox);

                Element successElement = DocumentHelper.createElement("success");
                successElement.setText("Record with id " + recordRepox.getId() + " saved successfully");
                RestUtils.writeRestResponse(out, successElement);
            }
            catch(Exception e) {
                createErrorMessage(out, MessageType.OTHER, "Unable to save Record");
            }
        }
        else{
            createErrorMessage(out, MessageType.NOT_FOUND, "Unable to save or update record. Data source with ID \"" + dataSourceId + "\" was not found.");
        }
    }

    @Override
    public void deleteRecord(OutputStream out, String recordId) throws IOException {
        try {
            Urn recordUrn = new Urn(recordId);
            ConfigSingleton.getRepoxContextUtil().getRepoxManager().getAccessPointsManager().deleteRecord(recordUrn);
            ConfigSingleton.getRepoxContextUtil().getRepoxManager().getRecordCountManager().updateDeletedRecordsCount(recordUrn.getDataSourceId(), 1);

            Element successElement = DocumentHelper.createElement("success");
            successElement.setText("Record with id " + recordId + " marked as deleted successfully");

            ConfigSingleton.getRepoxContextUtil().getRepoxManager().getRecordCountManager().getRecordCount(recordUrn.getDataSourceId()).setCount(ConfigSingleton.getRepoxContextUtil().getRepoxManager().getRecordCountManager().getRecordCount(recordUrn.getDataSourceId()).getCount() - 1);


            RestUtils.writeRestResponse(out, successElement);
        }
        catch(Exception e) {
            createErrorMessage(out, MessageType.OTHER, "Unable to permanently remove Record");
        }

    }

    @Override
    public void eraseRecord(OutputStream out, String recordId) throws IOException {
        try {
            Urn recordUrn = new Urn(recordId);
            ConfigSingleton.getRepoxContextUtil().getRepoxManager().getAccessPointsManager().removeRecord(recordUrn);

            ConfigSingleton.getRepoxContextUtil().getRepoxManager().getRecordCountManager().getRecordCount(recordUrn.getDataSourceId()).setLastLineCounted(ConfigSingleton.getRepoxContextUtil().getRepoxManager().getRecordCountManager().getRecordCount(recordUrn.getDataSourceId()).getLastLineCounted() - 1);
            ConfigSingleton.getRepoxContextUtil().getRepoxManager().getRecordCountManager().updateDeletedRecordsCount(recordUrn.getDataSourceId(), 1);

            Element successElement = DocumentHelper.createElement("success");
            successElement.setText("Record with id " + recordId + " permanently removed successfully");
            RestUtils.writeRestResponse(out, successElement);
        }
        catch(Exception e) {
            createErrorMessage(out, MessageType.OTHER, "Unable to permanently remove Record");
        }
    }

    public void createErrorMessage(OutputStream out, MessageType type, String cause) {
        Element errorMessage = DocumentHelper.createElement("error");
        errorMessage.addAttribute("type", type.name());
        errorMessage.addAttribute("requestURI", getRequestURI());
        errorMessage.addAttribute("cause", cause);

        try {
            RestUtils.writeRestResponse(out, errorMessage);
        } catch (IOException e) {
            e.printStackTrace();
            log.error("Error. RestUtils.writeRestResponse(out, errorMessage): " + e.getMessage());
        }
    }

    public void createMapping(OutputStream out, String id,String description,String srcSchemaId,String srcSchemaVersion,
                              String destSchemaId,String destSchemaVersion,String isXslVersion2,
                              String xslFilename,InputStream xsdFile){
        try {
            Document document = DocumentHelper.createDocument();
            Element root = document.addElement("response");

            File xsltDir = ConfigSingleton.getRepoxContextUtil().getRepoxManager().getMetadataTransformationManager().getXsltDir();
            TransformationsFileManager.Response result = TransformationsFileManager.writeXslFile(xslFilename + ".xsl", xsltDir,xsdFile);

            if(result == TransformationsFileManager.Response.ERROR){
                createErrorMessage(out, MessageType.OTHER, "Error creating Mapping: id \"" + id + "\" error during file saving.");
                return;
            }
            else if(result == TransformationsFileManager.Response.FILE_TOO_BIG){
                createErrorMessage(out, MessageType.OTHER, "Error creating Mapping: id \"" + id + "\" xsd file is too big.");
                return;
            }
            else if(result == TransformationsFileManager.Response.XSL_ALREADY_EXISTS){
                createErrorMessage(out, MessageType.ALREADY_EXISTS, "Error creating Mapping: id \"" + id + "\" xsd filename already exists.");
                return;
            }

            String srcXsdLink = ConfigSingleton.getRepoxContextUtil().getRepoxManager()
                    .getMetadataSchemaManager().getSchemaXSD(srcSchemaId,Double.valueOf(srcSchemaVersion));

            String destXsdLink = ConfigSingleton.getRepoxContextUtil().getRepoxManager()
                    .getMetadataSchemaManager().getSchemaXSD(destSchemaId,Double.valueOf(destSchemaVersion));

            MetadataTransformation mtdTransformation = new MetadataTransformation(id,
                    description,srcSchemaId,destSchemaId,xslFilename + ".xsl",
                    false,Boolean.valueOf(isXslVersion2),destXsdLink,"");
            mtdTransformation.setSourceSchema(srcXsdLink);
            mtdTransformation.setMDRCompliant(true);

            //If a file was uploaded, then erase its old files
            mtdTransformation.setDeleteOldFiles(true);

            ConfigSingleton.getRepoxContextUtil().getRepoxManager().getMetadataTransformationManager().
                    saveMetadataTransformation(mtdTransformation,"");

            root.addElement("status").setText("OK");
            RestUtils.writeRestResponse(out, root);
        } catch (SameStylesheetTransformationException e) {
            createErrorMessage(out, MessageType.OTHER, "Error creating Mapping: id \"" + id + "\" stylesheet already exists.");
        }catch (AlreadyExistsException e) {
            createErrorMessage(out, MessageType.ALREADY_EXISTS, "Error creating Mapping: id \"" + id + "\" already exists.");
        } catch (IOException e) {
            createErrorMessage(out, MessageType.OTHER, "Error creating Mapping: IO Error");
        } catch (DocumentException e) {
            createErrorMessage(out, MessageType.OTHER, "Error creating Mapping: Document Exception");
        } catch (NumberFormatException e) {
            createErrorMessage(out, MessageType.OTHER, "Error creating Mapping: Source and Destination Schema versions must be doubles");
        }
    }

    public void updateMapping(OutputStream out, String id,String description,String srcSchemaId,String srcSchemaVersion,
                              String destSchemaId,String destSchemaVersion,String isXslVersion2,
                              String xslFilename,InputStream xsdFile,String oldMtdTransId){
        try {
            Document document = DocumentHelper.createDocument();
            Element root = document.addElement("response");

            MetadataTransformation mtdTransformation = ConfigSingleton.getRepoxContextUtil().
                    getRepoxManager().getMetadataTransformationManager().loadMetadataTransformation(id);

            if(xsdFile != null && xslFilename != null){
                File xsltDir = ConfigSingleton.getRepoxContextUtil().getRepoxManager().getMetadataTransformationManager().getXsltDir();
                TransformationsFileManager.Response result = TransformationsFileManager.writeXslFile(xslFilename + ".xsl", xsltDir,xsdFile);
                if(result == TransformationsFileManager.Response.ERROR){
                    createErrorMessage(out, MessageType.OTHER, "Error creating Mapping: id \"" + id + "\" error during file saving.");
                    return;
                }
                else if(result == TransformationsFileManager.Response.FILE_TOO_BIG){
                    createErrorMessage(out, MessageType.OTHER, "Error creating Mapping: id \"" + id + "\" xsd file is too big.");
                    return;
                }
                else if(result == TransformationsFileManager.Response.XSL_ALREADY_EXISTS){
                    createErrorMessage(out, MessageType.ALREADY_EXISTS, "Error creating Mapping: id \"" + id + "\" xsd filename already exists.");
                    return;
                }else{
                    mtdTransformation.setStylesheet(xslFilename + ".xsl");
                }
            }

            if(srcSchemaVersion != null){
                String srcXsdLink = ConfigSingleton.getRepoxContextUtil().getRepoxManager()
                        .getMetadataSchemaManager().getSchemaXSD(srcSchemaId,Double.valueOf(srcSchemaVersion));
                mtdTransformation.setSourceSchema(srcXsdLink);
            }

            if(destSchemaVersion != null){
                String destXsdLink = ConfigSingleton.getRepoxContextUtil().getRepoxManager()
                        .getMetadataSchemaManager().getSchemaXSD(destSchemaId,Double.valueOf(destSchemaVersion));
                mtdTransformation.setDestSchema(destXsdLink);
            }

            if(description != null)
                mtdTransformation.setDescription(description);
            if(srcSchemaId != null)
                mtdTransformation.setSourceFormat(srcSchemaId);
            if(destSchemaId != null)
                mtdTransformation.setDestinationFormat(destSchemaId);
            if(isXslVersion2 != null)
                mtdTransformation.setVersionTwo(Boolean.valueOf(isXslVersion2));
            mtdTransformation.setMDRCompliant(true);

            ConfigSingleton.getRepoxContextUtil().getRepoxManager().getMetadataTransformationManager().
                    saveMetadataTransformation(mtdTransformation,oldMtdTransId);

            root.addElement("status").setText("OK");
            RestUtils.writeRestResponse(out, root);
        } catch (SameStylesheetTransformationException e) {
            createErrorMessage(out, MessageType.OTHER, "Error creating Mapping: id \"" + id + "\" stylesheet already exists.");
        }catch (AlreadyExistsException e) {
            createErrorMessage(out, MessageType.ALREADY_EXISTS, "Error creating Mapping: id \"" + id + "\" already exists.");
        } catch (IOException e) {
            createErrorMessage(out, MessageType.OTHER, "Error creating Mapping: IO Error");
        } catch (DocumentException e) {
            createErrorMessage(out, MessageType.OTHER, "Error creating Mapping: Document Exception");
        } catch (NumberFormatException e) {
            createErrorMessage(out, MessageType.OTHER, "Error creating Mapping: Source and Destination Schema versions must be doubles");
        }
    }

    public void removeMapping(OutputStream out, String transformationID){
        Document document = DocumentHelper.createDocument();
        Element root = document.addElement("response");
        try {
            boolean result = ConfigSingleton.getRepoxContextUtil().getRepoxManager().getMetadataTransformationManager().
                    deleteMetadataTransformation(transformationID);
            if(result)
                root.addElement("status").setText("OK");
            else
                root.addElement("status").setText("ID_NOT_FOUND");
            RestUtils.writeRestResponse(out, root);
        } catch (IOException e) {
            createErrorMessage(out, MessageType.OTHER, "Error removing Mapping: IO Error");
        } catch (DocumentException e) {
            createErrorMessage(out, MessageType.OTHER, "Error removing Mapping: Document Exception");
        }
    }

    private void saveNewMetadataSchema(String metadataFormat,String schema, String namespace,OutputStream out){
        boolean exists = ConfigSingleton.getRepoxContextUtil().getRepoxManager().getMetadataSchemaManager().
                schemaExists(metadataFormat);

        if(!exists){
            List<MetadataSchemaVersion> metadataSchemaVersions = new ArrayList<MetadataSchemaVersion>();
            metadataSchemaVersions.add(new MetadataSchemaVersion(1.0,schema));
            ConfigSingleton.getRepoxContextUtil().getRepoxManager().getMetadataSchemaManager().
                    saveMetadataSchema(null,metadataFormat,null,namespace,null,null,metadataSchemaVersions,true);
        }
    }

    public void listMappings(OutputStream out){
        Document document = DocumentHelper.createDocument();
        Element root = document.addElement("mappingsList");
        try {
            Map<String,List<MetadataTransformation>> transformations =
                    ConfigSingleton.getRepoxContextUtil().getRepoxManager().getMetadataTransformationManager().getMetadataTransformations();
            Iterator iterator=transformations.entrySet().iterator();
            while(iterator.hasNext()){
                Map.Entry mapEntry=(Map.Entry)iterator.next();
                for (MetadataTransformation metadataTransformation : (List<MetadataTransformation>)mapEntry.getValue()) {
                    Element transformationElement = root.addElement("mapping");
                    transformationElement.addAttribute("id",metadataTransformation.getId());
                    transformationElement.addElement("description").setText(metadataTransformation.getDescription());
                    transformationElement.addElement("sourceFormat").setText(metadataTransformation.getSourceFormat());
                    transformationElement.addElement("destinationFormat").setText(metadataTransformation.getDestinationFormat());
                }
            }
            RestUtils.writeRestResponse(out, root);
        } catch (IOException e) {
            createErrorMessage(out, MessageType.OTHER, "Error listing Mappings: IO Error");
        }
    }

    public void createMetadataSchema(OutputStream out, String id, String oldSchemaId,String namespace,String designation,String description,
                                     String notes,Boolean oaiAvailable,List<MetadataSchemaVersion> metadataSchemaVersions){
        try {
            Document document = DocumentHelper.createDocument();
            Element root = document.addElement("response");

            MessageType messageType = ConfigSingleton.getRepoxContextUtil().getRepoxManager().getMetadataSchemaManager().
                    saveMetadataSchema(designation,id,description,namespace,
                            notes,oldSchemaId,metadataSchemaVersions, oaiAvailable);

            if(messageType == MessageType.ALREADY_EXISTS){
                createErrorMessage(out, MessageType.OTHER, "Error creating Schema: id \"" + id + "\" schema already exists.");
                return;
            }

            root.addElement("status").setText("OK");
            RestUtils.writeRestResponse(out, root);
        } catch (IOException e) {
            createErrorMessage(out, MessageType.OTHER, "Error creating Schema: IO Error");
        } catch (NumberFormatException e) {
            createErrorMessage(out, MessageType.OTHER, "Error creating Schema: Schema versions must be doubles");
        }
    }

    public void updateMetadataSchema(OutputStream out, String id, String oldSchemaId,String namespace,String designation,String description,
                                     String notes,Boolean oaiAvailable,List<MetadataSchemaVersion> metadataSchemaVersions){
        try {
            Document document = DocumentHelper.createDocument();
            Element root = document.addElement("response");

            MessageType messageType = ConfigSingleton.getRepoxContextUtil().getRepoxManager().getMetadataSchemaManager().
                    updateMetadataSchema(designation,id,description,namespace,
                            notes,oldSchemaId,metadataSchemaVersions, oaiAvailable);

            if(messageType == MessageType.ALREADY_EXISTS){
                createErrorMessage(out, MessageType.OTHER, "Error creating Schema: id \"" + id + "\" schema already exists.");
                return;
            }

            root.addElement("status").setText("OK");
            RestUtils.writeRestResponse(out, root);
        } catch (IOException e) {
            createErrorMessage(out, MessageType.OTHER, "Error creating Schema: IO Error");
        } catch (NumberFormatException e) {
            createErrorMessage(out, MessageType.OTHER, "Error creating Schema: Schema versions must be doubles");
        }
    }

    public void listMetadataSchemas(OutputStream out){
        Document document = DocumentHelper.createDocument();
        Element root = document.addElement("schemasList");
        try {
            List<MetadataSchema> metadataSchemas=ConfigSingleton.getRepoxContextUtil().getRepoxManager().
                    getMetadataSchemaManager().getMetadataSchemas();

            for (MetadataSchema metadataSchema : metadataSchemas) {
                Element schemaElement = root.addElement("schema");
                schemaElement.addAttribute("shortDesignation", metadataSchema.getShortDesignation());
                schemaElement.addElement("namespace").setText(metadataSchema.getNamespace());
                if(metadataSchema.getDesignation() != null)
                    schemaElement.addElement("designation").setText(metadataSchema.getDesignation());
                if(metadataSchema.getDescription() != null)
                    schemaElement.addElement("description").setText(metadataSchema.getDescription());
                if(metadataSchema.getNotes() != null)
                    schemaElement.addElement("notes").setText(metadataSchema.getNotes());
                schemaElement.addElement("oaiAvailable").setText(String.valueOf(metadataSchema.isOAIAvailable()).toUpperCase());

                Element versionsElement = schemaElement.addElement("versions");
                for(MetadataSchemaVersion metadataSchemaVersion : metadataSchema.getMetadataSchemaVersions()){
                    Element versionElement = versionsElement.addElement("version");
                    versionElement.addAttribute("version",String.valueOf(metadataSchemaVersion.getVersion()));
                    versionElement.addAttribute("xsdLink",metadataSchemaVersion.getXsdLink());
                }
            }
            RestUtils.writeRestResponse(out, root);
        } catch (IOException e) {
            createErrorMessage(out, MessageType.OTHER, "Error listing Mappings: IO Error");
        }
    }

    public void removeMetadataSchema(OutputStream out, String schemaID){
        Document document = DocumentHelper.createDocument();
        Element root = document.addElement("response");
        try {
            boolean result = ConfigSingleton.getRepoxContextUtil().getRepoxManager().getMetadataSchemaManager().
                    deleteMetadataSchema(schemaID);
            if(result)
                root.addElement("status").setText("OK");
            else
                root.addElement("status").setText("ID_NOT_FOUND");
            RestUtils.writeRestResponse(out, root);
        } catch (IOException e) {
            createErrorMessage(out, MessageType.OTHER, "Error removing schema: IO Error");
        }
    }

    public void getStatistics(OutputStream out, String type){
        try {
            StatisticsManager manager =  ConfigSingleton.getRepoxContextUtil().getRepoxManager().getStatisticsManager();
            RepoxStatistics statistics = manager.generateStatistics(null);

            Document statisticsReport = manager.getStatisticsReport(statistics);
            statisticsReport.getRootElement().addAttribute("type",type);
            RestUtils.writeRestResponse(out, statisticsReport.getRootElement());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (DocumentException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
