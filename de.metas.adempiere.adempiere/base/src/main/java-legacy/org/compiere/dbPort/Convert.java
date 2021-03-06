/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2006 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
package org.compiere.dbPort;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.adempiere.ad.dao.IQueryStatisticsLogger;
import org.adempiere.ad.migration.logger.IMigrationLogger;
import org.adempiere.service.ISysConfigBL;
import org.adempiere.util.Services;
import org.compiere.model.I_AD_MigrationScript;
import org.compiere.model.MSequence;
import org.slf4j.Logger;
import de.metas.logging.LogManager;
import org.compiere.util.DisplayType;
import org.compiere.util.Ini;

/**
 *  Convert SQL to Target DB
 *
 *  @author     Jorg Janke, Victor Perez
 *  
 *  @author Teo Sarca, www.arhipac.ro
 *  		<li>BF [ 2782095 ] Do not log *Access records
 *  			https://sourceforge.net/tracker/?func=detail&aid=2782095&group_id=176962&atid=879332
 *  		<li>TODO: BF [ 2782611 ] Migration scripts are not UTF8
 *  			https://sourceforge.net/tracker/?func=detail&aid=2782611&group_id=176962&atid=879332
 *  @author Teo Sarca
 *  		<li>BF [ 3137355 ] PG query not valid when contains quotes and backslashes
 *  			https://sourceforge.net/tracker/?func=detail&aid=3137355&group_id=176962&atid=879332	
 */
public abstract class Convert
{
	/**
	 * Query Statistics logger
	 * 
	 * NOTE: to minimize logging overhead, we are retrieving our service here, because we don't expect this service to change
	 */
	private static final IQueryStatisticsLogger QUERY_STATISTICS_LOGGER = Services.get(IQueryStatisticsLogger.class);

	/** RegEx: insensitive and dot to include line end characters   */
	public static final int         REGEX_FLAGS = Pattern.CASE_INSENSITIVE | Pattern.DOTALL;

	/** Statement used                  */
	protected Statement               m_stmt = null;

	/** Last Conversion Error           */
	protected String                  m_conversionError = null;
	/** Last Execution Error            */
	private Exception               m_exception = null;
	/** Verbose Messages                */
	private boolean                 m_verbose = true;

	/**	Logger	*/
	private static Logger	log	= LogManager.getLogger(Convert.class);
	
    private static FileOutputStream tempFilePg = null;
    private static Writer writerPg;

    /**
	 *  Set Verbose
	 *  @param verbose
	 */
	public final void setVerbose (boolean verbose)
	{
		m_verbose = verbose;
	}   //  setVerbose

	/**************************************************************************
	 *  Execute SQL Statement (stops at first error).
	 *  If an error occurred hadError() returns true.
	 *  You can get details via getConversionError() or getException()
	 *  @param sqlStatements
	 *  @param conn connection
	 *  @return true if success
	 *  @throws IllegalStateException if no connection
	 */
	public final boolean execute (String sqlStatements, Connection conn)
	{
		if (conn == null)
			throw new IllegalStateException ("Require connection");
		//
		final List<String> sqls = convert (sqlStatements);
		m_exception = null;
		if (m_conversionError != null || sqls == null || sqls.isEmpty())
			return false;

		boolean ok = true;
		int i = 0;
		String statement = null;
		try
		{
			if (m_stmt == null)
				m_stmt = conn.createStatement();
			//
			for (i = 0; ok && i < sqls.size(); i++)
			{
				statement = sqls.get(i);
				if (statement.length() == 0)
				{
					if (m_verbose)
						log.trace("Skipping empty (" + i + ")");
				}
				else
				{
					if (m_verbose)
						log.info("Executing (" + i + ") <<" + statement + ">>");
					else
						log.info("Executing " + i);
					try
					{
						m_stmt.clearWarnings();
						int no = m_stmt.executeUpdate(statement);
						SQLWarning warn = m_stmt.getWarnings();
						if (warn != null)
						{
							if (m_verbose)
								log.info("- " + warn);
							else
							{
								log.info("Executing (" + i + ") <<" + statement + ">>");
								log.info("- " + warn);
							}
						}
						if (m_verbose)
							log.debug("- ok " + no);
					}
					catch (SQLException ex)
					{
						//  Ignore Drop Errors
						if (!statement.startsWith("DROP "))
						{
							ok = false;
							m_exception = ex;
						}
						if (!m_verbose)
							log.info("Executing (" + i + ") <<" + statement + ">>");
						log.info("Error executing " + i + "/" + sqls.size() + " = " + ex);
					}
				}
			}   //  for all statements
		}
		catch (SQLException e)
		{
			m_exception = e;
			if (!m_verbose)
				log.info("Executing (" + i + ") <<" + statement + ">>");
			log.info("Error executing " + i + "/" + sqls.size() + " = " + e);
			return false;
		}
		return ok;
	}   //  execute

