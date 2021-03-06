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
package org.compiere.process;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import org.slf4j.Logger;
import de.metas.logging.LogManager;

import org.adempiere.acct.api.IAcctSchemaDAO;
import org.adempiere.ad.table.api.IADTableDAO;
import org.adempiere.util.Services;
import org.compiere.model.I_C_AcctSchema;
import org.compiere.model.I_C_BankStatement;
import org.compiere.model.I_M_MatchInv;
import org.compiere.model.I_M_MatchPO;
import org.compiere.model.MAllocationHdr;
import org.compiere.model.MCash;
import org.compiere.model.MInOut;
import org.compiere.model.MInventory;
import org.compiere.model.MInvoice;
import org.compiere.model.MJournal;
import org.compiere.model.MMovement;
import org.compiere.model.MOrder;
import org.compiere.model.MPayment;
import org.compiere.model.MProjectIssue;
import org.compiere.model.MRequisition;
import org.compiere.model.X_C_DocType;
import org.compiere.model.X_M_Production;
import org.compiere.util.DB;
import org.compiere.util.TimeUtil;
import org.eevolution.model.I_DD_Order;
import org.eevolution.model.I_PP_Order;
import org.eevolution.model.X_HR_Process;

/**
 *	Accounting Fact Reset
 *	
 *  @author Jorg Janke
 *  @version $Id: FactAcctReset.java,v 1.5 2006/09/21 21:05:02 jjanke Exp $
 */
public class FactAcctReset extends SvrProcess
{
	/**	Client Parameter		*/
	private int		p_AD_Client_ID = 0;
	/** Table Parameter			*/
	private int		p_AD_Table_ID = 0;
	/**	Delete Parameter		*/
	private boolean	p_DeletePosting = false;
	
	private int		m_countReset = 0;
	private int		m_countDelete = 0;
	private Timestamp p_DateAcct_From = null ;
	private Timestamp p_DateAcct_To = null;
	
