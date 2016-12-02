package org.apache.empire.db.codegen;

import java.sql.Types;

import org.apache.empire.data.DataType;

public class CodeGenParserMySQL extends CodeGenParser {

	// private static final Logger log = LoggerFactory.getLogger(CodeGenParserMySQL.class);
	
	public CodeGenParserMySQL(CodeGenConfig config) {
		super(config);
	}

	@Override
	protected double getColumnSize(DataType empireType, int dataType, int columnSize) {
		
		switch (empireType) {
			
			case INTEGER: {
				// return size in byte, depending on MySQL Integer Types
				// see http://dev.mysql.com/doc/refman/5.7/en/integer-types.html
				// ignore the "real" columnsize as its just a "format hint"
				switch(dataType) {
					case Types.TINYINT:
						return 1; // TINYINT, 1 byte
					case Types.SMALLINT:
						return 2; // SMALLINT, 2 byte
					case Types.BIGINT:
						return 8; // BIGINT, 8 byte
					default: 
						return 4; // Types.INTEGER, INT, 4 byte
				}
			}
			
			default:
				return super.getColumnSize(empireType, dataType, columnSize);
			
		}
		
	}
	
}