	/**
	 *  Return last execution exception
	 *  @return execution exception
	 */
	public final Exception getException()
	{
		return m_exception;
	}   //  getException

	/**
	 *  Returns true if a conversion or execution error had occurred.
	 *  Get more details via getConversionError() or getException()
	 *  @return true if error had occurred
	 */
	public final boolean hasError()
	{
		return (m_exception != null) | (m_conversionError != null);
	}   //  hasError

	/**
	 *  Convert SQL Statement (stops at first error).
	 *  Statements are delimited by /
	 *  If an error occurred hadError() returns true.
	 *  You can get details via getConversionError()
	 *  @param sqlStatements
	 *  @return converted statement as a string
	 */
	public final String convertAll (String sqlStatements)
	{
		final List<String> sqls = convert (sqlStatements);
		final StringBuilder sb = new StringBuilder(sqlStatements.length() + 10);
		for (final String sql : sqls)
		{
			//  line.separator
			sb.append(sql).append("\n/\n");
			if (m_verbose)
				log.info("Statement: " + sql);
		}
		return sb.toString();
	}   //  convertAll

	/**
	 *  Convert SQL Statement (stops at first error).
	 *  If an error occurred hadError() returns true.
	 *  You can get details via getConversionError()
	 *  @param sqlStatements
	 *  @return Array of converted Statements
	 */
	public final List<String> convert (String sqlStatements)
	{
		m_conversionError = null;
		if (sqlStatements == null || sqlStatements.isEmpty())
		{
			m_conversionError = "SQL_Statement is null or has zero length";
			log.info(m_conversionError);
			return null;
		}
		//
		return convertIt (sqlStatements);
	}   //  convert

	/**
	 *  Return last conversion error or null.
	 *  @return lst conversion error
	 */
	public final String getConversionError()
	{
		return m_conversionError;
	}   //  getConversionError

	
	/**************************************************************************
	 *  Conversion routine (stops at first error).
	 *  <pre>
	 *  - convertStatement
	 *      - convertWithConvertMap
	 *      - convertComplexStatement
	 *      - decode, sequence, exception
	 *  </pre>
	 *  @param sqlStatements
	 *  @return array of converted statements
	 */
	protected final List<String> convertIt (String sqlStatements)
	{
		final List<String> result = convertStatement(sqlStatements);     //  may return more than one target statement
		if (result == null)
		{
			return Collections.emptyList();
		}
		return result;
	}   //  convertIt

	/**
	 * Clean up Statement. Remove trailing spaces, carriage return and tab 
	 * 
	 * @param statement
	 * @return sql statement
	 */
	protected final String cleanUpStatement(String statement)
	{
		String clean = statement.trim();

		// Convert cr/lf/tab to single space
		Matcher m = Pattern.compile("\\s+").matcher(clean);
		clean = m.replaceAll(" ");

		clean = clean.trim();
		return clean;
	} // removeComments
	
