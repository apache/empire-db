package org.apache.empire.struts2.websample.web.actions;

import org.apache.empire.db.DBRecord;
import org.apache.empire.db.DBTable;
import org.apache.empire.struts2.actionsupport.RecordActionSupport;
import org.apache.empire.struts2.actionsupport.SessionPersistence;
import org.apache.empire.struts2.websample.db.records.EmployeeRecord;
import org.apache.empire.struts2.websample.web.actiontypes.DetailAction;
import org.apache.struts2.interceptor.NoParameters;


@SuppressWarnings("serial")
/**
 * EmployeeDetailAction
 * <p>
 * This class provides form processing functions for an Employee record.<br>
 * The class uses a RecordActionSupport object which does most of the work.<br>
 * For multi-record forms it is possible to have several RecordActionSupport members.<br>
 * In this case each must be given a differnt property name however (see RecordActionSupport overloads).
 * </p>
 */
public class EmployeeDetailAction extends DetailAction
    implements NoParameters // set this to provide custom parameter handling
{
    /**
     * Action mappings
     */

    protected RecordActionSupport recordSupport = null;

    // ------- Action Construction -------
    
    public EmployeeDetailAction() {
        // Init Record Support Object
        DBTable table = getDatabase().T_EMPLOYEES;
        DBRecord record = new EmployeeRecord(this);
        // create a support Object
        recordSupport = new RecordActionSupport(this, table, record, SessionPersistence.Key);
    }

    // ------- Action Properties -------
    
    public EmployeeRecord getEmployee()
    {
        return (EmployeeRecord) recordSupport.getRecord();
    }

    // ------- Action Methods -------

    @Override
    public String doCreate() {
        // Create Record
        if (!recordSupport.createRecord()) {
            setActionError(recordSupport);
            return RETURN;
        }
        // Done
        return INPUT;
    }

    @Override
    public String doLoad() {
        // Load Record
        if (!recordSupport.loadRecord()) {
            setActionError(recordSupport);
            return RETURN;
        }
        // Set Edit Mode
        return INPUT;
    }

    @Override
    public String doSave() {
        // Load Form Data into record
        if (!recordSupport.loadFormData()) {
            if (recordSupport.hasError())
                setActionError(recordSupport);
            return INPUT;
        }
        // Now save the record
        if (!recordSupport.saveChanges()) {
            setActionError(recordSupport);
            return INPUT;
        }
        // Erfolg
        return RETURN;
    }

    @Override
    public String doDelete() {
        // Delete Record
        if (!recordSupport.deleteRecord()) {
            setActionError(recordSupport);
            return INPUT;
        }
        // Erfolg
        return RETURN;
    }

}