	/**
	 *  Prepare - e.g., get Parameters.
	 */
	@Override
	protected void prepare()
	{
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null)
				;
			else if (name.equals("AD_Client_ID"))
				p_AD_Client_ID = ((BigDecimal)para[i].getParameter()).intValue();
			else if (name.equals("AD_Table_ID"))
				p_AD_Table_ID = ((BigDecimal)para[i].getParameter()).intValue();
			else if (name.equals("DeletePosting"))
				p_DeletePosting = "Y".equals(para[i].getParameter());
			else if (name.equals("DateAcct"))
			{
				p_DateAcct_From = (Timestamp)para[i].getParameter();
				p_DateAcct_To = (Timestamp)para[i].getParameter_To();
			}
			else
				log.error("Unknown Parameter: " + name);
		}
	}	//	prepare

	/**
	 *  Perform process.
	 *  @return Message (clear text)
	 *  @throws Exception if not successful
	 */
	@Override
	protected String doIt() throws Exception
	{
		log.info("AD_Client_ID=" + p_AD_Client_ID 
			+ ", AD_Table_ID=" + p_AD_Table_ID + ", DeletePosting=" + p_DeletePosting);
		//	List of Tables with Accounting Consequences
		String sql = "SELECT AD_Table_ID, TableName "
			+ "FROM AD_Table t "
			+ "WHERE t.IsView='N'";
		if (p_AD_Table_ID > 0)
			sql += " AND t.AD_Table_ID=" + p_AD_Table_ID;
		sql += " AND EXISTS (SELECT * FROM AD_Column c "
				+ "WHERE t.AD_Table_ID=c.AD_Table_ID AND c.ColumnName='Posted' AND c.IsActive='Y')";
		PreparedStatement pstmt = null;
		try
		{
			pstmt = DB.prepareStatement(sql, get_TrxName());
			ResultSet rs = pstmt.executeQuery();
			while (rs.next())
			{
				int AD_Table_ID = rs.getInt(1);
				String TableName = rs.getString(2);
				if (p_DeletePosting)
					delete (TableName, AD_Table_ID);
				else
					reset (TableName);
			}
			rs.close();
			pstmt.close();
			pstmt = null;
		}
		catch (Exception e)
		{
			log.error(sql, e);
		}
		try
		{
			if (pstmt != null)
				pstmt.close();
			pstmt = null;
		}
		catch (Exception e)
		{
			pstmt = null;
		}
		//
		return "@Updated@ = " + m_countReset + ", @Deleted@ = " + m_countDelete;
	}	//	doIt

	/**
	 * 	Reset Accounting Table and update count
	 *	@param TableName table
	 */
	private void reset (String TableName)
	{
		String sql = "UPDATE " + TableName
			+ " SET Processing='N' WHERE AD_Client_ID=" + p_AD_Client_ID
			+ " AND (Processing<>'N' OR Processing IS NULL)";
		int unlocked = DB.executeUpdate(sql, get_TrxName());
		//
		sql = "UPDATE " + TableName
			+ " SET Posted='N' WHERE AD_Client_ID=" + p_AD_Client_ID
			+ " AND (Posted NOT IN ('Y','N') OR Posted IS NULL) AND Processed='Y'";
		int invalid = DB.executeUpdate(sql, get_TrxName());
		//
		if (unlocked + invalid != 0)
			log.debug(TableName + " - Unlocked=" + unlocked + " - Invalid=" + invalid);
		m_countReset += unlocked + invalid; 
	}	//	reset

	/**
	 * 	Delete Accounting Table where period status is open and update count.
	 * 	@param TableName table name
	 *	@param AD_Table_ID table
	 */
	private void delete (String TableName, int AD_Table_ID)
	{
		final IADTableDAO adTableDAO = Services.get(IADTableDAO.class);
		
		Timestamp today = TimeUtil.trunc(new Timestamp (System.currentTimeMillis()), TimeUtil.TRUNC_DAY);

		final I_C_AcctSchema as = Services.get(IAcctSchemaDAO.class).retrieveAcctSchema(getCtx());
		final boolean autoPeriod = as != null && as.isAutoPeriodControl();
		
		if (autoPeriod)
		{
			Timestamp temp = TimeUtil.addDays(today, - as.getPeriod_OpenHistory());
			if ( p_DateAcct_From == null || p_DateAcct_From.before(temp) ) {
				p_DateAcct_From = temp;
				log.info("DateAcct From set to: " + p_DateAcct_From);
			}
			temp = TimeUtil.addDays(today, as.getPeriod_OpenFuture());
			if ( p_DateAcct_To == null || p_DateAcct_To.after(temp) ) {
				p_DateAcct_To = temp;
				log.info("DateAcct To set to: " + p_DateAcct_To);
			}
		}

		reset(TableName);
		m_countReset = 0;
		//
		String docBaseType = null;
		if (AD_Table_ID == MInvoice.Table_ID)
			docBaseType = "IN ('" + X_C_DocType.DOCBASETYPE_APInvoice 
				+ "','" + X_C_DocType.DOCBASETYPE_APCreditMemo
				+ "','" + X_C_DocType.DOCBASETYPE_ARInvoice
				+ "','" + X_C_DocType.DOCBASETYPE_ARCreditMemo
				+ "','" + X_C_DocType.DOCBASETYPE_ARProFormaInvoice + "')";
		else if (AD_Table_ID == MInOut.Table_ID)
			docBaseType = "IN ('" + X_C_DocType.DOCBASETYPE_MaterialDelivery
				+ "','" + X_C_DocType.DOCBASETYPE_MaterialReceipt + "')";
		else if (AD_Table_ID == MPayment.Table_ID)
			docBaseType = "IN ('" + X_C_DocType.DOCBASETYPE_APPayment
				+ "','" + X_C_DocType.DOCBASETYPE_ARReceipt + "')";
		else if (AD_Table_ID == MOrder.Table_ID)
			docBaseType = "IN ('" + X_C_DocType.DOCBASETYPE_SalesOrder
				+ "','" + X_C_DocType.DOCBASETYPE_PurchaseOrder + "')";
		else if (AD_Table_ID == MProjectIssue.Table_ID)
			docBaseType = "= '" + X_C_DocType.DOCBASETYPE_ProjectIssue + "'";
		else if (AD_Table_ID == I_C_BankStatement.Table_ID)
			docBaseType = "= '" + X_C_DocType.DOCBASETYPE_BankStatement + "'";
		else if (AD_Table_ID == MCash.Table_ID)
			docBaseType = "= '" + X_C_DocType.DOCBASETYPE_CashJournal + "'";
		else if (AD_Table_ID == MAllocationHdr.Table_ID)
			docBaseType = "= '" + X_C_DocType.DOCBASETYPE_PaymentAllocation + "'";
		else if (AD_Table_ID == MJournal.Table_ID)
			docBaseType = "= '" + X_C_DocType.DOCBASETYPE_GLJournal + "'";
	//	else if (AD_Table_ID == M.Table_ID)
	//		docBaseType = "= '" + X_C_DocType.DOCBASETYPE_GLDocument + "'";
		else if (AD_Table_ID == MMovement.Table_ID)
			docBaseType = "= '" + X_C_DocType.DOCBASETYPE_MaterialMovement + "'";
		else if (AD_Table_ID == MRequisition.Table_ID)
			docBaseType = "= '" + X_C_DocType.DOCBASETYPE_PurchaseRequisition + "'";
		else if (AD_Table_ID == MInventory.Table_ID)
			docBaseType = "= '" + X_C_DocType.DOCBASETYPE_MaterialPhysicalInventory + "'";
		else if (AD_Table_ID == X_M_Production.Table_ID)
			docBaseType = "= '" + X_C_DocType.DOCBASETYPE_MaterialProduction + "'";
		else if (AD_Table_ID == adTableDAO.retrieveTableId(I_M_MatchInv.Table_Name))
			docBaseType = "= '" + X_C_DocType.DOCBASETYPE_MatchInvoice + "'";
		else if (AD_Table_ID == adTableDAO.retrieveTableId(I_M_MatchPO.Table_Name))
			docBaseType = "= '" + X_C_DocType.DOCBASETYPE_MatchPO + "'";
		else if (AD_Table_ID == adTableDAO.retrieveTableId(I_PP_Order.Table_Name))
			docBaseType = "IN ('" + X_C_DocType.DOCBASETYPE_ManufacturingOrder 
				+ "','" + X_C_DocType.DOCBASETYPE_MaintenanceOrder
				+ "','" + X_C_DocType.DOCBASETYPE_QualityOrder + "')";
		else if (AD_Table_ID == adTableDAO.retrieveTableId(I_DD_Order.Table_Name))
			docBaseType = "= '" + X_C_DocType.DOCBASETYPE_DistributionOrder+ "'";
		else if (AD_Table_ID == X_HR_Process.Table_ID)
			docBaseType = "= '" + X_C_DocType.DOCBASETYPE_Payroll+ "'";
		//
		if (docBaseType == null)
		{
			String s = TableName + ": Unknown DocBaseType";
			log.error(s);
			addLog(s);
			docBaseType = "";
			return;
		}
		else
			docBaseType = " AND pc.DocBaseType " + docBaseType;
		
		//	Doc
		String sql1 = "UPDATE " + TableName
			+ " SET Posted='N', Processing='N' "
			+ "WHERE AD_Client_ID=" + p_AD_Client_ID
			+ " AND (Posted<>'N' OR Posted IS NULL OR Processing<>'N' OR Processing IS NULL)"
			+ " AND EXISTS (SELECT 1 FROM C_PeriodControl pc"
			+ " INNER JOIN Fact_Acct fact ON (fact.C_Period_ID=pc.C_Period_ID) "
			+ " WHERE fact.AD_Table_ID=" + AD_Table_ID
			+ " AND fact.Record_ID=" + TableName + "." + TableName + "_ID";
		if ( !autoPeriod )
			sql1 += " AND pc.PeriodStatus = 'O'" + docBaseType;
		if (p_DateAcct_From != null)
			sql1 += " AND TRUNC(fact.DateAcct) >= " + DB.TO_DATE(p_DateAcct_From);
		if (p_DateAcct_To != null)
			sql1 += " AND TRUNC(fact.DateAcct) <= " + DB.TO_DATE(p_DateAcct_To);
		sql1 += ")";

		log.debug(sql1);

		int reset = DB.executeUpdate(sql1, get_TrxName()); 
		//	Fact
		String sql2 = "DELETE FROM Fact_Acct "
			+ "WHERE AD_Client_ID=" + p_AD_Client_ID
			+ " AND AD_Table_ID=" + AD_Table_ID;
		if ( !autoPeriod )
			sql2 += " AND EXISTS (SELECT 1 FROM C_PeriodControl pc "
				+ "WHERE pc.PeriodStatus = 'O'" + docBaseType
				+ " AND Fact_Acct.C_Period_ID=pc.C_Period_ID)";
		else
			sql2 += " AND EXISTS (SELECT 1 FROM C_PeriodControl pc "
				+ "WHERE Fact_Acct.C_Period_ID=pc.C_Period_ID)";
		if (p_DateAcct_From != null)
			sql2 += " AND TRUNC(Fact_Acct.DateAcct) >= " + DB.TO_DATE(p_DateAcct_From);
		if (p_DateAcct_To != null)
			sql2 += " AND TRUNC(Fact_Acct.DateAcct) <= " + DB.TO_DATE(p_DateAcct_To);

		log.debug(sql2);
		
		int deleted = DB.executeUpdate(sql2, get_TrxName());
		//
		log.info(TableName + "(" + AD_Table_ID + ") - Reset=" + reset + " - Deleted=" + deleted);
		String s = TableName + " - Reset=" + reset + " - Deleted=" + deleted;
		addLog(s);
		//
		m_countReset += reset;
		m_countDelete += deleted;
	}	//	delete

}	//	FactAcctReset