	/**
	 * Utility method to replace quoted string with a predefined marker
	 * @param retValue
	 * @param retVars
	 * @return string
	 */
	protected final String replaceQuotedStrings(String inputValue, final Collection<String> retVars)
	{
		// save every value  
		// Carlos Ruiz - globalqss - better matching regexp
		retVars.clear();
		
		// First we need to replace double quotes to not be matched by regexp - Teo Sarca BF [3137355 ]
		final String quoteMarker = "<--QUOTE"+System.currentTimeMillis()+"-->";
		inputValue = inputValue.replace("''", quoteMarker);
		
		Pattern p = Pattern.compile("'[[^']*]*'");
		Matcher m = p.matcher(inputValue);
		int i = 0;
		StringBuffer retValue = new StringBuffer(inputValue.length());
		while (m.find()) {
			String var = inputValue.substring(m.start(), m.end()).replace(quoteMarker, "''"); // Put back quotes, if any
			retVars.add(var);
			m.appendReplacement(retValue, "<--" + i + "-->");
			i++;
		}
		m.appendTail(retValue);
		return retValue.toString()
			.replace(quoteMarker, "''") // Put back quotes, if any
		;
	}

	/**
	 * Utility method to recover quoted string store in retVars
	 * @param retValue
	 * @param retVars
	 * @return string
	 */
	protected final String recoverQuotedStrings(String retValue, Collection<String> retVars)
	{
		int i = 0;
		for (String replacement : retVars)
		{
			//hengsin, special character in replacement can cause exception
			replacement = escapeQuotedString(replacement);
			retValue = retValue.replace("<--" + i + "-->", replacement);
			i++;
		}
		return retValue;
	}
	
	/**
	 * hook for database specific escape of quoted string ( if needed )
	 * @param in
	 * @return string
	 */
	protected String escapeQuotedString(String in)
	{
		return in;
	}
	
	/**
	 * Convert simple SQL Statement. Based on ConvertMap
	 * 
	 * @param sqlStatement
	 * @return converted Statement
	 */
	private final String applyConvertMap(String sqlStatement) {
		// Error Checks
		if (sqlStatement.toUpperCase().indexOf("EXCEPTION WHEN") != -1) {
			String error = "Exception clause needs to be converted: "
					+ sqlStatement;
			log.info(error);
			m_conversionError = error;
			return sqlStatement;
		}

		// Carlos Ruiz - globalqss
		// Standard Statement -- change the keys in ConvertMap
		
		String retValue = sqlStatement;

		// for each iteration in the conversion map
		final ConvertMap convertMap = getConvertMap();
		if (convertMap != null)
		{
//			final Iterator<Pattern> iter = convertMap.keySet().iterator();
			for (final Map.Entry<Pattern, String> pattern2replacement : convertMap.getPattern2ReplacementEntries())
//			while (iter.hasNext())
			{
			    // replace the key on convertmap (i.e.: number by numeric)   
				final Pattern regex = pattern2replacement.getKey();
				final String replacement = pattern2replacement.getValue();
				try
				{
					final Matcher matcher = regex.matcher(retValue);
					retValue = matcher.replaceAll(replacement);
	
				}
				catch (Exception e)
				{
					String error = "Error expression: " + regex + " - " + e;
					log.info(error);
					m_conversionError = error;
				}
			}
		}
		
		return retValue;
	} // convertSimpleStatement
	
	/**
	 * do convert map base conversion
	 * @param sqlStatement
	 * @return string
	 */
	protected final String convertWithConvertMap(String sqlStatement) {
		try 
		{
			sqlStatement = applyConvertMap(cleanUpStatement(sqlStatement));
		}
		catch (RuntimeException e) {
			log.warn(e.getLocalizedMessage());
			m_exception = e;
		}
		
		return sqlStatement;
	}
	
	/**
	 * Get convert map for use in sql convertion
	 * @return map
	 */
	protected ConvertMap getConvertMap()
	{
		return null;
	}
	
	/**
	 *  Convert single Statements.
	 *  - remove comments
	 *      - process FUNCTION/TRIGGER/PROCEDURE
	 *      - process Statement
	 *  @param sqlStatement
	 *  @return converted statement
	 */
	protected abstract List<String> convertStatement (String sqlStatement);

