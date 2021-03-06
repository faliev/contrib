/*
 * Copyright 2007 EDL FOUNDATION
 *
 * Licensed under the EUPL, Version 1.1 or - as soon they
 * will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence");
 * you may not use this work except in compliance with the
 * Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl
 *
 * Unless required by applicable law or agreed to in
 * writing, software distributed under the Licence is
 * distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.europeana.repox2sip.models;

import eu.europeana.definitions.domain.Language;
import org.hibernate.annotations.Index;

import javax.persistence.*;
import java.io.Serializable;
import java.net.URL;
import java.util.List;

/**
 * This class contains the DataSet's properties and is used to persist/retrieve into/from the database system.
 * A DataSet is a Collection of digital objects.
 *
 * @author Nicola Aloia   <nicola.aloia@isti.cnr.it>
 *         Date: 23-mar-2010
 *         Time: 18.17.22
 */
@Entity
public class DataSet implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column
    private Long id = -1L;

    @ManyToOne(targetEntity = eu.europeana.repox2sip.models.Provider.class)
    @JoinColumn(name = "provider_id", nullable = false)
    @Index(name = "providerId_index")
    private Provider provider;

    @Column(nullable = false, name = "dataset_name")
    private String name;

    @Column(nullable = false, name = "dataset_language")
    private Language language;

    @Column(nullable = false)
    private String qName;

    @Column(nullable = false, name = "id_q_name")
    private String idQName;

    @Column(nullable = false, unique = true)
    private String nameCode;

    @Column(nullable = false, name = "dataset_type")
    private DataSetType type;

    @Column
    private URL homePage;


    @Column
    private ImporterStrategy strategy;

    @Column
    @Lob
    private String description;

    @Column
    private String oaiSet;


    @OneToMany(targetEntity = Request.class, mappedBy = "dataSet", cascade = CascadeType.ALL)
    private List<Request> requests;

    /**
     * Get the Identifier of the DataSet.  The Identifier value is generated by the server side application. If
     * an error occurs on the server side (e.g. "DataSet Already Exists"), the default value (-1L) is returned.
     *
     * @return - Long - the DataSet identifier
     */
    public Long getId() {
        return id;
    }

    /**
     * Set the Identifier of the DataSet. This method is used by the Server side application. A value set by the
     * client side application is ignored.
     *
     * @param id - Long
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Get the Provider Identifier.
     *
     * @return Provider - the Provider of the DataSet
     * @see {@link Provider}
     */
    public Provider getProvider() {
        return provider;
    }

    /**
     * Set the Provider of the DataSet.
     *
     * @param provider Provider - the Provider Identifier
     * @see {@link Provider}
     */
    public void setProvider(Provider provider) {
        this.provider = provider;
    }

    /**
     * Get the DataSet name (i.e. the name of the collection of digital objects).
     *
     * @return - String
     */
    public String getName() {
        return name;
    }

    /**
     * Set the DataSet name (i.e. the name of the collection of digital objects).
     *
     * @param name - String
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the language code of the DataSet. The Europeana {@link Language) enumeration is used.
     *
     * @return Language - the language code.
     * @see {@link Language}
     */
    public Language getLanguage() {
        return language;
    }

    /**
     * Set the language code of the DataSet. The Europeana {@link Language) enumeration must be used.
     *
     * @param language Language - the language code.
     * @see {@link Language}
     */
    public void setLanguage(Language language) {
        this.language = language;
    }

    /**
     * Get the DataSet QName. The value of the �QName� is the qualified name used by the provider as the
     * identifier of the metadata records.
     *
     * @return - String
     */
    public String getQName() {
        return qName;
    }

    /**
     * Set the DataSet QName. The value of the �QName� is the qualified name used by the provider as the
     * identifier of the metadata records.
     *
     * @param qName - String
     */
    public void setQName(String qName) {
        this.qName = qName;
    }

    /**
     * Get the code associated to the DataSet. The NameCode is used by the Europeana server side application
     * to generate a unique file identifier for harversted metadata.
     *
     * @return - String
     */
    public String getNameCode() {
        return nameCode;
    }

    /**
     * Set the code associated to the DataSet. The NameCode is used by the Europeana server side application
     * to generate a unique file identifier for harversted metadata.
     *
     * @param nameCode - String
     */
    public void setnameCode(String nameCode) {
        this.nameCode = nameCode;
    }

    /**
     * Get the type of the DataSet, as defined in the Europeana (@link DataSetType) enum.
     *
     * @return - DataSetType enum.
     * @see {@link DataSetType}
     */
    public DataSetType getType() {
        return type;
    }

    /**
     * Set the type of the DataSet, as defined in the Europeana (@link DataSetType) enum.
     *
     * @param type - DataSetType enum.
     * @see {@link DataSetType}
     */
    public void setType(DataSetType type) {
        this.type = type;
    }

    /**
     * Get the List of Request belonging to this DataSet instance.
     *
     * @return List<Request>
     * @see {@link Request}
     */
    public List<Request> getRequests() {
        return requests;
    }

    /**
     * Get the URL of the DataSet's Home Page
     *
     * @return URL
     */
    public URL getHomePage() {
        return homePage;
    }

    /**
     * Set the URL of the DataSet's Home Page
     *
     * @param homePage URL
     */
    public void setHomePage(URL homePage) {
        this.homePage = homePage;
    }

    /**
     * Get the List of Request belonging to this DataSet instance.
     *
     * @param requests List<Request>
     * @see {@link Request}
     */
    public void setRequests(List<Request> requests) {
        this.requests = requests;
    }

    /**
     * Get the ingestion strategy adopted by the harvester for a specific data set.
     *
     * @return ImporterStrategy
     */
    public ImporterStrategy getStrategy() {
        return strategy;
    }

    /**
     * Set the ingestion strategy adopted by the harvester for a specific data set.
     * Possible values:
     * <ol>
     * <li>DataSourceDirectoryImporter </li>
     * <li>DataSourceOai </li>
     * <li>DataSourceZ3950  </li>
     * </ol>
     */
    public void setStrategy(ImporterStrategy strategy) {
        this.strategy = strategy;
    }

    /**
     * Returns a description of the Data Set
     *
     * @return String
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set a textual description of the Data Set
     *
     * @param description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Get OAI settings
     *
     * @return String
     */
    public String getOaiSet() {
        return oaiSet;
    }

    /**
     * Set OAI properties
     */
    public void setOaiSet(String oaiSet) {
        this.oaiSet = oaiSet;
    }

    /**
     * Get the xpath expression identifying the record id.
     *
     * @return
     */
    public String getIdQName() {
        return idQName;
    }

    /**
     * Set the xpath expression identifying the record id.
     */
    public void setIdQName(String idQName) {
        this.idQName = idQName;
    }

    /**
     * Compare the given DataSet ignoring the generated Identifier. Needed to avoid duplicated object
     *
     * @param o
     * @return boolean
     */
    public boolean equalsIgnoreId(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DataSet)) {
            return false;
        }

        DataSet dataSet = (DataSet) o;

        if (description != null ? !description.equals(dataSet.description) : dataSet.description != null) {
            return false;
        }
        if (homePage != null ? !homePage.equals(dataSet.homePage) : dataSet.homePage != null) {
            return false;
        }
        if (idQName != null ? !idQName.equals(dataSet.idQName) : dataSet.idQName != null) {
            return false;
        }
        if (language != dataSet.language) {
            return false;
        }
        if (name != null ? !name.equals(dataSet.name) : dataSet.name != null) {
            return false;
        }
        if (nameCode != null ? !nameCode.equals(dataSet.nameCode) : dataSet.nameCode != null) {
            return false;
        }
        if (oaiSet != null ? !oaiSet.equals(dataSet.oaiSet) : dataSet.oaiSet != null) {
            return false;
        }
        if (provider != null ? !provider.equals(dataSet.provider) : dataSet.provider != null) {
            return false;
        }
        if (qName != null ? !qName.equals(dataSet.qName) : dataSet.qName != null) {
            return false;
        }
        if (requests != null ? !requests.equals(dataSet.requests) : dataSet.requests != null) {
            return false;
        }
        if (strategy != dataSet.strategy) {
            return false;
        }
        if (type != dataSet.type) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return "DataSet{" +
                "id=" + id +
                ", provider=" + provider +
                ", name='" + name + '\'' +
                ", language=" + language +
                ", qName='" + qName + '\'' +
                ", idQName='" + idQName + '\'' +
                ", nameCode='" + nameCode + '\'' +
                ", type=" + type +
                ", homePage=" + homePage +
                ", strategy=" + strategy +
                ", description='" + description + '\'' +
                ", oaiSet='" + oaiSet + '\'' +
                ", requests=" + requests +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DataSet)) {
            return false;
        }

        DataSet dataSet = (DataSet) o;

        if (description != null ? !description.equals(dataSet.description) : dataSet.description != null) {
            return false;
        }
        if (homePage != null ? !homePage.equals(dataSet.homePage) : dataSet.homePage != null) {
            return false;
        }
        if (id != null ? !id.equals(dataSet.id) : dataSet.id != null) {
            return false;
        }
        if (idQName != null ? !idQName.equals(dataSet.idQName) : dataSet.idQName != null) {
            return false;
        }
        if (language != dataSet.language) {
            return false;
        }
        if (name != null ? !name.equals(dataSet.name) : dataSet.name != null) {
            return false;
        }
        if (nameCode != null ? !nameCode.equals(dataSet.nameCode) : dataSet.nameCode != null) {
            return false;
        }
        if (oaiSet != null ? !oaiSet.equals(dataSet.oaiSet) : dataSet.oaiSet != null) {
            return false;
        }
        if (provider != null ? !provider.equals(dataSet.provider) : dataSet.provider != null) {
            return false;
        }
        if (qName != null ? !qName.equals(dataSet.qName) : dataSet.qName != null) {
            return false;
        }
        if (requests != null ? !requests.equals(dataSet.requests) : dataSet.requests != null) {
            return false;
        }
        if (strategy != dataSet.strategy) {
            return false;
        }
        if (type != dataSet.type) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (provider != null ? provider.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (language != null ? language.hashCode() : 0);
        result = 31 * result + (qName != null ? qName.hashCode() : 0);
        result = 31 * result + (idQName != null ? idQName.hashCode() : 0);
        result = 31 * result + (nameCode != null ? nameCode.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (homePage != null ? homePage.hashCode() : 0);
        result = 31 * result + (strategy != null ? strategy.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (oaiSet != null ? oaiSet.hashCode() : 0);
        result = 31 * result + (requests != null ? requests.hashCode() : 0);
        return result;
    }
}
