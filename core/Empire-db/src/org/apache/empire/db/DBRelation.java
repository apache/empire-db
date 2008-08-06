/*
 * ESTEAM Software GmbH, 15.12.2004
 */
package org.apache.empire.db;

/**
 * This class creates a DBReferene object for a foreing key relation.
 * 
 * @author ESTEAM software <A TARGET="esteam" HREF="http://www.esteam.de">www.esteam.de </A>
 */
public class DBRelation extends DBObject
{
	public static class DBReference
	{
	    private DBTableColumn sourceColumn;
        private DBTableColumn targetColumn;
	    
	    public DBReference(DBTableColumn sourceColumn, DBTableColumn targetColumn)
	    {
	       this.sourceColumn = sourceColumn;
	       this.targetColumn = targetColumn;
	    }

        public DBTableColumn getSourceColumn()
        {
            return sourceColumn;
        }

        public DBTableColumn getTargetColumn()
        {
            return targetColumn;
        }
	}

	// Members
    private DBDatabase    db;
    private String        name;
    private DBReference[] references;

    /**
     * Creates a DBReferene object for a foreing key relation.
     */
	public DBRelation(DBDatabase db, String name, DBReference[] references)
	{
	   this.db         = db;
	   this.name       = name;
	   this.references = references;
	}
	
    /**
     * Returns the name.
     * 
     * @return Returns the name
     */
    public String getName()
    {
        return name;
    }

    /**
     * Returns the full qualified table name.
     * 
     * @return the full qualified table name
     */
    public String getFullName()
    {
        String  schema = db.getSchema();
        return (schema!=null) ? schema+"."+name : name;
    }
    
    /**
     * Returns the references.
     * 
     * @return the references
     */
    public DBReference[] getReferences()
    {
        return references;
    }

    @Override
    public DBDatabase getDatabase()
    {
        return db;
    }

}