	/**
	 * task it_FRESH_47 : Log only Postgresql migration scripts
	 * 
	 * @param oraStatement
	 * @param pgStatement
	 */
	public static void logMigrationScript(String oraStatement, String pgStatement)
	{
		if (QUERY_STATISTICS_LOGGER != null)
		{
			QUERY_STATISTICS_LOGGER.collect(pgStatement);
		}

		// Check AdempiereSys
		// check property Log migration script
		final boolean logMigrationScript = Ini.isPropertyBool(Ini.P_LOGMIGRATIONSCRIPT);
		if (logMigrationScript)
		{
			if (dontLog(oraStatement))
				return;
			// task it_FRESH_47 : we only need scripts for Postgresql database
			// Log postgres migration scripts in temp directory
			// migration_script_postgresql.sql

			try
			{
				if (pgStatement == null)
				{
					// if oracle call convert for postgres before logging
					Convert_PostgreSQL convert = new Convert_PostgreSQL();
					List<String> r = convert.convert(oraStatement);
					pgStatement = r.get(0);
				}
				if (tempFilePg == null)
				{
					File fileNamePg = createMigrationScriptFile("postgresql");
					tempFilePg = new FileOutputStream(fileNamePg, true);
					writerPg = new BufferedWriter(new OutputStreamWriter(tempFilePg, "UTF8"));
				}
				writeLogMigrationScript(writerPg, pgStatement);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	private static final File createMigrationScriptFile(final String dbType) throws IOException
	{
		final int scriptId = (MSequence.getNextID(0, I_AD_MigrationScript.Table_Name)* 10);
		final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmm");

		final String prefix = scriptId + "_migration_script_"
				+ dateFormat.format(new Date())
				+ "_";
		final String sufix = "_"
				+ dbType
				+ ".sql";
		final File file = File.createTempFile(prefix, sufix);
		return file;
	}

	private static boolean dontLog(String statement)
	{
		// metas: teo_sarca: end
		final String uppStmt = statement.toUpperCase().trim();
		
		//
		// Don't log selects
		if (uppStmt.startsWith("SELECT "))
		{
			return true;
		}
		
		//
		// FIXME: don't log update to statistic process hardcoded
		if (uppStmt.startsWith("UPDATE AD_PROCESS SET STATISTIC_"))
		{
			return true;
		}
		
		//
		// Don't log DELETE FROM Some_Table WHERE AD_Table_ID=? AND Record_ID=?
		if (uppStmt.startsWith("DELETE FROM ") && uppStmt.endsWith(" WHERE AD_TABLE_ID=? AND RECORD_ID=?"))
		{
			return true;
		}

		//
		// Check that INSERT/UPDATE/DELETE statements are about our ignored tables
		final Set<String> exceptionTablesUC = Services.get(IMigrationLogger.class).getTablesToIgnoreUC();
		for (final String tableNameUC : exceptionTablesUC)
		{
			if (uppStmt.startsWith("INSERT INTO " + tableNameUC + " "))
				return true;
			if (uppStmt.startsWith("DELETE FROM " + tableNameUC + " "))
				return true;
			if (uppStmt.startsWith("DELETE " + tableNameUC + " "))
				return true;
			if (uppStmt.startsWith("UPDATE " + tableNameUC + " "))
				return true;
			if (uppStmt.startsWith("INSERT INTO " + tableNameUC + "("))
				return true;
		}
		
		return false;
	}

	private static void writeLogMigrationScript(Writer w, String statement) throws IOException
	{
		final String prm_COMMENT = Services.get(ISysConfigBL.class).getValue("DICTIONARY_ID_COMMENTS");
		// log time and date
		SimpleDateFormat format = DisplayType.getDateFormat(DisplayType.DateTime);
		String dateTimeText = format.format(new Timestamp(System.currentTimeMillis()));
		w.append("-- ");
		w.append(dateTimeText);
		w.append("\n");
		// log sysconfig comment
		w.append("-- ");
		w.append(prm_COMMENT);
		w.append("\n");
		// log statement
		w.append(statement);
		// close statement
		w.append("\n;\n\n");
		// flush stream - teo_sarca BF [ 1894474 ]
		w.flush();
	}

	/**
	 * Mark given keyword as native.
	 * 
	 * Some prefixes/suffixes can be added, but this depends on implementation.
	 * 
	 * @param keyword
	 * @return keyword with some prefix/suffix markers.
	 */
	public String markNative(String keyword)
	{
		return keyword;
	}

}   //  Convert
