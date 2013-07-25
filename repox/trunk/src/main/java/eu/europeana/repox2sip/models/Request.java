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

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * This class contains the Request's properties and is used to persist/retrieve into/from the database system.
 *
 * @author Nicola Aloia   <nicola.aloia@isti.cnr.it>
 *         Date: 23-mar-2010
 *         Time: 18.17.22
 */
@Entity
public class Request implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column
    private Long id = -1L;

    @ManyToOne(targetEntity = DataSet.class)
    @JoinColumn(name = "dataset_id", nullable = false)
    // @Index(name = "dataSetId_index")
    private DataSet dataSet;


    @Column(nullable = false, name = "request_name")
    private String name;

    @Column
    @Temporal(TemporalType.DATE)
    private Date creationDate;

    @Column
    private RequestStatus status;

    @Column  
    @ManyToMany (cascade = CascadeType.ALL)
    private List<MetadataRecord> metadataRecords;


    public Request() {
        metadataRecords = new ArrayList<MetadataRecord>();
    }

    /**
     * Get the Identifier of the Request.  The Identifier value is generated by the server side application. If
     * an error occurs on the server side (e.g. "Request Already Exists"), the default value (-1L) is returned.
     *
     * @return - Long - the Request identifier
     */
    public Long getId() {
        return id;
    }

    /**
     * Set the Identifier of the Request. This method is used by the Server side application. A value set by the
     * client side application is ignored.
     *
     * @param id - Long
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Get the DataSet identifier to which this request belongs.
     *
     * @return DataSet
     * @see DataSet
     */
    public DataSet getDataSet() {
        return dataSet;
    }

    /**
     * Set the DataSet identifier to which this request belongs.
     *
     * @param dataSet DataSet
     * @see DataSet
     */
    public void setDataSet(DataSet dataSet) {
        this.dataSet = dataSet;
    }

    /**
     * Get the original name of the file supplied by the Provider. The file
     * contains the original request, for traceability.
     *
     * @return String
     */
    public String getName() {
        return name;
    }

    /**
     * Set the original name of the file supplied by the Provider. The file
     * contains the original request, for traceability.
     *
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the Date of the Request creation.
     *
     * @return Date
     */
    public Date getCreationDate() {
        return creationDate;
    }

    /**
     * Set the Date of the Request creation.
     *
     * @param creationDate Date
     */
    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    /**
     * Get the status of the Request. The {@link RequestStatus} is used to manage to whole Request process.
     *
     * @return RequestStatus
     * @see RequestStatus
     */
    public RequestStatus getStatus() {
        return status;
    }

    /**
     * Set the status of the Request. The {@link  RequestStatus} is used to manage to whole Request process.
     *
     * @param status
     * @see RequestStatus
     */
    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    /**
     * Get the metadata records of this Request.
     *
     * @return List<MetadataRecord>
     * @see MetadataRecord
     */
    public List<MetadataRecord> getMetadataRecords() {
        return metadataRecords;
    }

    /**
     * Set the metadata records of this Request.
     *
     * @param metadataRecords List<MetadataRecord>
     * @see MetadataRecord
     */
    public void setMetadataRecords(List<MetadataRecord> metadataRecords) {
        this.metadataRecords = metadataRecords;
    }

    /**
     * Add a MetadataRecord to the request.
     *
     * @param record MetadataRecord
     */
    public void addMetadataRecord(MetadataRecord record) {
        if (!metadataRecords.contains(record)) {
            metadataRecords.add(record);
        }
        if (!record.getRequests().contains(this)) {
            record.getRequests().add(this);
        }
    }

    @Override
    public String toString() {
        return "Request{" +
                "id=" + id +
                //     ", dataSet=" + dataSet +
                ", name='" + name + '\'' +
                ", creationDate=" + creationDate +
                ", status=" + status +
                ", metadataRecords=" + metadataRecords +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Request)) {
            return false;
        }

        Request request = (Request) o;

        if (creationDate != null ? !creationDate.equals(request.creationDate) : request.creationDate != null) {
            return false;
        }
        if (dataSet != null ? !dataSet.equals(request.dataSet) : request.dataSet != null) {
            return false;
        }
        if (id != null ? !id.equals(request.id) : request.id != null) {
            return false;
        }
        if (metadataRecords != null ? !metadataRecords.equals(request.metadataRecords) : request.metadataRecords != null) {
            return false;
        }
        if (name != null ? !name.equals(request.name) : request.name != null) {
            return false;
        }
        if (status != request.status) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        //   result = 31 * result + (dataSet != null ? dataSet.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (creationDate != null ? creationDate.hashCode() : 0);
        result = 31 * result + (status != null ? status.hashCode() : 0);
        result = 31 * result + (metadataRecords != null ? metadataRecords.hashCode() : 0);
        return result;
    }

    /**
     * Compare the given Request ignoring the generated Identifier. Needed to avoid duplicated object
     *
     * @param o Object
     * @return boolean
     */
    public boolean equalsIgnoreId(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Request)) {
            return false;
        }

        Request request = (Request) o;

        if (creationDate != null ? !creationDate.equals(request.creationDate) : request.creationDate != null) {
            return false;
        }
        if (dataSet != null ? !dataSet.equals(request.dataSet) : request.dataSet != null) {
            return false;
        }
        if (metadataRecords != null ? !metadataRecords.equals(request.metadataRecords) : request.metadataRecords != null) {
            return false;
        }
        if (name != null ? !name.equals(request.name) : request.name != null) {
            return false;
        }
        if (status != request.status) {
            return false;
        }

        return true;
    }

}